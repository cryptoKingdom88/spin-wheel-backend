-- Create indexes for performance optimization

-- Indexes for users table
CREATE INDEX idx_users_last_daily_login ON users(last_daily_login);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Indexes for deposit_missions table
CREATE INDEX idx_deposit_missions_active ON deposit_missions(active);
CREATE INDEX idx_deposit_missions_amount_range ON deposit_missions(min_amount, max_amount);

-- Indexes for user_mission_progress table
CREATE INDEX idx_user_mission_progress_user_id ON user_mission_progress(user_id);
CREATE INDEX idx_user_mission_progress_mission_id ON user_mission_progress(mission_id);
CREATE INDEX idx_user_mission_progress_last_claim ON user_mission_progress(last_claim_date);

-- Indexes for roulette_slots table
CREATE INDEX idx_roulette_slots_active ON roulette_slots(active);
CREATE INDEX idx_roulette_slots_type_active ON roulette_slots(slot_type, active);
CREATE INDEX idx_roulette_slots_weight ON roulette_slots(weight);

-- Indexes for letter_collections table
CREATE INDEX idx_letter_collections_user_id ON letter_collections(user_id);
CREATE INDEX idx_letter_collections_letter ON letter_collections(letter);
CREATE INDEX idx_letter_collections_count ON letter_collections(count);

-- Indexes for letter_words table
CREATE INDEX idx_letter_words_active ON letter_words(active);
CREATE INDEX idx_letter_words_word ON letter_words(word);

-- Indexes for transaction_logs table
CREATE INDEX idx_transaction_logs_user_id ON transaction_logs(user_id);
CREATE INDEX idx_transaction_logs_type ON transaction_logs(transaction_type);
CREATE INDEX idx_transaction_logs_created_at ON transaction_logs(created_at);
CREATE INDEX idx_transaction_logs_user_created ON transaction_logs(user_id, created_at);