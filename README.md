# FrietSync Backend

FrietSync is a project and team collaboration platform. This repository contains its Java backend: a REST API and authenticated WebSocket services for project planning, collaboration, document search, and AI-assisted chat. It does not contain the web frontend.

## Contents

- [What the backend provides](#what-the-backend-provides)
- [Architecture and technology](#architecture-and-technology)
- [Getting started](#getting-started)
- [APIs](#apis)
- [Invites](#invites)
- [Sprints](#sprints)
- [Authentication](#how-jwt-authentication-works)
- [Password recovery](#password-recovery)
- [Security notes](#security-notes)
- [Troubleshooting](#troubleshooting)

## What the backend provides

- **Accounts and access:** email signup, OTP verification, JWT access tokens, refresh-token cookies, invitations, workspace membership, and role-based permissions.
- **Project planning:** projects, issues, labels, assignments, issue links and checklists, sprints, milestones, releases, boards, calendars, Gantt data, and project history.
- **Team collaboration:** project chat, threaded discussions and comments, polls, workspace community posts and reactions, meetings, live presence, notifications, and project calls.
- **Shared workspaces:** live collaborative canvases with conversion of canvas elements into issues.
- **Documents and AI:** project document upload and download, text extraction, semantic search, and RAG chat that can retrieve from project documents and other indexed project data.
- **Reporting and integrations:** project budgets and expenses, employee records, daily briefings, replay snapshots, and Google and GitHub OAuth integrations.

## Architecture and technology

The backend is organized as a Spring application. HTTP controllers map requests to services; services implement business rules and call repositories; JPA entities map application data to PostgreSQL. WebSocket message handlers provide authenticated, project-scoped real-time features. Flyway manages database schema changes.

| Area | Technology |
|---|---|
| Runtime and API | Java 25, Spring Boot 4.1.1, Spring MVC, Spring Security |
| Authentication | JWT access tokens, HTTP-only refresh-token cookies, BCrypt password hashing |
| Database | PostgreSQL with pgvector; Flyway migrations |
| Cache and ephemeral state | Redis |
| AI and embeddings | Spring AI with Google Gemini models |
| Documents | Apache Tika text extraction and AWS S3 object storage |
| Upload protection | ClamAV file scanning when enabled |
| Real-time messaging | STOMP over WebSocket; Janus is used for project calls |

Important source directories under `backend/src/main/java/com/frietsync/backend`:

| Directory | Contents |
|---|---|
| `controller` | REST and WebSocket endpoints, grouped by feature |
| `service` | Business logic and integration clients |
| `repository` | Spring Data and custom database queries |
| `entity` | Persisted JPA models |
| `dto` | API request and response types |
| `config` | Security, database, storage, AI, and WebSocket configuration |
| `exception` | Application exceptions and API error handling |

Flyway scripts are in `backend/src/main/resources/db/migration`.

## Getting started

### Requirements

- Docker Desktop with Docker Compose for the recommended setup, or Java 25 plus accessible PostgreSQL and Redis instances for running the application directly.
- Credentials for any external services you intend to use (for example, Gemini, AWS S3, SMTP, Google/GitHub OAuth, ClamAV, or Janus). These integrations are not all provided as Docker Compose services.

### Run the complete local stack with Docker

From the repository root, copy the example configuration and edit it:

```powershell
Copy-Item backend\.env.example backend\.env
```

In `backend\.env`, set the database URL to the Compose service name, and configure database and Redis credentials:

```dotenv
DB_URL=jdbc:postgresql://postgres:5432/frietsync
DB_USERNAME=frietsync
DB_PASSWORD=change-this-password
REDIS_HOST=redis
REDIS_PASSWORD=change-this-password
```

Use the same database username and password for the PostgreSQL container. Compose defaults `POSTGRES_DB` to `frietsync`. Set real credentials for integrations you use; do not commit `backend\.env` or paste secrets into the README.

Start the backend, PostgreSQL, and Redis:

```powershell
docker compose --env-file backend\.env -f backend\docker-compose.yml up --build -d
docker compose --env-file backend\.env -f backend\docker-compose.yml ps
```

Follow backend logs and check the health endpoint:

```powershell
docker compose --env-file backend\.env -f backend\docker-compose.yml logs -f backend
curl.exe http://localhost:8080/api/v1/check
```

The health endpoint should return `{"status":"ok"}`. The backend is published on `http://localhost:8080`; PostgreSQL and Redis are available to the backend over the Compose network and are not published to host ports by default. `docker compose down` stops the services but preserves the named data volumes; `docker compose down -v` also deletes the local database and Redis data.

### Run the application directly

Start PostgreSQL (with pgvector) and Redis separately, then configure `backend\.env` to use host-accessible addresses, for example `localhost` for both services. From the `backend` directory, run:

```powershell
.\mvnw.cmd spring-boot:run
```

For this mode, `DB_URL` should point to the local PostgreSQL host, such as `jdbc:postgresql://localhost:5432/frietsync`. Do not use the Docker service names `postgres` or `redis` when running the application directly on the host.

### Configuration and external services

[`backend/.env.example`](backend/.env.example) lists the configuration keys consumed by `application.properties`. Copy it to `backend\.env` and replace placeholders. Never commit a populated `.env` file.

| Configuration | Used for |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_SSL` | Redis connection |
| `JWT_SECRET`, `JWT_EXPIRATION_MS` | Access-token signing and expiry |
| `REFRESH_COOKIE_SECURE`, `REFRESH_COOKIE_SAME_SITE`, `ALLOWED_ORIGINS` | Cookie and CORS policy |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | OTP and notification email |
| `AWS_REGION`, `AWS_S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | Uploaded files and generated assets in AWS S3; session token is optional |
| `GEMINI_API_KEY` | Gemini chat and embedding requests |
| `GOOGLE_*`, `GITHUB_*` | OAuth client credentials, callbacks, encryption keys, and frontend redirect URLs |
| `CLAMAV_HOST`, `CLAMAV_PORT`, `CLAMAV_ENABLED` | Malware scanning for supported uploads |
| `JANUS_*` | Project voice/video calls |
| `RECAPTCHA_*` | Optional signup verification |
| `INVITE_VALIDITY_DAYS`, `INVITES_URL` | Invitation expiry and frontend link |
| `FLYWAY_ENABLED`, `JPA_DDL_AUTO` | Schema migration and Hibernate validation behavior |

The Compose stack includes PostgreSQL, Redis, and the backend only. Configure an AWS S3 bucket and Gemini key for document uploads and AI features. If `CLAMAV_ENABLED=true`, a reachable ClamAV server is required for uploads; for local development without ClamAV, disable scanning explicitly. OAuth callbacks must exactly match the URLs configured at the identity provider. Voice/video calls additionally require Janus; the optional `voice` Compose profile expects a Janus configuration directory. Enable that profile with `docker compose --env-file backend\.env -f backend\docker-compose.yml --profile voice up -d`.

### Build and test

From `backend`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
.\mvnw.cmd checkstyle:check
```

The Dockerfile builds the application with tests skipped, so run the test command separately when validating code changes. Database schema changes belong in a new Flyway migration; do not modify migrations already applied to shared or deployed databases.

## APIs

The backend routes are grouped under `/api/v1`, with selected v2 endpoints under `/api/v2`. Protected endpoints require `Authorization: Bearer <access-token>` unless noted otherwise. An `Authenticated` access label means the endpoint needs a valid access token; project/workspace membership, resource ownership, and role checks are enforced separately by the relevant service.

The API tables below describe the main routes and feature-specific behavior. For the live request schemas and a try-it-out interface, run the backend and open [Swagger UI](http://localhost:8080/swagger-ui.html); the generated OpenAPI document is available at `http://localhost:8080/v3/api-docs`. Multipart uploads, binary downloads, OAuth callbacks, and WebSocket messages have additional notes in their feature sections below.

| Method | URL | Access | Request body / notes |
|---|---|---|---|
| GET | `/api/v1/check` | Public | Health check |
| POST | `/api/v1/auth/signup` | Public | `name, email, password` |
| POST | `/api/v1/auth/resend-otp` | Public | `email` |
| POST | `/api/v1/auth/verify-otp` | Public | `email, code` |
| POST | `/api/v1/auth/login` | Public | `email, password`; sets the refresh-token cookie |
| POST | `/api/v1/auth/forgot-password` | Public | `email` |
| POST | `/api/v1/auth/reset-password` | Public | `email, code, newPassword` |
| POST | `/api/v1/auth/refresh` | Refresh-token cookie | No body; returns a new `accessToken` and refresh-token cookie |
| POST | `/api/v1/auth/logout` | Refresh-token cookie | No body; invalidates the refresh token and clears the cookie |
| POST | `/api/v1/admin/invites` | Admin | `email, purpose`, role when required, optional `projectId` and `expiresAt` |
| POST | `/api/v1/admin/invites/revoke` | Admin | `inviteId` |
| GET | `/api/v1/invites/me` | Authenticated | Lists the current user's pending invites |
| POST | `/api/v1/invites/accept` | Authenticated | `inviteId` |
| POST | `/api/v1/invites/reject` | Authenticated | `inviteId` |
| POST | `/api/v1/projects/{projectId}/sprints` | Authenticated | `name, startDate, endDate`, optional `goal` |
| GET | `/api/v1/projects/{projectId}/sprints` | Authenticated | Lists a project's sprints |
| GET | `/api/v1/sprints/{id}` | Authenticated | Gets a sprint |
| PATCH | `/api/v1/sprints/{id}` | Authenticated | Optional `name, goal, startDate, endDate` fields |
| POST | `/api/v1/sprints/{id}/start` | Authenticated | Starts a planned sprint |
| POST | `/api/v1/sprints/{id}/complete` | Authenticated | Completes an active sprint |
| DELETE | `/api/v1/sprints/{id}` | Authenticated | Deletes a sprint |

## Additional backend endpoints

The backend also exposes routes for workspaces, projects, issues, meetings, polls, releases, milestones, notifications, chat, community, canvases, calls, threads, semantic search, and AI chat. Unless specifically listed as public above, these endpoints require a valid access token; the service applies the relevant workspace, project, resource, and role checks.

### GitHub integration

Configure `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`, `GITHUB_REDIRECT_URI` (the backend callback URL), `GITHUB_OAUTH_ENCRYPTION_KEY` (base64 for 32 random bytes), and `GITHUB_OAUTH_FRONTEND_RESULT_URI`. `GITHUB_OAUTH_STATE_TTL_SECONDS` is optional and defaults to 600. The OAuth app requests `read:user` and `repo` so the chosen project repository can be read, including private repositories. Access tokens are encrypted at rest.

| Method | Route | Purpose |
| --- | --- | --- |
| GET | `/api/v1/integrations/github/connect` | Return `{ "authorizationUrl": "..." }` for the signed-in user's OAuth flow |
| GET | `/api/v1/integrations/github/callback` | GitHub OAuth callback; redirects to the frontend with `?github=connected`, `denied`, or `failed` |
| DELETE | `/api/v1/integrations/github/disconnect` | Remove the signed-in user's GitHub connection |
| PUT | `/api/v1/integrations/github/projects/{projectId}/repository` | Link `{ "owner": "...", "repository": "..." }`; project admin, manager, or team lead only |
| DELETE | `/api/v1/integrations/github/projects/{projectId}/repository` | Unlink the project's repository; project admin, manager, or team lead only |
| GET | `/api/v1/integrations/github/projects/{projectId}/contributions` | Return commit totals and daily counts, pull request totals, and merged pull request totals for project members |

Each member connects their own GitHub account to associate a GitHub login with their FrietSync user. A project admin, manager, or team lead can link a repository that belongs to another person or organization, provided their connected GitHub account has access to it. The repository must be readable by the account performing the contribution request; users without repository access cannot fetch its activity. The member list includes users without a GitHub connection with zero activity. GitHub activity is fetched live and currently paginates up to the most recent 1,000 commits and 1,000 pull requests.

### Workspaces and projects

| Method | URL | Access | Notes |
|---|---|---|---|
| POST | `/api/v1/workspaces` | Authenticated | Create a workspace |
| GET | `/api/v1/workspaces/me` | Authenticated | Get the current workspace |
| GET | `/api/v1/workspaces/{workspaceId}` | Authenticated | Get workspace details |
| PATCH | `/api/v1/workspaces/{workspaceId}` | Authenticated | Update a workspace |
| GET | `/api/v1/workspaces/{workspaceId}/members` | Authenticated | List workspace members |
| POST | `/api/v1/projects` | Admin | Create a project |
| GET | `/api/v1/projects/me` | Authenticated | List the user's projects |
| GET | `/api/v1/projects/{projectId}` | Authenticated | Get a project, including `githubRepositoryOwner` and `githubRepositoryName` when a GitHub repository is linked (both are `null` otherwise) |
| PUT | `/api/v1/projects/{projectId}` | Authenticated | Update a project (`multipart/form-data`) |
| POST | `/api/v1/projects/{projectId}/profile-picture` | Authenticated | Upload a project profile picture (`multipart/form-data`) |
| DELETE | `/api/v1/projects/{projectId}/profile-picture` | Authenticated | Remove a project profile picture |
| DELETE | `/api/v1/projects/{projectId}` | Authenticated | Delete a project |
| GET | `/api/v1/projects/{projectId}/members` | Authenticated | List project members |
| POST | `/api/v1/projects/{projectId}/members` | Authenticated | Add a project member |
| DELETE | `/api/v1/projects/{projectId}/members/{memberUserId}` | Authenticated | Remove a project member |
| POST | `/api/v1/projects/{projectId}/milestones` | Project admin or project manager | Create a milestone |
| GET | `/api/v1/projects/{projectId}/milestones` | Project member or project leader | List milestones |
| PATCH | `/api/v1/milestones/{milestoneId}` | Project admin or project manager | Update a milestone |
| DELETE | `/api/v1/milestones/{milestoneId}` | Project admin or project manager | Delete a milestone |
| POST | `/api/v1/projects/{projectId}/releases` | Authenticated | Create a release |
| GET | `/api/v1/projects/{projectId}/releases` | Authenticated | List releases |
| PATCH | `/api/v1/releases/{releaseId}` | Authenticated | Update a release |
| DELETE | `/api/v1/releases/{releaseId}` | Authenticated | Delete a release |
| POST | `/api/v1/releases/{releaseId}/issues` | Authenticated | Attach issues to a release |
| POST | `/api/v1/releases/{releaseId}/file` | Authenticated | Upload a release file |

### Issues, assignments, and links

| Method | URL | Access | Notes |
|---|---|---|---|
| POST | `/api/v1/projects/{projectId}/issues` | Authenticated | Create an issue |
| GET | `/api/v1/projects/{projectId}/issues` | Authenticated | List project issues |
| GET | `/api/v1/issues/{issueId}` | Authenticated | Get an issue |
| PATCH | `/api/v1/issues/{issueId}` | Authenticated | Update an issue |
| PATCH | `/api/v1/issues/{issueId}/status` | Project admin or project manager | Set the issue status to any `IssueStatus` value |
| DELETE | `/api/v1/issues/{issueId}` | Authenticated | Delete an issue |
| POST | `/api/v1/issues/{issueId}/resolve` | Authenticated | Resolve an issue |
| GET | `/api/v1/issues/{issueId}/history?page=0&size=20` | Authenticated | Paginated history; responses include a readable `message` and retain the raw `action` |
| POST | `/api/v1/issues/{issueId}/review/approve` | Authenticated | Approve an issue review |
| POST | `/api/v1/issues/{issueId}/review/reject` | Authenticated | Reject an issue review |
| POST | `/api/v1/issues/{issueId}/labels` | Authenticated | Add an issue label |
| GET | `/api/v1/issues/{issueId}/labels` | Authenticated | List labels |
| DELETE | `/api/v1/issues/{issueId}/labels/{labelId}` | Authenticated | Remove an issue label |
| GET | `/api/v1/issues/me` | Authenticated | List issues assigned to the current user |
| PUT | `/api/v1/issues/{issueId}/github-issue` | Authenticated | Link an issue to a GitHub issue |
| DELETE | `/api/v1/issues/{issueId}/github-issue` | Authenticated | Remove the GitHub issue link |
| POST | `/api/v1/projects/{projectId}/issue` | Authenticated | Singular alias for issue creation |
| POST | `/api/v1/issues/{issueId}/assign` | Authenticated | Assign an issue directly |

| POST | `/api/v1/issues/{issueId}/request` | Authenticated | Request issue assignment |
| GET | `/api/v1/issues/{issueId}/request` | Authenticated | Get your assignment request |
| DELETE | `/api/v1/issues/{issueId}/request` | Authenticated | Cancel your assignment request |
| GET | `/api/v1/projects/{projectId}/issue-requests` | Authenticated | List assignment requests |
| POST | `/api/v1/issues/{issueId}/issue-requests` | Authenticated | Decide on a request |
| DELETE | `/api/v1/projects/{projectId}/issue-requests?requestId={requestId}` | Authenticated | Reject a request |
| POST | `/api/v1/issues/{issueId}/links` | Authenticated | Link two issues |
| GET | `/api/v1/issues/{issueId}/links` | Authenticated | List issue links |
| DELETE | `/api/v1/issue-links/{linkId}` | Authenticated | Remove an issue link |
| GET | `/api/v1/projects/{projectId}/dependency-graph` | Authenticated | Get project issues and their links |
| POST | `/api/v1/issues/{issueId}/attachments` | Authenticated | Upload attached files |
| GET | `/api/v1/issues/{issueId}/attachments` | Authenticated | List issue attachments |
| DELETE | `/api/v1/attachments/{attachmentId}` | Authenticated | Delete an attachment |
| GET | `/api/v1/attachments/{attachmentId}/download` | Authenticated | Download an attachment |

Project admins can perform all project-manager actions while retaining their existing admin-only actions. Issue creation is allowed for the project's Admin, Project Manager, Team Lead, or a Reporter. Sprint creation is allowed for the Admin, assigned Project Manager, or Team Lead. Project Managers, Admins, and Team Leads can assign issues to contributors; only the Project Manager or Admin can assign a sprint to a contributor. Contributors can request sprint assignment, which the Project Manager or Admin can approve.

Project admins and project managers can set issue and sprint statuses directly through the status endpoints. Existing contributor issue-resolution and assigned-contributor sprint start/complete endpoints keep their current permissions.

### Sprint, meeting, poll, notifications, and chat

| Method | URL | Access | Notes |
|---|---|---|---|
| PATCH | `/api/v1/sprints/{sprintId}/status` | Project admin or project manager | Set the sprint status to any `SprintStatus` value |
| POST | `/api/v1/projects/{projectId}/polls` | Authenticated | Create a poll |
| GET | `/api/v1/projects/{projectId}/polls` | Authenticated | List project polls |
| GET | `/api/v1/polls/{pollId}` | Authenticated | Get a poll |
| PUT | `/api/v1/polls/{pollId}/votes` | Authenticated | Cast a vote |
| POST | `/api/v1/polls/{pollId}/close` | Authenticated | Close a poll |
| POST | `/api/v1/projects/{projectId}/meetings` | Authenticated | Schedule a meeting |
| GET | `/api/v1/meetings` | Authenticated | List your meetings |
| GET | `/api/v1/projects/{projectId}/meetings` | Authenticated | List project meetings |
| PATCH | `/api/v1/meetings/{meetingId}` | Authenticated | Update a meeting |
| DELETE | `/api/v1/meetings/{meetingId}` | Authenticated | Cancel a meeting |
| GET | `/api/v1/projects/{projectId}/chat/messages?page=0&size=50` | Authenticated | Fetch chat messages; `size` is 1–100 |
| GET | `/api/v1/projects/{projectId}/chat/messages/search?q=term&page=0&size=20` | Authenticated | Search project chat messages |
| GET | `/api/v1/projects/{projectId}/chat/messages/around/{messageId}?before=25&after=25` | Authenticated | Fetch a message window; each count is 0–50 |
| PATCH | `/api/v1/projects/{projectId}/chat/messages/{messageId}` | Authenticated | Edit a chat message |
| DELETE | `/api/v1/projects/{projectId}/chat/messages/{messageId}` | Authenticated | Delete a chat message |
| POST | `/api/v1/projects/{projectId}/chat/messages/{messageId}/forward` | Authenticated | Forward a message to another project |
| POST | `/api/v1/projects/{projectId}/chat/attachments` | Authenticated | Upload multipart `file` |
| GET | `/api/v1/projects/{projectId}/chat/attachments?page=0` | Authenticated | List project chat attachments |
| GET | `/api/v1/projects/{projectId}/chat/attachments/{attachmentId}/download` | Authenticated | Download a chat attachment |
| GET | `/api/v1/projects/{projectId}/chat/pinned-messages` | Authenticated | List pinned chat messages |
| PUT | `/api/v1/projects/{projectId}/chat/messages/{messageId}/pin` | Authenticated | Pin a chat message |
| DELETE | `/api/v1/projects/{projectId}/chat/messages/{messageId}/pin` | Authenticated | Unpin a chat message |
| GET | `/api/v1/projects/{projectId}/chat/mute` | Authenticated | Get your project chat mute state |
| PUT | `/api/v1/projects/{projectId}/chat/mute` | Authenticated | Update your project chat mute state |
| DELETE | `/api/v1/projects/{projectId}/chat/mute` | Authenticated | Clear your project chat mute state |
| GET | `/api/v1/notifications` | Authenticated | List notifications |
| POST | `/api/v1/notifications/{id}/read` | Authenticated | Mark a notification as read |
| POST | `/api/v1/notifications/read-all` | Authenticated | Mark all notifications as read |
| GET | `/api/v1/notifications/unread-count` | Authenticated | Get unread count |
| GET | `/api/v1/notifications/preferences` | Authenticated | Get notification preferences |
| PUT | `/api/v1/notifications/preferences` | Authenticated | Update notification preferences |
| DELETE | `/api/v1/users/workspace/leave` | Authenticated | Leave the current workspace |

### Users, workspace lifecycle, comments, documents, and reporting

| Method | URL | Access | Notes |
|---|---|---|---|
| GET | `/api/v1/users/me` | Authenticated | Get the current user's profile |
| PATCH | `/api/v1/users/me` | Authenticated | Update the current user's profile |
| POST | `/api/v1/users/me/profile-picture` | Authenticated | Upload a profile picture (`multipart/form-data`) |
| DELETE | `/api/v1/users/me/profile-picture` | Authenticated | Remove the current user's profile picture |
| DELETE | `/api/v1/users/me` | Authenticated | Delete the current user's account |
| DELETE | `/api/v1/workspaces/{workspaceId}` | Authenticated | Delete a workspace |
| POST | `/api/v1/workspaces/{workspaceId}/restore` | Authenticated | Restore a deleted workspace |
| PATCH | `/api/v2/workspaces/me` | Authenticated | Update the current workspace using the v2 API |
| DELETE | `/api/v2/workspaces/me` | Authenticated | Delete the current workspace using the v2 API |
| GET | `/api/v2/workspaces/me/members` | Authenticated | List current workspace members using the v2 API |
| POST | `/api/v1/issues/{issueId}/comments` | Authenticated | Add an issue comment; accepts optional `mentionedUserIds` |
| GET | `/api/v1/issues/{issueId}/comments` | Authenticated | List issue comments |
| PATCH | `/api/v1/issues/{issueId}/comments/{commentId}` | Authenticated | Edit an issue comment |
| DELETE | `/api/v1/issues/{issueId}/comments/{commentId}` | Authenticated | Delete an issue comment |
| POST | `/api/v1/sprints/{sprintId}/comments` | Authenticated | Add a sprint comment; accepts optional `mentionedUserIds` |
| GET | `/api/v1/sprints/{sprintId}/comments` | Authenticated | List sprint comments |
| PATCH | `/api/v1/sprints/{sprintId}/comments/{commentId}` | Authenticated | Edit a sprint comment |
| DELETE | `/api/v1/sprints/{sprintId}/comments/{commentId}` | Authenticated | Delete a sprint comment |
| GET | `/api/v1/projects/{projectId}/documents` | Authenticated | List project documents |
| POST | `/api/v1/projects/{projectId}/documents` | Authenticated | Create a project document |
| POST | `/api/v1/projects/{projectId}/documents/upload` | Project manager or team lead | Upload a document as multipart `file`; optional `title`; max 25 MB; supports PDF, Word, PowerPoint, Excel, OpenDocument, RTF, text, Markdown, CSV, HTML, XML, and EPUB; extracts text for semantic search and RAG |
| GET | `/api/v1/documents/{documentId}` | Authenticated | Get a document |
| GET | `/api/v1/documents/{documentId}/download` | Project member | Download an uploaded project document |
| PATCH | `/api/v1/documents/{documentId}` | Authenticated | Update a document |
| DELETE | `/api/v1/documents/{documentId}` | Authenticated | Delete a document |
| GET | `/api/v1/projects/{projectId}/presence` | Authenticated | Get current project presence |
| GET | `/api/v1/projects/{projectId}/activity` | Authenticated | Get the latest 10 issue, milestone, and poll-creation events |
| GET | `/api/v1/projects/{projectId}/events?action={action}&page=0&size=20` | Authenticated | Paginated project events; `action` is optional and `size` is 1–100 |
| GET | `/api/v1/projects/{projectId}/summary` | Authenticated | Get the project summary as JSON |
| GET | `/api/v1/projects/{projectId}/board` | Authenticated | Get the project issue board |
| GET | `/api/v1/projects/{projectId}/calendar` | Authenticated | Get project calendar data |
| GET | `/api/v1/projects/{projectId}/timeline` | Authenticated | Get project timeline data |
| GET | `/api/v1/users/{userId}/record` | Authenticated | Get an employee record |
| GET | `/api/v1/users/{userId}/record/issues` | Authenticated | Get issue history for an employee record |
| GET | `/api/v1/users/{userId}/record/sprints` | Authenticated | Get sprint history for an employee record |
| GET | `/api/v1/users/{userId}/record/activity` | Authenticated | Get activity for an employee record |

### Budget, briefing, replay, and integrations

| Method | URL | Access | Notes |
|---|---|---|---|
| PUT | `/api/v1/projects/{projectId}/budget` | Authenticated | Set the project budget |
| GET | `/api/v1/projects/{projectId}/budget` | Authenticated | Get the project budget |
| GET | `/api/v1/projects/{projectId}/budget/report` | Authenticated | Get the project cost report |
| POST | `/api/v1/projects/{projectId}/expenses` | Authenticated | Add a project expense |
| GET | `/api/v1/projects/{projectId}/expenses` | Authenticated | List project expenses |
| PATCH | `/api/v1/expenses/{expenseId}` | Authenticated | Update an expense |
| DELETE | `/api/v1/expenses/{expenseId}` | Authenticated | Delete an expense |
| PUT | `/api/v1/users/{userId}/salary` | Admin | Set salary data for a user |
| GET | `/api/v1/briefing/today` | Authenticated | Get today's daily briefing |
| GET | `/api/v1/briefing?date=YYYY-MM-DD` | Authenticated | Get a briefing for a specific date |
| GET | `/api/v1/briefing/preferences` | Authenticated | Get briefing preferences |
| PUT | `/api/v1/briefing/preferences` | Authenticated | Update briefing preferences |
| GET | `/api/v1/projects/{projectId}/replay/timeline` | Authenticated | Get project replay phases |
| PATCH | `/api/v1/projects/{projectId}/replay/phases/{phaseId}` | Authenticated | Update a replay phase |
| GET | `/api/v1/projects/{projectId}/replay/snapshot?at=2026-10-10T12:00:00Z` | Authenticated | Get a replay snapshot at an optional ISO instant or date |
| POST | `/api/v2/projects/{projectId}/issues` | Authenticated | Create an issue using the v2 API |
| GET | `/api/v1/integrations/google/connect` | Authenticated | Start Google OAuth and return the authorization URL |
| GET | `/api/v1/integrations/google/callback` | OAuth callback | Complete Google OAuth and redirect to the frontend |
| DELETE | `/api/v1/integrations/google/disconnect` | Authenticated | Disconnect the Google account |

### Additional API routes

These routes are part of the current controllers and complement the feature tables above. Unless noted otherwise, they require a valid access token and the corresponding project or workspace access.

| Method | URL | Purpose |
|---|---|---|
| GET | `/api/v1/workspaces/deleted` | List deleted workspaces available to the user |
| POST | `/api/v1/projects/{projectId}/restore` | Restore a deleted project |
| POST | `/api/v1/milestones/{milestoneId}/restore` | Restore a deleted milestone |
| GET, POST | `/api/v1/users/me/skills` | List or add the current user's skills |
| DELETE | `/api/v1/users/me/skills/{skillId}` | Remove one of the current user's skills |
| GET | `/api/v1/projects/{projectId}/issues/checklist` | Get issue checklist data for a project |
| POST | `/api/v1/issues/{issueId}/restore` | Restore a deleted issue |
| GET | `/api/v1/sprints/me` | List sprints assigned to the current user |
| GET | `/api/v1/issues/{issueId}/sprints` | Get sprints associated with an issue |
| POST | `/api/v1/sprints/{sprintId}/restore` | Restore a deleted sprint |
| POST | `/api/v1/sprints/{sprintId}/assign` | Assign a sprint to a project contributor |
| POST | `/api/v1/sprints/{sprintId}/request` | Request assignment to a sprint |
| GET | `/api/v1/projects/{projectId}/sprints-requests` | List sprint assignment requests |
| POST | `/api/v1/sprints/{sprintId}/sprints-requests` | Approve a sprint assignment request |
| GET | `/api/v1/projects/{projectId}/gantt` | Get Gantt chart data |
| GET | `/api/v1/projects/{projectId}/gantt/critical-path` | Get the project's critical path |
| PATCH | `/api/v1/projects/{projectId}/gantt/issues/{issueId}` | Update an issue's Gantt scheduling data |
| PATCH | `/api/v1/projects/{projectId}/gantt/sprints/{sprintId}` | Update a sprint's Gantt scheduling data |
| POST, DELETE | `/api/v1/projects/{projectId}/gantt/baseline` | Create or remove the Gantt baseline |
| POST | `/api/v1/issues/{issueId}/review/approve` | Approve an issue review |
| POST | `/api/v1/issues/{issueId}/review/reject` | Reject an issue review |
| GET | `/api/v1/search/semantic?q={query}` | Search indexed project content semantically; optional `projectId`, `type`, and `status` filters |
| POST | `/api/v1/ai-chat/ask` | Ask the RAG chatbot a question |
| PUT | `/api/v1/ai-chat/messages/{messageId}/feedback` | Save feedback for an AI chat response |
| GET, POST | `/api/v1/projects/{projectId}/threads` | List or create project discussion threads |
| GET, DELETE | `/api/v1/projects/{projectId}/threads/{threadId}` | Get or delete a thread |
| POST | `/api/v1/projects/{projectId}/threads/{threadId}/comments` | Add a comment or nested reply to a thread |
| POST | `/api/v1/projects/{projectId}/threads/{threadId}/upvote` | Toggle the current user's upvote on a thread |
| POST, GET | `/api/v1/projects/{projectId}/canvases` | Create or list project canvases |
| GET, PATCH, DELETE | `/api/v1/canvases/{canvasId}` | Get, rename, or delete a canvas |
| POST | `/api/v1/projects/{projectId}/calls` | Start a project call; body may specify `type` (`VOICE` by default) |
| GET | `/api/v1/projects/{projectId}/calls/active` | Get the active call, optionally filtered by `type` |
| POST | `/api/v1/projects/{projectId}/calls/{callId}/join` | Join a call and get its connection details |
| POST | `/api/v1/projects/{projectId}/calls/{callId}/leave` | Leave a call |
| POST | `/api/v1/projects/{projectId}/calls/{callId}/end` | End a call |
| POST, GET | `/api/v1/workspaces/{workspaceId}/community/posts` | Create or list workspace community posts |
| GET | `/api/v1/workspaces/{workspaceId}/community/posts/pinned` | List pinned community posts |
| GET, PATCH, DELETE | `/api/v1/workspaces/{workspaceId}/community/posts/{postId}` | Get, edit, or delete a post |
| POST, DELETE | `/api/v1/workspaces/{workspaceId}/community/posts/{postId}/pin` | Pin or unpin a post |
| PUT, DELETE | `/api/v1/workspaces/{workspaceId}/community/posts/{postId}/reaction` | Add or remove a post reaction |
| POST, GET | `/api/v1/workspaces/{workspaceId}/community/posts/{postId}/comments` | Add or list post comments |
| PATCH, DELETE | `/api/v1/workspaces/{workspaceId}/community/posts/{postId}/comments/{commentId}` | Edit or delete a comment |
| PUT, DELETE | `/api/v1/workspaces/{workspaceId}/community/posts/{postId}/comments/{commentId}/reaction` | Add or remove a comment reaction |
| POST | `/api/v2/projects/{projectId}/issues` | Create an issue using the v2 API |

Community posts and comments are paginated with `page` (default `0`) and `size` (default `20`). Authors and users with the `ADMIN` role can edit or delete posts and comments; only users with the `ADMIN` role can pin or unpin posts. For precise request and response schemas, use Swagger UI.

### Documents, semantic search, and RAG

Text documents are created with `POST /api/v1/projects/{projectId}/documents` using JSON:

```json
{
  "title": "Deployment notes",
  "content": "The release is deployed to staging before production."
}
```

`title` is required; `content` is optional and limited to 50,000 characters. File uploads use `multipart/form-data` with a required `file` part and optional `title`; the upload limit is 25 MB. Supported formats include PDF, DOC/DOCX, PPT/PPTX, XLS/XLSX, ODT/ODS/ODP, RTF, TXT, Markdown, CSV, HTML, XML, and EPUB. Uploaded files are stored in the configured AWS S3 bucket; authorized project members download the original file through `/api/v1/documents/{documentId}/download`. The upload response includes the document ID, filename, content type, size, download URL, and extracted text.

Example:

```bash
curl -X POST "http://localhost:8080/api/v1/projects/<project-id>/documents/upload" \
  -H "Authorization: Bearer <access-token>" \
  -F "file=@project-plan.pdf" \
  -F "title=Project plan"
```

Apache Tika extracts readable text on upload. Up to 50,000 characters are stored and split into overlapping chunks for embedding. These chunks are included in semantic search and RAG retrieval. Password-protected, malformed, or otherwise unextractable files may be rejected. Scanned/image-only PDFs need OCR to yield searchable text; OCR is not configured.

Semantic search accepts `q` and optional filters:

```http
GET /api/v1/search/semantic?q=deployment%20plan&projectId=<project-uuid>&type=DOCUMENT
Authorization: Bearer <access-token>
```

The AI chat endpoint accepts a question and optional project scope:

```http
POST /api/v1/ai-chat/ask
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "projectId": "<project-uuid>",
  "question": "What does the uploaded deployment plan say about rollback?"
}
```

`projectId` may be omitted to search across projects the user can access. The response includes `messageId`, an `answer`, and source references when available. Use `PUT /api/v1/ai-chat/messages/{messageId}/feedback` to submit feedback for a response. AI and embedding calls require a configured Gemini API key.

### Project threads

Threads are project-scoped discussions. Members of an active project can list and create threads, add comments or nested replies (`parentCommentId`), and toggle an upvote. Create a thread with `{"title":"...", "content":"..."}`; add a comment with `{"content":"...", "parentCommentId":null}`. Thread titles are limited to 200 characters, content to 10,000, and comments to 2,000. Listing defaults to 20 newest threads per page; sorting supports `createdAt`, `upvotes`, or `title`. A thread can be deleted by its author, a project admin, or a project manager. Thread creation, comments, deletion, and upvotes require an active project.

### Workspace community and collaborative canvases

Workspace community posts support paging, pinning, reactions, and comments. The author can edit or remove their content; moderation actions are permission-checked by the service. Changes are broadcast to `/topic/workspaces/{workspaceId}/community`.

Canvases are created and managed through the REST routes above. Element create, update, delete, and convert-to-issue operations are sent over STOMP. Include an element's `revision` from the latest canvas response in update payloads to reject stale edits; updates without it are accepted for clients that do not yet send revisions. Canvas updates are broadcast to `/topic/canvases/{canvasId}`; conversion results and socket errors are delivered to the user's queue.

### Project calls

The calls API starts, joins, leaves, and ends voice/video calls. Janus provides the media server; configuring the API alone does not provide a media server. Configure `JANUS_BASE_URL` and related Janus secrets, then run a reachable Janus instance (the Compose `voice` profile is optional and requires the configured Janus files). Call lifecycle changes are broadcast on `/topic/projects/{projectId}/calls`.

The application also supports authenticated STOMP over `/ws`. Include `Authorization: Bearer <access-token>` in the STOMP `CONNECT` headers. Client messages go to `/app/...`; server broadcasts are received from `/topic/...`; user-specific events use `/user/queue/...`.

| Feature | Client sends/subscribes to | Server sends to |
|---|---|---|
| Project chat | Send messages to `/app/projects/{projectId}/chat`; send typing events to `/app/projects/{projectId}/chat/typing` | Subscribe to `/topic/projects/{projectId}/chat` and `/topic/projects/{projectId}/chat/typing` |
| Project activity | Subscribe to `/app/projects/{projectId}/activity` for the current snapshot | Subscribe to `/topic/projects/{projectId}/activity` for refreshed latest-10 activity |
| Issue history | Subscribe to `/topic/issues/{issueId}/history` | Issue history updates, including a readable `message` and the raw action |
| Poll results | Subscribe to `/topic/polls/{pollId}/results` | Poll created, updated, and closed events |
| AI chat streaming | Send a `RagChatRequest` to `/app/ai-chat` | Subscribe to `/user/queue/ai-chat`; events are `started`, `chunk`, `completed`, or `error` |
| Canvas collaboration | Send create/update/delete/convert-to-issue messages under `/app/canvases/{canvasId}/elements/...` | Subscribe to `/topic/canvases/{canvasId}`; conversion results and canvas errors use user queues |
| Calls | No client send destination | Subscribe to `/topic/projects/{projectId}/calls` for call events |
| Workspace community | No client send destination | Subscribe to `/topic/workspaces/{workspaceId}/community` for post/comment changes |
| Notifications | No client send destination | Subscribe to `/user/queue/notifications` |

Activity snapshots contain the latest 10 issue, milestone, and poll-creation events. WebSocket connections require a valid access token; protected destinations and operations are checked by the backend.

Notifications are generated for issue assignment and review activity, project membership and role changes, comments and mentions, sprint assignment requests and assignments, poll creation, meetings, invites, and chat messages. To mention users in an issue or sprint comment, include their project participant IDs in the optional `mentionedUserIds` array alongside `content` (up to 50 IDs). Notifications are available through the notifications API and the `/user/queue/notifications` WebSocket destination. Email delivery follows each user's notification preferences.

#### Poll API request and response formats

Poll routes require `Authorization: Bearer <access-token>`. Any authenticated project member can create, view, and vote in polls. Only the poll creator, project manager, or project admin can close one.

Create a poll with `POST /api/v1/projects/{projectId}/polls`:

```json
{
  "question": "Which approach should we use?",
  "description": "Optional background",
  "options": ["Approach A", "Approach B"],
  "endsAt": "2026-10-20T12:00:00Z",
  "mode": "SINGLE",
  "issueId": null,
  "sprintId": null,
  "milestoneId": null
}
```

`question` is required (up to 200 characters). `options` must contain 2–20 unique, nonblank values, each up to 300 characters. `description` is optional (up to 400 characters). `endsAt` is an optional future ISO-8601 timestamp; the server rejects votes at or after this deadline. `mode` is `SINGLE` or `MULTIPLE`; if omitted, it defaults to `SINGLE`. At most one of `issueId`, `sprintId`, or `milestoneId` can be provided, and the linked item must belong to the specified project. Creation returns **201 Created**.

Other poll routes:

| Method | Endpoint | Purpose | Request body |
|---|---|---|---|
| GET | `/api/v1/projects/{projectId}/polls` | List polls in the project | None |
| GET | `/api/v1/polls/{pollId}` | Get a poll and its current results | None |
| PUT | `/api/v1/polls/{pollId}/votes` | Cast or replace your vote | One of the vote bodies below |
| POST | `/api/v1/polls/{pollId}/close` | Close a poll and save its decision record | Optional `{"decisionRecord":"The selected approach and rationale"}` |

Single-choice vote body:

```json
{
  "optionId": "<option-uuid>"
}
```

Multiple-choice vote body:

```json
{
  "optionIds": ["<option-uuid-1>", "<option-uuid-2>"]
}
```

Use either `optionId` or `optionIds`, not both. The selected options must belong to that poll. A vote can be changed by sending another vote request while the poll remains open.

Poll endpoints return a `PollResponse` (or a list of them) with this shape:

```json
{
  "id": "<poll-uuid>",
  "projectId": "<project-uuid>",
  "question": "Which approach should we use?",
  "description": "Optional background",
  "status": "OPEN",
  "createdBy": "<user-uuid>",
  "issueId": null,
  "sprintId": null,
  "milestoneId": null,
  "endsAt": "2026-10-20T12:00:00Z",
  "closedAt": null,
  "decisionRecord": null,
  "createdAt": "2026-10-15T09:00:00Z",
  "options": [
    {"id": "<option-uuid-1>", "label": "Approach A", "order": 0, "votes": 2},
    {"id": "<option-uuid-2>", "label": "Approach B", "order": 1, "votes": 1}
  ],
  "myVote": "<option-uuid-1>",
  "myVotes": ["<option-uuid-1>"]
}
```

`myVote` is the current user's first selected option (or `null` when they have not voted); `myVotes` contains all of their selected options. A background scheduler checks for expired polls every 10 seconds and closes them. `decisionRecord` is optional and limited to 4,000 characters.

Polls also publish live updates over the authenticated STOMP connection at `/ws`. Subscribe to `/topic/polls/{pollId}/results` to receive events with `type` (`POLL_CREATED`, `POLL_UPDATED`, or `POLL_CLOSED`) and the updated `poll` response. `POLL_CLOSED` includes final option vote totals. Continue using REST to create polls, vote, and close; successful changes and deadline closures are broadcast after the database transaction commits. Use REST list/get endpoints for initial state, then apply WebSocket events for live updates.

Protected routes use `Authorization: Bearer <access-token>`, except refresh and logout, which use the `refreshToken` HTTP-only cookie set during login. 

### Project dashboard data

The project page can use the existing endpoints below; no dashboard-specific route is required:

| Endpoint | Dashboard data |
|---|---|
| `GET /api/v1/projects/{projectId}` | Project details and progress, plus `projectManager` and `teamLead` summaries (`id`, `name`, `role`) when assigned |
| `GET /api/v1/projects/{projectId}/members` | Project member names, emails, roles, and IDs |
| `GET /api/v1/projects/{projectId}/board` | Issue columns and card data, including assignee (`id`, `name`), labels (`id`, `name`, `color`), `assignedToCurrentUser`, and top-level `myTasksCount` |
| `GET /api/v1/projects/{projectId}/summary` | Issue and sprint totals, status counts, overdue counts, and estimated-hour summaries; use issue counts for task overview charts |
| `GET /api/v1/projects/{projectId}/sprints` | Sprint details including status and assigned contributor name and ID |
| `GET /api/v1/projects/{projectId}/milestones` | Milestone due date, progress, and completion state |
| `GET /api/v1/projects/{projectId}/activity` | Latest 10 issue, milestone, and poll-creation events with actor and message details |
| `GET /api/v1/projects/{projectId}/polls` | Polls ordered newest first, with option vote totals; the first entry is the latest poll |

The board endpoint reports issue status and priority but does not track a separate issue completion percentage or link issues to sprints. Render task progress from the project summary status counts, and render sprint counts from sprint statuses. The project manager and team lead summaries contain names but not profile image URLs.

Each board response now includes a project-wide `myTasksCount`; each issue card includes its first assignee (or `null`), all labels, and whether the authenticated user is assigned:

```json
{
  "columns": [
    {
      "status": "OPEN",
      "allowedTransitions": ["IN_PROGRESS"],
      "issues": [
        {
          "id": "<issue-uuid>",
          "issueNumber": 205,
          "title": "Setup analytics tracking",
          "priority": "MEDIUM",
          "dueDate": "2026-10-22",
          "assignee": {"id": "<user-uuid>", "name": "Alex Morgan"},
          "labels": [{"id": "<label-uuid>", "name": "Analytics", "color": "#21a5d8"}],
          "assignedToCurrentUser": true
        }
      ]
    }
  ],
  "myTasksCount": 6
}
```

Project responses include `projectManager` and `teamLead` as `null` when unassigned, or as `{ "id", "name", "role" }` when assigned. Sprint responses add `assignedContributorName`, which is `null` when no contributor is assigned.

`GET /api/v1/check` returns:

```json
{
  "status": "ok"
}
```

This endpoint can be used to check whether the server is running.

## Invites

Admins can create invites using:

```http
POST /api/v1/admin/invites
```

The admin must provide `email` and `purpose`. For workspace and project invites, include an allowed `role`; project invites also include `projectId`. `expiresAt` is optional; if omitted, the invite expires after the default validity period.

Supported `purpose` values are `WORKSPACE_INVITE`, `PROJECT_INVITE`, and `OWNERSHIP_TRANSFER`.

Allowed invite roles are:

- `PROJECT_MANAGER`
- `TEAM_LEAD`
- `CONTRIBUTOR`
- `REPORTER`

`ADMIN` cannot be assigned through an invite.

If provided, `expiresAt` must be an ISO-8601 timestamp in the future.

Example:

```json
{
  "email": "collaborator@example.com",
  "purpose": "WORKSPACE_INVITE",
  "role": "PROJECT_MANAGER",
  "expiresAt": "2030-05-01T12:00:00Z"
}
```

The specified expiration date is used for the invite.

Authenticated users can manage their pending invites using:

```http
GET  /api/v1/invites/me
POST /api/v1/invites/accept
POST /api/v1/invites/reject
```

Accepting or rejecting an invite requires:

```json
{
  "inviteId": "<invite UUID>"
}
```

Admins can revoke a pending invite using:

```http
POST /api/v1/admin/invites/revoke
```

This endpoint requires an admin access token:

```http
Authorization: Bearer <admin-access-token>
```

All routes under `/api/v1/admin/**` require the `ADMIN` role.

Invite responses include:

- `createdAt`
- `acceptedAt` — `null` until the invite is accepted
- `expiresAt`

Expired invites are marked as `EXPIRED` when the recipient loads their pending invites.

> **Default expiration:** New invites without `expiresAt` use the default invite validity period. Older invites without an expiration are also treated as expiring after seven days.

## Sprints

Sprint endpoints require authentication and use the `/api/v1` prefix.

When a sprint is linked to a milestone, its end date must be strictly earlier than the milestone due date. Creating or updating a sprint with an end date on or after that due date returns **400 Bad Request**. Moving a milestone due date to the same day as, or earlier than, any linked sprint's end date also returns **400 Bad Request** with `Sprint due date needs to be before the milestone date`.

A milestone is automatically marked complete when it has at least one linked sprint and every linked sprint is `COMPLETED`. Its response includes `completed` and `completedAt`; reopening, deleting, or moving a sprint out of the milestone clears its completion state if the condition no longer holds.

The assigned project manager or team lead can create a sprint. Only the project manager can assign a sprint to a project contributor.

Create a sprint:

```http
POST /api/v1/projects/{projectId}/sprints
```

```json
{
  "name": "Sprint 1",
  "goal": "Deliver the initial feature set",
  "startDate": "2026-10-05",
  "endDate": "2026-10-19"
}
```

`name`, `goal`, `startDate`, and `endDate` are required. Dates use the `YYYY-MM-DD` format. New sprints start with status `PLANNED`; use `POST /api/v1/sprints/{id}/start` to move one to `ACTIVE`, then `POST /api/v1/sprints/{id}/complete` to move it to `COMPLETED`.

`PATCH /api/v1/sprints/{id}` accepts any subset of `name`, `goal`, `startDate`, `endDate`, and `milestoneId`. `DELETE /api/v1/sprints/{id}` deletes the sprint.

---

## Signup

```http
POST /api/v1/auth/signup
```

Request body:

```json
{
  "name": "Test User",
  "email": "test@example.com",
  "password": "Secure1!"
}
```

The name may contain letters and spaces. The password must be 6–20 characters and contain at least one uppercase letter, one lowercase letter, one number, and one symbol.

If the email is already registered, the API returns an error.

---

## Login

```http
POST /api/v1/auth/login
```

Request body:

```json
{
  "email": "test@example.com",
  "password": "Secure1!"
}
```

If the credentials are correct, the API returns the user details and an access token. The refresh token is set as an HTTP-only cookie and is not included in the JSON response.

Example:

```json
{
  "user": {
    "id": "...",
    "name": "Test User",
    "email": "test@example.com",
    "role": "ADMIN"
  },
  "accessToken": "eyJ..."
}
```

## How JWT authentication works

The authentication flow works as follows:

1. The user logs in with their email and password.
2. The server validates the credentials, returns an access token, and sets the refresh token as an HTTP-only cookie.
3. The client sends the access token with requests to protected endpoints in the `Authorization` header:

```http
Authorization: Bearer <access-token>
```

4. `JwtAuthFilter` reads the token and `JwtUtil` validates it.
5. If the token is valid, the user's identity is placed in the Spring Security context.
6. `SecurityConfig` and the relevant service check whether that user may access the requested endpoint.

### Access control

- `/api/v1/auth/**` is publicly accessible because users need to access authentication endpoints before they have an access token
- `/api/v1/check` is publicly accessible
- `/api/v1/admin/**` requires the `ADMIN` role
- All other endpoints require a valid access token

### Access token and refresh token

The **access token** is used to authenticate normal API requests.

When the access token expires, the frontend can send the **refresh token** to:

```http
POST /api/v1/auth/refresh
```

with the refresh-token cookie automatically included in the request. The endpoint does not accept a JSON refresh-token body.

If the refresh token is valid, the server issues a new access token.

The user does not need to log in again every time the access token expires, as long as the refresh token is still valid.

Logout invalidates the refresh token so that it cannot be used to obtain new access tokens.

> The exact expiration times for the access token and refresh token should be kept in the application's configuration. Do not assume the access token is valid for 24 hours unless that is the configured value.

---

## Password recovery

The forgot-password flow works in three steps.

### 1. Request an OTP

```http
POST /api/v1/auth/forgot-password
```

The user provides their email address.

The server:

1. Checks whether the account exists.
2. Generates a 6-digit OTP.
3. Saves the OTP.
4. Sends the OTP to the user's email.

### 2. Verify the OTP

```http
POST /api/v1/auth/verify-otp
```

Request body:

```json
{
  "email": "test@example.com",
  "code": "123456"
}
```

The server checks whether the OTP:

- belongs to the specified email
- has the correct purpose
- has not expired
- has not already been used
- matches the provided code

### 3. Reset the password

```http
POST /api/v1/auth/reset-password
```

Request body:

```json
{
  "email": "test@example.com",
  "code": "123456",
  "newPassword": "newPassword123"
}
```

The server validates the OTP again before allowing the password to be changed.

After successful validation, the new password is BCrypt-hashed and saved.

The OTP is then marked as used.

## OTP Rules

- OTPs contain 6 digits
- OTPs are valid for 10 minutes
- Each OTP has a purpose, such as `SIGNUP` or `RESET_PASSWORD`
- An OTP can only be used for its intended purpose
- When a new OTP is generated for the same email and purpose, the previous OTP is invalidated
- Used OTPs cannot be reused
- Expired OTPs cannot be used

## Security Notes

- Passwords are never stored as plain text
- Access tokens must be sent using the `Authorization: Bearer <token>` header
- Admin endpoints require the `ADMIN` role
- Refresh tokens are required to obtain new access tokens after access-token expiration
- Refresh tokens are invalidated during logout
- OTPs expire after their configured validity period
- OTPs are single-use
- Use HTTPS in production and set `REFRESH_COOKIE_SECURE=true`
- Restrict `ALLOWED_ORIGINS` to the actual frontend origins
- Use strong, private values for JWT, OAuth encryption, database, SMTP, S3, and Janus credentials; never commit populated `.env` files

## Troubleshooting

| Symptom | Checks |
|---|---|
| Backend cannot connect to PostgreSQL or Redis in Docker | Use `postgres` and `redis` as the service hostnames in `backend\.env`; `localhost` inside a container refers to that container, not the host |
| Backend fails Hibernate schema validation | Confirm Flyway is enabled and inspect backend logs for migration errors. New migrations must use the `V<number>__description.sql` naming format and a version higher than the latest applied migration |
| Document upload fails | Verify the file extension and 25 MB limit, AWS S3 bucket/credentials, and ClamAV availability when `CLAMAV_ENABLED=true` |
| RAG does not find a document | Confirm extraction completed, the file has selectable text, indexing/embedding succeeded, and the requester can access the project. Scanned PDFs require OCR, which is not configured |
| WebSocket connection or subscription is rejected | Send a valid, unexpired bearer token in the STOMP `CONNECT` `Authorization` header, use an allowed origin, and subscribe/send to a supported destination for a project or workspace the user can access |
| Google or GitHub OAuth fails | Check that the provider's registered callback URL exactly matches `GOOGLE_REDIRECT_URI` or `GITHUB_REDIRECT_URI`, and verify client credentials and encryption-key configuration |
| Docker services appear stopped | From the repository root, run `docker compose --env-file backend\.env -f backend\docker-compose.yml ps` and inspect `docker compose --env-file backend\.env -f backend\docker-compose.yml logs backend`; use `GET /api/v1/check` after startup to check the HTTP service |
