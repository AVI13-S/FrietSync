-- FrietSync Baseline Migration (V1)

CREATE TABLE workspaces (
                            id UUID PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            admin_id UUID NOT NULL,
                            created_on TIMESTAMP WITH TIME ZONE NOT NULL,
                            deletion_requested_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       name VARCHAR(255) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       password_version INTEGER NOT NULL DEFAULT 1,
                       workspace_id UUID REFERENCES workspaces(id),
                       role VARCHAR(50) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       bio VARCHAR(2000),
                       job_title VARCHAR(200),
                       github_url VARCHAR(200),
                       linkedin_url VARCHAR(200),
                       portfolio_url VARCHAR(200),
                       profile_picture_key VARCHAR(2000)
);

CREATE TABLE projects (
                          id UUID PRIMARY KEY,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          workspace_id UUID REFERENCES workspaces(id),
                          name VARCHAR(255) NOT NULL,
                          description VARCHAR(1000),
                          profile_picture_key VARCHAR(2000),
                          status VARCHAR(50) NOT NULL,
                          start_date DATE,
                          deadline DATE,
                          complete_date DATE,
                          created_by UUID NOT NULL,
                          issue_counter INTEGER NOT NULL DEFAULT 0,
                          sprint_counter INTEGER NOT NULL DEFAULT 0,
                          team_size INTEGER NOT NULL DEFAULT 1,
                          admin_id UUID NOT NULL,
                          project_manager_id UUID,
                          team_lead_id UUID
);

CREATE TABLE project_members (
                                 id UUID PRIMARY KEY,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 role VARCHAR(50) NOT NULL,
                                 CONSTRAINT uk_project_member UNIQUE (project_id, user_id)
);

CREATE TABLE labels (
                        id UUID PRIMARY KEY,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                        name VARCHAR(20) NOT NULL,
                        color VARCHAR(7),
                        CONSTRAINT uk_project_label UNIQUE (project_id, name)
);

