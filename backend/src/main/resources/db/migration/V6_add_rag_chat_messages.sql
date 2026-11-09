CREATE TABLE IF NOT EXISTS rag_chat_messages (
                                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    question VARCHAR(1000) NOT NULL,
    answer TEXT NOT NULL,
    feedback VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_rag_chat_messages_project_user
    ON rag_chat_messages (project_id, user_id, created_at DESC);