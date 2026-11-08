ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS forwarded BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS forwarded_from_sender_name VARCHAR(255);