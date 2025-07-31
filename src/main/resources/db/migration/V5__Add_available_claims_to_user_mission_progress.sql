-- Add available_claims column to track accumulated claim opportunities
-- This allows users to accumulate multiple claim opportunities from repeated deposits

ALTER TABLE user_mission_progress 
ADD COLUMN available_claims INTEGER NOT NULL DEFAULT 0;

-- Update existing records to have 1 available claim if they haven't been used
UPDATE user_mission_progress 
SET available_claims = 1 
WHERE claims_used = 0;

-- Add comment for clarity
COMMENT ON COLUMN user_mission_progress.available_claims IS 'Number of accumulated claim opportunities from deposits';