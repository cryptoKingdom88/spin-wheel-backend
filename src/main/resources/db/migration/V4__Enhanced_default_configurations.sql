-- Enhanced default configurations for production use

-- Clear existing sample data to replace with production-ready defaults
DELETE FROM user_mission_progress;
DELETE FROM letter_collections;
DELETE FROM transaction_logs;
DELETE FROM users;
DELETE FROM roulette_slots;
DELETE FROM letter_words;
DELETE FROM deposit_missions;

-- Insert comprehensive deposit mission configurations
INSERT INTO deposit_missions (name, min_amount, max_amount, spins_granted, max_claims, active) VALUES
-- First deposit bonus (one-time only)
('First Deposit Bonus', 0.01, NULL, 1, 1, TRUE),

-- Regular deposit tiers with realistic limits
('Bronze Tier - Small Deposits', 10.00, 49.99, 1, 20, TRUE),
('Silver Tier - Medium Deposits', 50.00, 99.99, 2, 50, TRUE),
('Gold Tier - Large Deposits', 100.00, 199.99, 3, 100, TRUE),
('Platinum Tier - Premium Deposits', 200.00, 499.99, 5, 200, TRUE),
('Diamond Tier - VIP Deposits', 500.00, 999.99, 8, 300, TRUE),
('Elite Tier - High Roller', 1000.00, NULL, 12, 500, TRUE),

-- Special promotional missions (can be activated/deactivated)
('Weekend Warrior', 25.00, 99.99, 2, 10, FALSE),
('Happy Hour Bonus', 50.00, 149.99, 3, 15, FALSE),
('Monthly High Roller', 500.00, NULL, 10, 50, FALSE);

-- Insert balanced roulette slot configurations
-- Cash rewards with decreasing probability for higher amounts
INSERT INTO roulette_slots (slot_type, slot_value, weight, active) VALUES
-- Small cash rewards (higher probability)
('CASH', '0.50', 25, TRUE),
('CASH', '1.00', 20, TRUE),
('CASH', '2.00', 15, TRUE),
('CASH', '5.00', 12, TRUE),

-- Medium cash rewards (moderate probability)
('CASH', '10.00', 8, TRUE),
('CASH', '15.00', 6, TRUE),
('CASH', '25.00', 5, TRUE),
('CASH', '50.00', 4, TRUE),

-- Large cash rewards (low probability)
('CASH', '100.00', 2, TRUE),
('CASH', '250.00', 1, TRUE),
('CASH', '500.00', 1, TRUE),

-- Jackpot rewards (very low probability)
('CASH', '1000.00', 1, TRUE),

-- Letter rewards with balanced distribution
-- Common letters (higher weight)
('LETTER', 'A', 30, TRUE),
('LETTER', 'E', 28, TRUE),
('LETTER', 'I', 25, TRUE),
('LETTER', 'O', 25, TRUE),
('LETTER', 'U', 20, TRUE),
('LETTER', 'R', 22, TRUE),
('LETTER', 'S', 22, TRUE),
('LETTER', 'T', 22, TRUE),
('LETTER', 'L', 20, TRUE),
('LETTER', 'N', 20, TRUE),

-- Moderately common letters
('LETTER', 'C', 18, TRUE),
('LETTER', 'D', 18, TRUE),
('LETTER', 'G', 16, TRUE),
('LETTER', 'H', 18, TRUE),
('LETTER', 'M', 16, TRUE),
('LETTER', 'P', 15, TRUE),
('LETTER', 'B', 15, TRUE),
('LETTER', 'F', 14, TRUE),
('LETTER', 'W', 14, TRUE),
('LETTER', 'Y', 12, TRUE),
('LETTER', 'V', 12, TRUE),

-- Less common letters (lower weight)
('LETTER', 'K', 10, TRUE),
('LETTER', 'J', 8, TRUE),
('LETTER', 'X', 6, TRUE),
('LETTER', 'Q', 5, TRUE),
('LETTER', 'Z', 5, TRUE);

-- Insert comprehensive letter word collection bonuses
INSERT INTO letter_words (word, required_letters, reward_amount, active) VALUES
-- Easy words (3-4 letters) - lower rewards
('WIN', '{"W":1,"I":1,"N":1}', 5.00, TRUE),
('LUCK', '{"L":1,"U":1,"C":1,"K":1}', 8.00, TRUE),
('PLAY', '{"P":1,"L":1,"A":1,"Y":1}', 10.00, TRUE),
('GAME', '{"G":1,"A":1,"M":1,"E":1}', 12.00, TRUE),
('SPIN', '{"S":1,"P":1,"I":1,"N":1}', 15.00, TRUE),

-- Medium words (5-6 letters) - moderate rewards
('HAPPY', '{"H":1,"A":1,"P":2,"Y":1}', 25.00, TRUE),
('LUCKY', '{"L":1,"U":1,"C":1,"K":1,"Y":1}', 30.00, TRUE),
('BONUS', '{"B":1,"O":1,"N":1,"U":1,"S":1}', 35.00, TRUE),
('WINNER', '{"W":1,"I":1,"N":2,"E":1,"R":1}', 50.00, TRUE),
('CASINO', '{"C":1,"A":1,"S":1,"I":1,"N":1,"O":1}', 75.00, TRUE),
('ROULETTE', '{"R":1,"O":1,"U":1,"L":1,"E":2,"T":2}', 100.00, TRUE),

-- Hard words (7+ letters) - high rewards
('JACKPOT', '{"J":1,"A":1,"C":1,"K":1,"P":1,"O":1,"T":1}', 150.00, TRUE),
('FORTUNE', '{"F":1,"O":1,"R":1,"T":1,"U":1,"N":1,"E":1}', 200.00, TRUE),
('CHAMPION', '{"C":1,"H":1,"A":1,"M":1,"P":1,"I":1,"O":1,"N":1}', 250.00, TRUE),
('TREASURE', '{"T":1,"R":2,"E":2,"A":1,"S":1,"U":1}', 300.00, TRUE),

-- Special themed words
('DIAMOND', '{"D":1,"I":1,"A":1,"M":1,"O":1,"N":1}', 500.00, TRUE),
('PLATINUM', '{"P":1,"L":1,"A":1,"T":1,"I":1,"N":1,"U":1,"M":1}', 750.00, TRUE),
('MILLIONAIRE', '{"M":1,"I":2,"L":2,"O":1,"N":2,"A":1,"R":1,"E":1}', 1000.00, TRUE);

-- Insert a demo user for testing (can be removed in production)
INSERT INTO users (id, cash_balance, available_spins, first_deposit_bonus_used, last_daily_login) VALUES
(999999, 0.00, 1, FALSE, NULL);