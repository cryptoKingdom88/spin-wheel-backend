package com.casino.roulette.service;

import com.casino.roulette.exception.UserNotFoundException;
import com.casino.roulette.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserValidationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.casino.roulette.repository.TransactionLogRepository transactionLogRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void validateUserExists_ShouldThrowException_WhenUserNotFound() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> userService.validateUserExists(userId)
        );

        assertEquals(userId, exception.getUserId());
        assertEquals("User not found with ID: " + userId, exception.getMessage());
        verify(userRepository).findById(userId);
    }

    @Test
    void validateUserExists_ShouldReturnUser_WhenUserExists() {
        // Given
        Long userId = 1L;
        com.casino.roulette.entity.User user = new com.casino.roulette.entity.User(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        com.casino.roulette.entity.User result = userService.validateUserExists(userId);

        // Then
        assertNotNull(result);
        assertEquals(userId, result.getId());
        verify(userRepository).findById(userId);
    }

    @Test
    void validateUserExists_ShouldThrowException_WhenUserIdIsNull() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.validateUserExists(null)
        );

        assertEquals("User ID cannot be null", exception.getMessage());
        verifyNoInteractions(userRepository);
    }
}