CREATE TABLE milestones (
                            id UUID PRIMARY KEY,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                            name VARCHAR(200) NOT NULL,
                            description VARCHAR(2000),
                            due_date DATE NOT NULL,
                            completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE issues (
                        id UUID PRIMARY KEY,
                        created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                        project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                        issue_number INTEGER NOT NULL,
                        title VARCHAR(200) NOT NULL,
                        description VARCHAR(5000),
                        status VARCHAR(50) NOT NULL,
                        priority VARCHAR(50) NOT NULL,
                        start_date DATE,
                        due_date DATE,
                        milestone_id UUID REFERENCES milestones(id) ON DELETE SET NULL,
                        estimated_hours DOUBLE PRECISION,
                        created_by UUID NOT NULL,
                        resolved_at TIMESTAMP WITH TIME ZONE,
                        closed_at TIMESTAMP WITH TIME ZONE,
                        deleted_at TIMESTAMP WITH TIME ZONE,
                        github_issue_number INTEGER,
                        github_issue_title VARCHAR(500),
                        github_issue_url VARCHAR(1000),
                        github_issue_repository VARCHAR(500),
                        CONSTRAINT uk_project_issue_number UNIQUE (project_id, issue_number)
);

CREATE INDEX idx_issues_project ON issues(project_id);

CREATE TABLE issue_assignees (
                                 id UUID PRIMARY KEY,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                                 user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                 CONSTRAINT uk_issue_assignee UNIQUE (issue_id, user_id)
);

CREATE TABLE issue_labels (
                              id UUID PRIMARY KEY,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                              label_id UUID NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
                              CONSTRAINT uk_issue_label UNIQUE (issue_id, label_id)
);

CREATE TABLE issue_comments (
                                id UUID PRIMARY KEY,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                                user_id UUID NOT NULL REFERENCES users(id),
                                content TEXT NOT NULL
);

CREATE TABLE issue_attachments (
                                   id UUID PRIMARY KEY,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                                   uploaded_by UUID NOT NULL,
                                   file_name VARCHAR(255) NOT NULL,
                                   file_key VARCHAR(1000) NOT NULL,
                                   file_size BIGINT NOT NULL
);

CREATE TABLE issue_history (
                               id UUID PRIMARY KEY,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               issue_id UUID NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
                               user_id UUID NOT NULL,
                               action VARCHAR(50) NOT NULL,
                               details TEXT
);

CREATE TABLE sprints (
                         id UUID PRIMARY KEY,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                         issue_id UUID REFERENCES issues(id) ON DELETE SET NULL,
                         sprint_number INTEGER NOT NULL,
                         name VARCHAR(255) NOT NULL,
                         goal TEXT,
                         status VARCHAR(50) NOT NULL,
                         start_date DATE,
                         end_date DATE,
                         created_by UUID NOT NULL,
                         assigned_contributor_id UUID,
                         CONSTRAINT uk_sprint_project_number UNIQUE (project_id, sprint_number)
);

CREATE TABLE sprint_comments (
                                 id UUID PRIMARY KEY,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 sprint_id UUID NOT NULL REFERENCES sprints(id) ON DELETE CASCADE,
                                 user_id UUID NOT NULL,
                                 content TEXT NOT NULL
);

CREATE TABLE project_budgets (
                                 id UUID PRIMARY KEY,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                 total_budget NUMERIC(15, 2) NOT NULL,
                                 currency VARCHAR(10) NOT NULL DEFAULT 'USD',
                                 status VARCHAR(50) NOT NULL
);

CREATE TABLE project_expenses (
                                  id UUID PRIMARY KEY,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                  title VARCHAR(255) NOT NULL,
                                  amount NUMERIC(15, 2) NOT NULL,
                                  category VARCHAR(50) NOT NULL,
                                  expense_date DATE NOT NULL,
                                  recorded_by UUID NOT NULL,
                                  receipt_url VARCHAR(1000)
);

CREATE TABLE user_salaries (
                               id UUID PRIMARY KEY,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
                               monthly_amount NUMERIC(15, 2) NOT NULL,
                               currency VARCHAR(10) NOT NULL DEFAULT 'USD'
);

CREATE TABLE chat_messages (
                               id UUID PRIMARY KEY,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
                               workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
                               sender_id UUID NOT NULL REFERENCES users(id),
                               recipient_id UUID REFERENCES users(id),
                               content TEXT NOT NULL
);

CREATE TABLE chat_attachments (
                                  id UUID PRIMARY KEY,
                                  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                  message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
                                  file_name VARCHAR(255) NOT NULL,
                                  file_key VARCHAR(1000) NOT NULL,
                                  file_size BIGINT NOT NULL
);

CREATE TABLE chat_mutes (
                            id UUID PRIMARY KEY,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
                            workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE
);

CREATE TABLE invites (
                         id UUID PRIMARY KEY,
                         created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         email VARCHAR(255) NOT NULL,
                         workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
                         project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
                         role VARCHAR(50) NOT NULL,
                         purpose VARCHAR(50) NOT NULL,
                         token VARCHAR(255) NOT NULL UNIQUE,
                         expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                         status VARCHAR(50) NOT NULL,
                         invited_by UUID NOT NULL
);

CREATE TABLE notifications (
                               id UUID PRIMARY KEY,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               title VARCHAR(255) NOT NULL,
                               message TEXT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               reference_id UUID
);

CREATE TABLE notification_preferences (
                                          id UUID PRIMARY KEY,
                                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                          user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                          email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                                          push_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE meetings (
                          id UUID PRIMARY KEY,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                          title VARCHAR(255) NOT NULL,
                          description TEXT,
                          start_time TIMESTAMP WITH TIME ZONE NOT NULL,
                          end_time TIMESTAMP WITH TIME ZONE NOT NULL,
                          created_by UUID NOT NULL
);

CREATE TABLE meeting_attendees (
                                   id UUID PRIMARY KEY,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   meeting_id UUID NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
                                   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   attendance_status VARCHAR(50) NOT NULL
);

CREATE TABLE polls (
                       id UUID PRIMARY KEY,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                       project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                       question VARCHAR(500) NOT NULL,
                       mode VARCHAR(50) NOT NULL,
                       status VARCHAR(50) NOT NULL,
                       created_by UUID NOT NULL
);

CREATE TABLE poll_options (
                              id UUID PRIMARY KEY,
                              created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                              poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
                              option_text VARCHAR(255) NOT NULL
);

CREATE TABLE poll_votes (
                            id UUID PRIMARY KEY,
                            created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                            poll_id UUID NOT NULL REFERENCES polls(id) ON DELETE CASCADE,
                            option_id UUID NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT uk_poll_user_vote UNIQUE (poll_id, user_id)
);

CREATE TABLE project_documents (
                                   id UUID PRIMARY KEY,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                   title VARCHAR(255) NOT NULL,
                                   content TEXT,
                                   created_by UUID NOT NULL
);

CREATE TABLE community_posts (
                                 id UUID PRIMARY KEY,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                 workspace_id UUID REFERENCES workspaces(id) ON DELETE CASCADE,
                                 project_id UUID REFERENCES projects(id) ON DELETE CASCADE,
                                 author_id UUID NOT NULL REFERENCES users(id),
                                 title VARCHAR(255) NOT NULL,
                                 content TEXT NOT NULL
);

CREATE TABLE community_comments (
                                    id UUID PRIMARY KEY,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    post_id UUID NOT NULL REFERENCES community_posts(id) ON DELETE CASCADE,
                                    author_id UUID NOT NULL REFERENCES users(id),
                                    content TEXT NOT NULL
);

CREATE TABLE community_reactions (
                                     id UUID PRIMARY KEY,
                                     created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                     user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     target_type VARCHAR(50) NOT NULL,
                                     target_id UUID NOT NULL,
                                     reaction_type VARCHAR(50) NOT NULL,
                                     CONSTRAINT uk_reaction UNIQUE (user_id, target_type, target_id, reaction_type)
);

CREATE TABLE project_activities (
                                    id UUID PRIMARY KEY,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                    user_id UUID NOT NULL REFERENCES users(id),
                                    action VARCHAR(255) NOT NULL,
                                    details TEXT
);

CREATE TABLE google_connections (
                                    id UUID PRIMARY KEY,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                    google_email VARCHAR(255) NOT NULL,
                                    access_token TEXT NOT NULL,
                                    refresh_token TEXT,
                                    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE github_connections (
                                    id UUID PRIMARY KEY,
                                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                                    github_username VARCHAR(255) NOT NULL,
                                    access_token TEXT NOT NULL
);

CREATE TABLE project_github_repositories (
                                             id UUID PRIMARY KEY,
                                             created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                             updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                             project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                             repository_name VARCHAR(255) NOT NULL,
                                             repository_url VARCHAR(1000) NOT NULL
);

CREATE TABLE call_sessions (
                               id UUID PRIMARY KEY,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                               project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                               room_id BIGINT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               active BOOLEAN NOT NULL DEFAULT TRUE,
                               started_by UUID NOT NULL
);

CREATE TABLE call_participants (
                                   id UUID PRIMARY KEY,
                                   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   call_session_id UUID NOT NULL REFERENCES call_sessions(id) ON DELETE CASCADE,
                                   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   joined_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                   left_at TIMESTAMP WITH TIME ZONE
);
