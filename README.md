# GitHub Repository Access Mapper

A Spring Boot backend service that connects to the GitHub API, retrieves all repositories within a given organization, determines which users have access to each repository (including their permission level/role), and exposes a REST API endpoint returning a structured JSON access report.

---

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Authentication Setup](#authentication-setup)
- [Configuration](#configuration)
- [How to Run](#how-to-run)
- [API Endpoint](#api-endpoint)
- [Example Response](#example-response)
- [Architecture & Design Decisions](#architecture--design-decisions)
- [Assumptions](#assumptions)

---

## Features

- **GitHub Authentication** — Authenticates using a Personal Access Token (PAT) via the `Authorization: Bearer` header.
- **Organization Repository Discovery** — Retrieves all repositories (public, private, internal) for a given GitHub organization.
- **Collaborator Permission Mapping** — For each repository, fetches all collaborators and their permission levels (admin, maintain, write, triage, read).
- **User-Centric Aggregation** — Aggregates data into a report that maps each user to the repositories they can access.
- **Concurrent API Calls** — Uses `CompletableFuture` with a bounded thread pool to fetch collaborator data for multiple repositories in parallel, supporting 100+ repos and 1000+ users efficiently.
- **Pagination Support** — Follows GitHub's `Link` header pagination to retrieve all pages of results.
- **Rate-Limit Awareness** — Monitors `X-RateLimit-Remaining` headers and stops gracefully when the limit is exhausted.
- **Robust Error Handling** — Structured JSON error responses for authentication failures, missing resources, rate limits, and server errors.
- **OpenAPI / Swagger Documentation** — Auto-generated API docs available at `/swagger-ui.html`.

---

## Prerequisites

- **Java 17** or later
- **Gradle 8+** (included via the Gradle Wrapper — no separate install needed)
- A **GitHub Personal Access Token** with appropriate scopes

---

## Authentication Setup

1. Go to [GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)](https://github.com/settings/tokens).
2. Click **Generate new token (classic)**.
3. Select the following scopes:
   - `read:org` — to list organization repositories
   - `repo` — to access private repository collaborators (use `public_repo` if you only need public repos)
4. Copy the generated token.
5. Set it as an environment variable:

```bash
export GITHUB_TOKEN=ghp_your_personal_access_token_here
```

> **Security Note:** Never commit your token to source control. Use environment variables or a `.env` file (already in `.gitignore`).

---

## Configuration

The application is configured via environment variables (or `application.properties`):

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `GITHUB_TOKEN` | **Yes** | — | GitHub Personal Access Token for API authentication |
| `GITHUB_ORG` | No | — | Default organization to scan. Can be overridden via the `?org=` query parameter |

Additional properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `github.api-base-url` | `https://api.github.com` | GitHub API base URL (useful for GitHub Enterprise) |
| `github.page-size` | `100` | Items per page for API pagination (max 100) |
| `github.max-concurrency` | `10` | Max parallel threads for fetching collaborators |

---

## How to Run

### Option 1: Using environment variables

```bash
cd github_access_report_backend

# Set required environment variables
export GITHUB_TOKEN=ghp_your_token_here
export GITHUB_ORG=your-org-name    # optional

# Run the application
./gradlew bootRun
```

### Option 2: Using a .env file

Copy the example and fill in your values:

```bash
cp .env.example .env
# Edit .env with your actual token and org name
```

Then run with env vars loaded:

```bash
cd github_access_report_backend
export $(cat .env | xargs) && ./gradlew bootRun
```

The application starts on **port 8080** by default (or the port configured by the environment).

---

## API Endpoint

### `GET /api/report`

Generates and returns the access report for a GitHub organization.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `org` | No | Organization name. Overrides the default `GITHUB_ORG` environment variable. If neither is set, returns a `400 Bad Request`. |

**Example Requests:**

```bash
# Using the default org (from GITHUB_ORG env var)
curl http://localhost:8080/api/report

# Specifying an organization explicitly
curl http://localhost:8080/api/report?org=spring-projects

# Pretty-printed output
curl -s http://localhost:8080/api/report?org=myorg | python3 -m json.tool
```

**Other Useful Endpoints:**

| Endpoint | Description |
|----------|-------------|
| `GET /` | Welcome message |
| `GET /health` | Health check |
| `GET /docs` | Redirects to Swagger UI |
| `GET /swagger-ui.html` | Interactive API documentation |
| `GET /api-docs` | Raw OpenAPI JSON spec |

---

## Example Response

```json
{
  "organization": "myorg",
  "generatedAt": "2025-01-15T10:30:00.123Z",
  "totalRepositories": 42,
  "totalUsers": 15,
  "users": [
    {
      "username": "alice",
      "repositories": [
        {
          "repository": "myorg/backend-api",
          "role": "admin"
        },
        {
          "repository": "myorg/frontend-app",
          "role": "write"
        }
      ]
    },
    {
      "username": "bob",
      "repositories": [
        {
          "repository": "myorg/backend-api",
          "role": "read"
        }
      ]
    }
  ]
}
```

**Error Response Example (400):**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Organization name is required. Provide it via the 'org' query parameter or set the GITHUB_ORG environment variable.",
  "timestamp": "2025-01-15T10:30:00.456Z"
}
```

---

## Architecture & Design Decisions

### Project Structure

```
src/main/java/com/example/demo/
├── githubaccessreportbackendApplication.java  # Main entry point
├── HelloController.java                        # Basic health/info endpoints
├── client/
│   └── GitHubApiClient.java                   # Low-level GitHub REST API client
├── config/
│   ├── GitHubProperties.java                  # Configuration properties
│   ├── OpenApiConfig.java                     # Swagger/OpenAPI metadata
│   └── RestTemplateConfig.java                # HTTP client with auth headers
├── controller/
│   └── ReportController.java                  # REST endpoint for access report
├── dto/
│   ├── AccessReport.java                      # Top-level report response
│   ├── UserAccess.java                        # Per-user access summary
│   ├── RepositoryPermission.java              # Per-repo permission entry
│   └── ErrorResponse.java                     # Structured error response
├── exception/
│   ├── GitHubApiException.java                # Custom exception for API errors
│   └── GlobalExceptionHandler.java            # Global exception -> JSON mapping
└── service/
    └── GitHubAccessReportService.java         # Aggregation & concurrency logic
```

### Key Design Decisions

1. **Concurrent Collaborator Fetching**: The service uses `CompletableFuture` with a bounded `ExecutorService` to fetch collaborators for multiple repositories in parallel. This avoids sequential API calls and dramatically reduces response time for organizations with 100+ repositories.

2. **Bounded Thread Pool**: The concurrency level is configurable (`github.max-concurrency`, default 10) to balance throughput against GitHub's rate limits (5,000 requests/hour for authenticated users).

3. **Pagination via Link Headers**: All paginated endpoints use the standard GitHub `Link` header to navigate pages, ensuring complete data retrieval regardless of the number of items.

4. **Rate-Limit Monitoring**: The client inspects `X-RateLimit-Remaining` headers and throws a clear error before exhausting the limit, preventing silent failures.

5. **Graceful Per-Repository Error Handling**: If fetching collaborators fails for a single repository (e.g., insufficient permissions on a specific repo), the error is logged and that repo is skipped — the rest of the report still generates successfully.

6. **No Database Required**: The service fetches data live from the GitHub API on each request. JPA/H2 dependencies were removed since no persistence is needed.

7. **User-Centric Aggregation**: The raw GitHub API is repository-centric (repo → collaborators). The service inverts this into a user-centric view (user → repositories + permissions) as required by the assignment.

---

## Assumptions

1. **Authentication**: The service uses a GitHub Personal Access Token (classic) for authentication. Fine-grained tokens or OAuth apps are not supported in this version.

2. **Collaborators Endpoint**: The `/repos/{owner}/{repo}/collaborators` endpoint is used, which requires push access to the repository. For organizations where the token owner doesn't have push access to all repos, some repositories may be skipped (with a warning logged).

3. **Real-Time Data**: Reports are generated live on each API call — there is no caching or persistence layer. For very large organizations, response times depend on GitHub API latency and rate limits.

4. **Rate Limits**: GitHub allows 5,000 authenticated API requests per hour. For an organization with N repositories, the service makes approximately `1 + N` API calls (1 for listing repos + N for collaborators, each potentially with pagination). Organizations with more than ~4,900 repositories may hit rate limits in a single report generation.

5. **Permission Levels**: The service reports the highest permission level per user per repository. GitHub's permission hierarchy (highest to lowest): `admin` > `maintain` > `write` > `triage` > `read`.

6. **Organization Visibility**: The token must belong to an account that is a member of the organization (or the organization's repos must be public) for the API to return results.
