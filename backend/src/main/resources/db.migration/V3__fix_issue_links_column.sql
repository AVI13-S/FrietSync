-- Ensure link_type column exists and has a default value before validating

CREATE TABLE IF NOT EXISTS issue_links (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    target_issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    link_type VARCHAR(20) NOT NULL DEFAULT 'RELATES_TO',
    created_by UUID NOT NULL
);

ALTER TABLE issue_links ADD COLUMN IF NOT EXISTS link_type VARCHAR(20);
UPDATE issue_links SET link_type = 'RELATES_TO' WHERE link_type IS NULL;
ALTER TABLE issue_links ALTER COLUMN link_type SET DEFAULT 'RELATES_TO';
ALTER TABLE issue_links ALTER COLUMN link_type SET NOT NULL;
