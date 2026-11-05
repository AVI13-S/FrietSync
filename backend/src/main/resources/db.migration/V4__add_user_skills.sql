CREATE TABLE user_skills (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_name VARCHAR(100) NOT NULL
);

CREATE INDEX idx_user_skills_user ON user_skills(user_id);
