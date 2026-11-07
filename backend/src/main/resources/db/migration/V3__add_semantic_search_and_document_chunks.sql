ALTER TABLE issues
    ADD COLUMN IF NOT EXISTS embedding vector(1536);

CREATE INDEX IF NOT EXISTS idx_issues_embedding_hnsw
    ON issues USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS project_document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES project_documents(id) ON DELETE CASCADE,
    project_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_document_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_project_document_chunks_project
    ON project_document_chunks (project_id);

CREATE INDEX IF NOT EXISTS idx_project_document_chunks_embedding_hnsw
    ON project_document_chunks USING hnsw (embedding vector_cosine_ops);
