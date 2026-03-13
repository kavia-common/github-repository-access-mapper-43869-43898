# GitHub Access Report Backend

A Spring Boot service that connects to GitHub and generates a report showing which users have access to which repositories within a given organization or user account.

## Features

- **Secure Authentication**: Authenticates with GitHub using a Personal Access Token (PAT)
- **Organization & User Support**: Works with both GitHub organizations and individual user accounts
- **URL Parsing**: Accepts org/user names or full GitHub URLs (e.g., `https://github.com/myorg`)
- **Concurrent API Calls**: Uses thread pools for parallel fetching of collaborator data across repositories
- **Scalable**: Designed for organizations with 100+ repositories and 1000+ users
- **Paginated API Usage**: Automatically handles GitHub API pagination
- **Error Handling**: Graceful handling of 401/403/404/429 errors with descriptive messages
- **OpenAPI Documentation**: Swagger UI available at `/swagger-ui.html`
- **Health Check**: Lightweight `/healthz` endpoint for monitoring

## Prerequisites

- Java 17 or higher
- Maven 3.8+ (or use the included Maven wrapper)

## Setup & Running

### 1. Configure Environment Variables

Create a `.env` file in the project root (or set environment variables):

```bash
# Required: GitHub Personal Access Token
# Needs 'repo' scope for private repos, 'read:org' for org membership
GITHUB_TOKEN=ghp_your_personal_access_token_here

# Required: GitHub organization or user name (or full URL)
GITHUB_ORG=your-org-name
# OR: GITHUB_ORG=https://github.com/your-org-name

# Optional: Server port (default: 3001)
PORT=3001
```

### 2. Generate a GitHub Personal Access Token

1. Go to [GitHub Settings > Developer Settings > Personal Access Tokens](https://github.com/settings/tokens)
2. Click "Generate new token (classic)"
3. Select the following scopes:
   - `repo` — Full control of private repositories (needed to list collaborators)
   - `read:org` — Read org membership (needed for organization repos)
4. Copy the generated token and set it as `GITHUB_TOKEN`

### 3. Build & Run

Using Maven:

```bash
cd github_access_report_backend
mvn clean package -DskipTests
java -jar target/github-access-report-1.0.0.jar
```

Or using the Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

The service will start on port **3001** by default.

## API Endpoints

### Generate Access Report (Default Org)

```
GET /api/report
```

Generates a report for the organization/user configured via `GITHUB_ORG`.

**Example:**
```bash
curl http://localhost:3001/api/report
```

### Generate Access Report (Specific Owner)

```
GET /api/report/{owner}
```

Generates a report for a specific organization or user, overriding the default.

**Example:**
```bash
curl http://localhost:3001/api/report/octocat
```

### Health Check

```
GET /healthz
```

Returns service health status.

**Example:**
```bash
curl http://localhost:3001/healthz
```

### API Documentation

- **Swagger UI**: [http://localhost:3001/swagger-ui.html](http://localhost:3001/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:3001/api-docs](http://localhost:3001/api-docs)

## Sample Response

```json
{
  "organization": "myorg",
  "generated_at": "2024-01-15T10:30:00Z",
  "total_repositories": 25,
  "total_users": 12,
  "users": [
    {
      "username": "alice",
      "avatar_url": "https://avatars.githubusercontent.com/u/123",
      "repositories": [
        {
          "repository": "backend-api",
          "full_name": "myorg/backend-api",
          "role": "admin",
          "private": true
        },
        {
          "repository": "frontend-app",
          "full_name": "myorg/frontend-app",
          "role": "write",
          "private": false
        }
      ]
    }
  ]
}
```

## Design Decisions & Assumptions

1. **Org vs User Detection**: The service first tries the `/orgs/{owner}/repos` endpoint. If it returns 404, it falls back to `/users/{owner}/repos`. This allows the same endpoint to work for both organizations and individual users.

2. **Concurrency Model**: A fixed-size thread pool (configurable via `github.api.max-concurrent-requests`) is used to fetch collaborators in parallel. This avoids sequential API calls that would be too slow for 100+ repositories.

3. **Collaborator Fallback**: If fetching collaborators for a specific repo fails (e.g., due to insufficient permissions on a fork), that repo is included in the report with an empty collaborator list rather than failing the entire report.

4. **Pagination**: All GitHub API list endpoints are automatically paginated using the `Link` header, fetching up to 100 items per page (GitHub's maximum).

5. **Rate Limiting**: The service respects GitHub API rate limits. If a 429 response is received, it's propagated as a clear error message. The concurrency limit helps avoid hitting rate limits too quickly.

6. **Permission Resolution**: The service resolves the highest permission level from GitHub's permissions object (admin > maintain > write/push > triage > read/pull).

7. **URL Parsing**: The `GITHUB_ORG` variable can be a plain name or a full GitHub URL. The service extracts the org/user name from URLs like `https://github.com/orgname`.

## Error Handling

| HTTP Status | Meaning |
|-------------|---------|
| 400 | Missing configuration (GITHUB_TOKEN or GITHUB_ORG not set) |
| 401 | Invalid GitHub token |
| 403 | Insufficient permissions or secondary rate limit |
| 404 | Organization/user not found |
| 429 | GitHub API rate limit exceeded |
| 502 | Unexpected GitHub API error |

## Project Structure

```
src/main/java/com/kavia/githubaccess/
├── GithubAccessReportApplication.java    # Application entry point
├── config/
│   ├── CorsConfig.java                   # CORS configuration
│   ├── GitHubProperties.java             # GitHub config properties
│   ├── OpenApiConfig.java                # Swagger/OpenAPI setup
│   └── WebClientConfig.java              # WebClient for GitHub API
├── controller/
│   └── ReportController.java             # REST API endpoints
├── exception/
│   ├── GitHubApiException.java           # Custom exception
│   └── GlobalExceptionHandler.java       # Global error handler
├── model/
│   ├── AccessReport.java                 # Report response model
│   ├── CollaboratorInfo.java             # Collaborator DTO
│   ├── RepositoryAccess.java             # Repo access entry
│   ├── RepositoryInfo.java               # Repository DTO
│   └── UserAccess.java                   # User access entry
└── service/
    ├── AccessReportService.java          # Report generation logic
    └── GitHubApiClient.java              # GitHub API client
```
