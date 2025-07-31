-- Create initial database schema for free roulette spin system

-- Users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    cash_balance DECIMAL(10,2) DEFAULT 0.00 NOT NULL,
    available_spins INTEGER DEFAULT 0 NOT NULL,
    first_deposit_bonus_used BOOLEAN DEFAULT FALSE NOT NULL,
    last_daily_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Deposit missions table
CREATE TABLE deposit_missions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    min_amount DECIMAL(10,2) NOT NULL,
    max_amount DECIMAL(10,2),
    spins_granted INTEGER NOT NULL,
    max_claims INTEGER NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- User mission progress table
CREATE TABLE user_mission_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    mission_id BIGINT NOT NULL,
    claims_used INTEGER DEFAULT 0 NOT NULL,
    last_claim_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_mission_progress_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_mission_progress_mission FOREIGN KEY (mission_id) REFERENCES deposit_missions(id),
    CONSTRAINT uk_user_mission_progress UNIQUE(user_id, mission_id)
);

-- Roulette slots table
CREATE TABLE roulette_slots (
    id BIGSERIAL PRIMARY KEY,
    slot_type VARCHAR(10) NOT NULL CHECK (slot_type IN ('CASH', 'LETTER')),
    slot_value VARCHAR(50) NOT NULL,
    weight INTEGER NOT NULL DEFAULT 1 CHECK (weight > 0),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Letter collections table
CREATE TABLE letter_collections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    letter VARCHAR(1) NOT NULL CHECK (letter ~ '^[A-Z]$'),
    count INTEGER DEFAULT 0 NOT NULL CHECK (count >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_letter_collections_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_letter_collections UNIQUE(user_id, letter)
);

-- Letter words table
CREATE TABLE letter_words (
    id BIGSERIAL PRIMARY KEY,
    word VARCHAR(50) NOT NULL,
    required_letters TEXT NOT NULL,
    reward_amount DECIMAL(10,2) NOT NULL CHECK (reward_amount > 0),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Transaction logs table
CREATE TABLE transaction_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount DECIMAL(10,2),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_transaction_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);