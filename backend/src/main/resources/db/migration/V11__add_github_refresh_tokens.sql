ALTER TABLE github_connections
    ADD COLUMN IF NOT EXISTS encrypted_refresh_token VARCHAR(4096),
    ADD COLUMN IF NOT EXISTS access_token_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS refresh_token_expires_at TIMESTAMP WITH TIME ZONE;
