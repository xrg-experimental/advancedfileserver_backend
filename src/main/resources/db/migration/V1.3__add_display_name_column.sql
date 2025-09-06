ALTER TABLE users ADD COLUMN display_name VARCHAR(50);

-- Set initial display names to usernames for existing users
UPDATE users SET display_name = username WHERE display_name IS NULL;
