-- Add missing tables for canvases, canvas_elements, gantt_baselines, and issue_links

CREATE TABLE IF NOT EXISTS canvases (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    created_by UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_canvases_project ON canvases(project_id);

CREATE TABLE IF NOT EXISTS canvas_elements (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    canvas_id UUID NOT NULL,
    element_type VARCHAR(20) NOT NULL,
    shape_type VARCHAR(30),
    pos_x DOUBLE PRECISION NOT NULL,
    pos_y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION,
    height DOUBLE PRECISION,
    end_x DOUBLE PRECISION,
    end_y DOUBLE PRECISION,
    rotation DOUBLE PRECISION NOT NULL DEFAULT 0,
    z_index INTEGER NOT NULL DEFAULT 0,
    text VARCHAR(2000),
    color VARCHAR(10),
    from_element_id UUID,
    to_element_id UUID,
    issue_id UUID,
    created_by UUID NOT NULL,
    updated_by UUID,
    revision BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_canvas_elements_canvas ON canvas_elements(canvas_id);

CREATE TABLE IF NOT EXISTS gantt_baselines (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    item_id UUID NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_by UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_gantt_baselines_project ON gantt_baselines(project_id);

CREATE TABLE IF NOT EXISTS issue_links (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    source_issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    target_issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    link_type VARCHAR(20) NOT NULL,
    created_by UUID NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_issue_links_project ON issue_links(project_id);
CREATE INDEX IF NOT EXISTS idx_issue_links_source ON issue_links(source_issue_id);
CREATE INDEX IF NOT EXISTS idx_issue_links_target ON issue_links(target_issue_id);
