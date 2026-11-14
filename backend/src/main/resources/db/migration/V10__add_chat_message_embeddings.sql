ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS embedding vector(1536);

CREATE INDEX IF NOT EXISTS idx_chat_messages_embedding_hnsw
    ON chat_messages USING hnsw (embedding vector_cosine_ops);