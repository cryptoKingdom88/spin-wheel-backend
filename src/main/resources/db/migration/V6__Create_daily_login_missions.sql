-- Create daily login missions table
CREATE TABLE daily_login_missions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    spins_granted INTEGER NOT NULL CHECK (spins_granted >= 1),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default daily login mission
INSERT INTO daily_login_missions (name, description, spins_granted, active) VALUES
('Daily Login', 'Login daily to receive 1 free spin! Available once per day.', 1, TRUE);

-- Add comment for clarity
COMMENT ON TABLE daily_login_missions IS 'Configuration for daily login missions';
COMMENT ON COLUMN daily_login_missions.spins_granted IS 'Number of spins granted for daily login';
COMMENT ON COLUMN daily_login_missions.active IS 'Whether this daily login mission is currently active';