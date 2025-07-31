-- Insert sample data for testing

-- Insert sample users
INSERT INTO users (id, cash_balance, available_spins, first_deposit_bonus_used, last_daily_login) VALUES
(1, 100.00, 5, FALSE, '2024-01-15 10:00:00'),
(2, 250.50, 0, TRUE, '2024-01-14 15:30:00'),
(3, 0.00, 3, FALSE, NULL);

-- Insert default deposit missions
INSERT INTO deposit_missions (name, min_amount, max_amount, spins_granted, max_claims, active) VALUES
('First Deposit Bonus', 0.01, NULL, 1, 1, TRUE),
('Small Deposit Tier', 50.00, 99.99, 1, 50, TRUE),
('Medium Deposit Tier', 100.00, 199.99, 1, 100, TRUE),
('Large Deposit Tier', 200.00, 499.99, 1, 200, TRUE),
('Premium Deposit Tier', 500.00, NULL, 2, 500, TRUE);

-- Insert sample user mission progress
INSERT INTO user_mission_progress (user_id, mission_id, claims_used, last_claim_date) VALUES
(1, 1, 1, '2024-01-15 10:00:00'),
(2, 1, 1, '2024-01-14 15:30:00'),
(2, 2, 5, '2024-01-14 16:00:00');

-- Insert default roulette slots with weights
INSERT INTO roulette_slots (slot_type, slot_value, weight, active) VALUES
-- Cash rewards (lower probability)
('CASH', '0.1', 30, TRUE),
('CASH', '0.5', 25, TRUE),
('CASH', '2', 20, TRUE),
('CASH', '3', 15, TRUE),
('CASH', '10.00', 10, TRUE),
('CASH', '30.00', 5, TRUE),
('CASH', '100.00', 2, TRUE),
('CASH', '500.00', 1, TRUE),
-- Letter rewards (higher probability)
('LETTER', 'H', 30, TRUE),
('LETTER', 'A', 20, TRUE),
('LETTER', 'P', 10, TRUE),
('LETTER', 'Y', 5, TRUE);

-- Insert sample letter collections
INSERT INTO letter_collections (user_id, letter, count) VALUES
(1, 'H', 1),
(1, 'A', 2),
(1, 'P', 1),
(1, 'Y', 1);

-- Insert default letter words for collection bonuses
INSERT INTO letter_words (word, required_letters, reward_amount, active) VALUES
('HAPPY', '{"H":1,"A":1,"P":2,"Y":1}', 25.00, TRUE);

-- Insert sample transaction logs
INSERT INTO transaction_logs (user_id, transaction_type, amount, description) VALUES
(1, 'DEPOSIT', 100.00, 'Initial deposit'),
(1, 'SPIN_WIN', 5.00, 'Roulette spin cash win'),
(1, 'DAILY_LOGIN_SPIN', NULL, 'Daily login free spin granted'),
(2, 'DEPOSIT', 250.50, 'Deposit transaction'),
(2, 'LETTER_BONUS', 25.00, 'Letter collection bonus for HAPPY'),
(3, 'DAILY_LOGIN_SPIN', NULL, 'Daily login free spin granted');