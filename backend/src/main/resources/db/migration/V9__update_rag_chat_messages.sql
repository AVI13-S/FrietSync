ALTER TABLE rag_chat_messages ALTER COLUMN project_id DROP NOT NULL;
ALTER TABLE rag_chat_messages ADD COLUMN IF NOT EXISTS workspace_id UUID;
CREATE INDEX IF NOT EXISTS idx_rag_chat_messages_user_created
    ON rag_chat_messages (user_id, created_at DESC);
