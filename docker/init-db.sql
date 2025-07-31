-- Database initialization script for Free Roulette Spin System
-- This script runs when the PostgreSQL container starts for the first time

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- Create user if it doesn't exist (handled by POSTGRES_USER env var)

-- Set timezone
SET timezone = 'UTC';

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE casino_roulette TO casino_user;

-- Log initialization
SELECT 'Database initialized successfully for Free Roulette Spin System' as status;