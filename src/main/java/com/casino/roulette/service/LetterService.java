package com.casino.roulette.service;

import com.casino.roulette.dto.LetterCollectionDTO;
import com.casino.roulette.dto.LetterWordDTO;
import com.casino.roulette.entity.LetterCollection;
import com.casino.roulette.entity.LetterWord;
import com.casino.roulette.entity.TransactionLog;
import com.casino.roulette.repository.LetterCollectionRepository;
import com.casino.roulette.repository.LetterWordRepository;
import com.casino.roulette.repository.TransactionLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class LetterService {
    
    private final LetterCollectionRepository letterCollectionRepository;
    private final LetterWordRepository letterWordRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final UserService userService;
    
    @Autowired
    public LetterService(LetterCollectionRepository letterCollectionRepository,
                        LetterWordRepository letterWordRepository,
                        TransactionLogRepository transactionLogRepository,
                        UserService userService) {
        this.letterCollectionRepository = letterCollectionRepository;
        this.letterWordRepository = letterWordRepository;
        this.transactionLogRepository = transactionLogRepository;
        this.userService = userService;
    }
    
    /**
     * Add a letter to user's collection
     */
    public void addLetterToCollection(Long userId, String letter) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (letter == null || letter.trim().isEmpty()) {
            throw new IllegalArgumentException("Letter cannot be null or empty");
        }
        if (letter.length() != 1 || !Character.isLetter(letter.charAt(0))) {
            throw new IllegalArgumentException("Letter must be a single alphabetic character");
        }
        
        // Validate user exists first
        userService.validateUserExists(userId);
        
        String upperLetter = letter.toUpperCase();
        
        // Find existing collection or create new one
        Optional<LetterCollection> existingCollection = letterCollectionRepository
            .findByUserIdAndLetter(userId, upperLetter);
        
        if (existingCollection.isPresent()) {
            // Increment existing collection
            int updatedRows = letterCollectionRepository
                .incrementExistingLetterCount(userId, upperLetter, 1);
            if (updatedRows == 0) {
                throw new RuntimeException("Failed to increment letter count for user: " + userId);
            }
        } else {
            // Create new collection
            LetterCollection newCollection = new LetterCollection(userId, upperLetter, 1);
            letterCollectionRepository.save(newCollection);
        }
        
        // Log the letter collection transaction
        TransactionLog log = TransactionLog.createLetterCollectedLog(userId, upperLetter);
        transactionLogRepository.save(log);
    }
    
    /**
     * Get user's letter collection
     */
    @Transactional(readOnly = true)
    public List<LetterCollectionDTO> getUserLetterCollection(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        System.out.println("=== getUserLetterCollection called for userId: " + userId + " ===");
        
        // Validate user exists first
        userService.validateUserExists(userId);
        
        List<LetterCollection> collections = letterCollectionRepository
            .findByUserIdWithPositiveCount(userId);
        
        System.out.println("Found " + collections.size() + " letter collections:");
        collections.forEach(c -> System.out.println("  " + c.getLetter() + ": " + c.getCount()));
        
        return collections.stream()
            .map(collection -> new LetterCollectionDTO(collection.getLetter(), collection.getCount()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get available words for collection bonuses
     */
    @Transactional(readOnly = true)
    public List<LetterWordDTO> getAvailableWords(Long userId) {
        System.out.println("=== getAvailableWords called for userId: " + userId + " ===");
        
        List<LetterWord> activeWords = letterWordRepository.findActiveWordsOrderByRewardDesc();
        Map<String, Integer> userLetters = getUserLetterMap(userId);
        
        System.out.println("Active words count: " + activeWords.size());
        System.out.println("User letters from getUserLetterMap: " + userLetters);
        
        return activeWords.stream()
            .map(word -> {
                Map<String, Integer> required = word.getRequiredLettersMap();
                boolean canClaim = word.canClaimWith(userLetters);
                
                // Debug logging with detailed comparison
                System.out.println("=== Letter Word Claim Check ===");
                System.out.println("User ID: " + userId);
                System.out.println("Word: " + word.getWord());
                System.out.println("Required: " + required);
                System.out.println("User has: " + userLetters);
                
                // Check each letter individually
                for (Map.Entry<String, Integer> entry : required.entrySet()) {
                    String letter = entry.getKey();
                    Integer requiredCount = entry.getValue();
                    Integer userCount = userLetters.getOrDefault(letter, 0);
                    boolean hasEnough = userCount >= requiredCount;
                    System.out.println("  " + letter + ": required=" + requiredCount + ", user=" + userCount + ", hasEnough=" + hasEnough);
                }
                
                System.out.println("Final canClaim: " + canClaim);
                System.out.println("===============================");
                
                return new LetterWordDTO(
                    word.getId(),
                    word.getWord(),
                    word.getRequiredLettersMap(),
                    word.getRewardAmount(),
                    canClaim
                );
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Claim word bonus
     */
    public void claimWordBonus(Long userId, Long wordId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (wordId == null) {
            throw new IllegalArgumentException("Word ID cannot be null");
        }
        
        // Validate user exists first
        userService.validateUserExists(userId);
        
        // Get the word
        LetterWord word = letterWordRepository.findById(wordId)
            .orElseThrow(() -> new IllegalArgumentException("Word not found: " + wordId));
        
        if (!word.getActive()) {
            throw new IllegalStateException("Word is not active: " + wordId);
        }
        
        // Get user's current letter collection
        Map<String, Integer> userLetters = getUserLetterMap(userId);
        
        // Check if user can claim this word
        if (!word.canClaimWith(userLetters)) {
            throw new IllegalStateException("User does not have sufficient letters to claim word: " + word.getWord());
        }
        
        // Deduct required letters from user's collection
        Map<String, Integer> requiredLetters = word.getRequiredLettersMap();
        for (Map.Entry<String, Integer> entry : requiredLetters.entrySet()) {
            String letter = entry.getKey();
            Integer requiredCount = entry.getValue();
            
            int updatedRows = letterCollectionRepository
                .decrementLetterCount(userId, letter, requiredCount);
            if (updatedRows == 0) {
                throw new RuntimeException("Failed to deduct letters for user: " + userId);
            }
        }
        
        // Grant cash reward to user
        userService.updateCashBalance(userId, word.getRewardAmount());
        
        // Log the bonus claim transaction
        TransactionLog log = TransactionLog.createLetterBonusLog(
            userId, word.getRewardAmount(), word.getWord());
        transactionLogRepository.save(log);
    }
    
    /**
     * Check if user can claim a specific word
     */
    @Transactional(readOnly = true)
    public boolean canClaimWord(Long userId, Long wordId) {
        if (userId == null || wordId == null) {
            return false;
        }
        
        Optional<LetterWord> wordOpt = letterWordRepository.findById(wordId);
        if (wordOpt.isEmpty() || !wordOpt.get().getActive()) {
            return false;
        }
        
        Map<String, Integer> userLetters = getUserLetterMap(userId);
        return wordOpt.get().canClaimWith(userLetters);
    }
    
    /**
     * Get count of a specific letter for a user
     */
    @Transactional(readOnly = true)
    public Integer getLetterCount(Long userId, String letter) {
        if (userId == null || letter == null) {
            return 0;
        }
        
        return letterCollectionRepository.getLetterCountForUser(userId, letter.toUpperCase());
    }
    
    /**
     * Check if user has at least the required amount of a specific letter
     */
    @Transactional(readOnly = true)
    public boolean hasAtLeastLetters(Long userId, String letter, Integer requiredCount) {
        if (userId == null || letter == null || requiredCount == null || requiredCount <= 0) {
            return false;
        }
        
        return letterCollectionRepository.hasAtLeastLetterCount(
            userId, letter.toUpperCase(), requiredCount);
    }
    
    /**
     * Get total number of letters collected by user
     */
    @Transactional(readOnly = true)
    public Long getTotalLetterCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        
        return letterCollectionRepository.getTotalLetterCountForUser(userId);
    }
    
    /**
     * Get distinct letters collected by user
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctLetters(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        return letterCollectionRepository.getDistinctLettersForUser(userId);
    }
    
    /**
     * Get count of distinct letters for user
     */
    @Transactional(readOnly = true)
    public long getDistinctLetterCount(Long userId) {
        if (userId == null) {
            return 0L;
        }
        
        return letterCollectionRepository.countDistinctLettersForUser(userId);
    }
    
    /**
     * Get all available words (without user-specific claim status)
     */
    @Transactional(readOnly = true)
    public List<LetterWordDTO> getAllAvailableWords() {
        List<LetterWord> activeWords = letterWordRepository.findActiveWordsOrderByRewardDesc();
        
        return activeWords.stream()
            .map(word -> new LetterWordDTO(
                word.getId(),
                word.getWord(),
                word.getRequiredLettersMap(),
                word.getRewardAmount(),
                false // No user context, so can't determine claim status
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Get words that user can currently claim
     */
    @Transactional(readOnly = true)
    public List<LetterWordDTO> getClaimableWords(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        
        return getAvailableWords(userId).stream()
            .filter(LetterWordDTO::getCanClaim)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if user has any claimable words
     */
    @Transactional(readOnly = true)
    public boolean hasClaimableWords(Long userId) {
        if (userId == null) {
            return false;
        }
        
        return !getClaimableWords(userId).isEmpty();
    }
    
    /**
     * Get user's letter collection as a map for internal use
     */
    private Map<String, Integer> getUserLetterMap(Long userId) {
        if (userId == null) return null;

        List<LetterCollection> collections = letterCollectionRepository
            .findByUserIdWithPositiveCount(userId);
        
        Map<String, Integer> letterMap = new HashMap<>();
        for (LetterCollection collection : collections) {
            letterMap.put(collection.getLetter(), collection.getCount());
        }
        
        return letterMap;
    }
}