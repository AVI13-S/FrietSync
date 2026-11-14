CREATE TABLE thread_votes (
                              id          UUID                     PRIMARY KEY,
                              thread_id   UUID                     NOT NULL,
                              user_id     UUID                     NOT NULL,
                              created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
                              updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,

                              CONSTRAINT uk_thread_votes_thread_user UNIQUE (thread_id, user_id),
                              CONSTRAINT fk_thread_votes_thread FOREIGN KEY (thread_id) REFERENCES project_threads (id) ON DELETE CASCADE,
                              CONSTRAINT fk_thread_votes_user   FOREIGN KEY (user_id)   REFERENCES users (id)           ON DELETE CASCADE
);

CREATE INDEX idx_thread_votes_user ON thread_votes (user_id);