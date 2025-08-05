-- Add column to track daily mission claim separately from login
ALTER TABLE users ADD COLUMN last_daily_mission_claim TIMESTAMP;