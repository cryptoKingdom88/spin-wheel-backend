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
('CASH', '1.00', 15, TRUE),
('CASH', '2.50', 10, TRUE),
('CASH', '5.00', 8, TRUE),
('CASH', '10.00', 5, TRUE),
('CASH', '25.00', 3, TRUE),
('CASH', '50.00', 2, TRUE),
('CASH', '100.00', 1, TRUE),
-- Letter rewards (higher probability)
('LETTER', 'A', 20, TRUE),
('LETTER', 'B', 18, TRUE),
('LETTER', 'C', 18, TRUE),
('LETTER', 'D', 16, TRUE),
('LETTER', 'E', 22, TRUE),
('LETTER', 'F', 14, TRUE),
('LETTER', 'G', 14, TRUE),
('LETTER', 'H', 16, TRUE),
('LETTER', 'I', 20, TRUE),
('LETTER', 'J', 8, TRUE),
('LETTER', 'K', 10, TRUE),
('LETTER', 'L', 16, TRUE),
('LETTER', 'M', 14, TRUE),
('LETTER', 'N', 18, TRUE),
('LETTER', 'O', 20, TRUE),
('LETTER', 'P', 12, TRUE),
('LETTER', 'Q', 6, TRUE),
('LETTER', 'R', 18, TRUE),
('LETTER', 'S', 18, TRUE),
('LETTER', 'T', 18, TRUE),
('LETTER', 'U', 14, TRUE),
('LETTER', 'V', 10, TRUE),
('LETTER', 'W', 12, TRUE),
('LETTER', 'X', 6, TRUE),
('LETTER', 'Y', 10, TRUE),
('LETTER', 'Z', 6, TRUE);

-- Insert sample letter collections
INSERT INTO letter_collections (user_id, letter, count) VALUES
(1, 'H', 1),
(1, 'A', 2),
(1, 'P', 1),
(1, 'Y', 1),
(2, 'L', 1),
(2, 'U', 1),
(2, 'C', 2),
(2, 'K', 1);

-- Insert default letter words for collection bonuses
INSERT INTO letter_words (word, required_letters, reward_amount, active) VALUES
('HAPPY', '{"H":1,"A":1,"P":2,"Y":1}', 25.00, TRUE),
('LUCKY', '{"L":1,"U":1,"C":1,"K":1,"Y":1}', 50.00, TRUE),
('CASINO', '{"C":1,"A":1,"S":1,"I":1,"N":1,"O":1}', 100.00, TRUE),
('WINNER', '{"W":1,"I":1,"N":2,"E":1,"R":1}', 75.00, TRUE),
('JACKPOT', '{"J":1,"A":1,"C":1,"K":1,"P":1,"O":1,"T":1}', 200.00, TRUE),
('SPIN', '{"S":1,"P":1,"I":1,"N":1}', 15.00, TRUE),
('BONUS', '{"B":1,"O":1,"N":1,"U":1,"S":1}', 30.00, TRUE);

-- Insert sample transaction logs
INSERT INTO transaction_logs (user_id, transaction_type, amount, description) VALUES
(1, 'DEPOSIT', 100.00, 'Initial deposit'),
(1, 'SPIN_WIN', 5.00, 'Roulette spin cash win'),
(1, 'DAILY_LOGIN_SPIN', NULL, 'Daily login free spin granted'),
(2, 'DEPOSIT', 250.50, 'Deposit transaction'),
(2, 'LETTER_BONUS', 25.00, 'Letter collection bonus for HAPPY'),
(3, 'DAILY_LOGIN_SPIN', NULL, 'Daily login free spin granted');