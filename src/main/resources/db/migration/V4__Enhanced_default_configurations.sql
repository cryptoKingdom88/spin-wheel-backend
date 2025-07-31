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
('First Deposit', 0.01, NULL, 1, 1, TRUE),

-- Regular deposit tiers with realistic limits
('Deposit R$ 50', 50.00, 99.99, 1, 50, TRUE),
('Deposit R$ 100', 100.00, 199.99, 1, 100, TRUE),
('Deposit R$ 200', 200.00, 499.99, 1, 100, TRUE),
('Deposit R$ 500', 500.00, NULL, 2, 500, TRUE),
-- Special promotional missions (can be activated/deactivated)
('Weekend Warrior', 25.00, 99.99, 2, 10, FALSE),
('Happy Hour Bonus', 50.00, 149.99, 3, 15, FALSE),
('Monthly High Roller', 500.00, NULL, 10, 50, FALSE);

-- Insert balanced roulette slot configurations
-- Cash rewards with decreasing probability for higher amounts
INSERT INTO roulette_slots (slot_type, slot_value, weight, active) VALUES
-- Small cash rewards (higher probability)
('CASH', '0.10', 30, TRUE),
('CASH', '0.50', 20, TRUE),
('CASH', '2.00', 15, TRUE),
('CASH', '3.00', 13, TRUE),
('CASH', '10.00', 10, TRUE),
('CASH', '30.00', 5, TRUE),
('CASH', '100.00', 2, TRUE),
('CASH', '500.00', 1, TRUE),
('LETTER', 'H', 25, TRUE),
('LETTER', 'A', 20, TRUE),
('LETTER', 'P', 10, TRUE),
('LETTER', 'Y', 5, TRUE);

-- Insert comprehensive letter word collection bonuses
INSERT INTO letter_words (word, required_letters, reward_amount, active) VALUES
('HAPPY', '{"H":1,"A":1,"P":2,"Y":1}', 100.00, TRUE);

-- Insert a demo user for testing (can be removed in production)
INSERT INTO users (id, cash_balance, available_spins, first_deposit_bonus_used, last_daily_login) VALUES
(999999, 0.00, 1, FALSE, NULL);