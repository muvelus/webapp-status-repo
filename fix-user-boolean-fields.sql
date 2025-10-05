
UPDATE users 
SET 
    is_account_non_expired = COALESCE(is_account_non_expired, true),
    is_account_non_locked = COALESCE(is_account_non_locked, true),
    is_credentials_non_expired = COALESCE(is_credentials_non_expired, true),
    is_enabled = COALESCE(is_enabled, true)
WHERE 
    is_account_non_expired IS NULL 
    OR is_account_non_locked IS NULL 
    OR is_credentials_non_expired IS NULL
    OR is_enabled IS NULL;

SELECT username, email, is_enabled, is_account_non_expired, is_account_non_locked, is_credentials_non_expired 
FROM users;
