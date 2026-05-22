# PRism

![CI](https://github.com/Rishab-Kumar-R/PRism/actions/workflows/ci.yml/badge.svg)
![Java](https://img.shields.io/badge/Java-21-orange)
![Quarkus](https://img.shields.io/badge/Quarkus-3.35.3-blue)
![License](https://img.shields.io/badge/license-MIT-green)

A GitHub App that automatically reviews pull requests using AI. Detects bugs, security issues, and code quality problems - then posts a structured review comment directly on the PR.

[//]: # "## Demo"
[//]: #
[//]: # "> Bot commenting on a real PR after opening it:"
[//]: #
[//]: # "![Demo](docs/demo.gif)"
[//]: #
[//]: # "> PR labels applied automatically based on review severity:"
[//]: #
[//]: # "- `ai: approved` - score ≥ 7, no critical issues"
[//]: # "- `ai: needs-work` - bugs, security issues, or significant problems found"
[//]: # "- `ai: large-pr` - diff too large, partially reviewed"

## Features

- **Automatic PR review** - triggers on PR open and every new commit push
- **`/review` command** - comment `/review` on any PR to trigger a manual re-review
- **Structured AI response** - score (1-10), severity, bug/security/performance counts, highlights, recommendation
- **Smart deduplication** - skips review if the same commit SHA was already reviewed
- **Diff size limit** - large PRs are truncated gracefully with a notice
- **PR labels** - auto-applies labels based on review outcome
- **Review history API** - paginated REST endpoints to query past reviews and stats
- **Error handling** - posts a fallback comment if the AI is unavailable

## How It Works

```
PR opened / new commit pushed / /review comment
              ↓
GitHub sends webhook to the app
              ↓
Fetch PR diff from GitHub
              ↓
Send diff to AI (via OpenRouter)
              ↓
AI returns structured review (score, severity, issues, full markdown)
              ↓
Post review comment on PR + apply label
              ↓
Save review record to database
```

## Tech Stack

| Layer              | Technology                                                                                             |
| ------------------ | ------------------------------------------------------------------------------------------------------ |
| Framework          | [Quarkus 3.35](https://quarkus.io/)                                                                    |
| GitHub Integration | [Quarkus GitHub App](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/index.html) |
| AI                 | [LangChain4j + OpenRouter](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)             |
| Persistence        | [Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)                          |
| Database           | H2 (dev) / PostgreSQL (prod)                                                                           |
| Testing            | JUnit 5 + RestAssured + Mockito                                                                        |

## Project Structure

```
src/main/java/dev/rishabkumar/prism/
├── ai/
│   ├── CodeReview.java              # Structured AI response object
│   ├── CodeReviewAI.java            # LangChain4j AI service interface
│   └── AIReviewService.java         # AI wrapper service
├── github/
│   ├── GitHubService.java           # Fetch diff, post comments, apply labels
│   ├── PullRequestHandler.java      # Webhook event listener (open + sync)
│   └── IssueCommentHandler.java     # /review comment trigger
├── review/
│   ├── ReviewRecord.java            # JPA entity for review history
│   ├── ReviewRepository.java        # Database queries
│   ├── ReviewResource.java          # REST API endpoints
│   ├── ReviewService.java           # Core review business logic
│   └── ReviewStats.java             # Stats response DTO
└── security/
    └── ApiKeyFilter.java            # API key auth for REST endpoints
```

## REST API

All endpoints require the `API-Key` header.

| Method | Endpoint                   | Description                                                       |
| ------ | -------------------------- | ----------------------------------------------------------------- |
| GET    | `/reviews`                 | All reviews (paginated)                                           |
| GET    | `/reviews/{id}`            | Single review with full breakdown                                 |
| GET    | `/reviews/repo/{repoName}` | Reviews for a specific repo                                       |
| GET    | `/reviews/pr/{prNumber}`   | Reviews for a specific PR                                         |
| GET    | `/reviews/stats`           | Aggregate stats - approval rate, average score, most common issue |

### Pagination

All list endpoints support `?page=0&size=20` query params.

### Stats response example

```json
{
  "totalReviews": 42,
  "approved": 30,
  "needsWork": 12,
  "approvalRate": "71%",
  "averageScore": 7.2,
  "totalBugs": 18,
  "totalSecurityIssues": 5,
  "totalPerformanceIssues": 9,
  "mostCommonIssue": "bugs",
  "mostReviewedRepo": "rishabkumar/my-project"
}
```

## Prerequisites

- Java 21
- A [GitHub App](https://docs.github.com/en/apps/creating-github-apps/registering-a-github-app/registering-a-github-app) with Pull Request and Issue Comment read/write permissions
- An [OpenRouter API key](https://openrouter.ai) (free tier available)

## Setup

**1. Clone the repo**

```bash
git clone https://github.com/Rishab-Kumar-R/PRism.git
cd PRism
```

**2. Create your `.env` file**

```bash
cp .env.example .env
```

Fill in your values:

```properties
OPENROUTER_API_KEY=your_openrouter_api_key
AI_MODEL_ID=openai/gpt-oss-120b:free
GITHUB_APP_ID=your_github_app_id
GITHUB_APP_PRIVATE_KEY=-----BEGIN RSA PRIVATE KEY-----\nyour_key_here\n-----END RSA PRIVATE KEY-----
API_KEY=your_strong_secret_key
```

**3. Run locally**

```bash
./gradlew quarkusDev
```

## Running Tests

```bash
./gradlew test
```

58 tests covering all layers - REST endpoints, repository queries, AI service, GitHub service, and the full review flow.

## Building

```bash
./gradlew build
```

## GitHub App Permissions Required

| Permission    | Access       |
| ------------- | ------------ |
| Pull requests | Read & Write |
| Issues        | Read & Write |
| Contents      | Read         |

## Contributing

1. Fork the repo
2. Create a feature branch (`git checkout -b feat/my-feature`)
3. Commit your changes
4. Push and open a PR - the bot will review it automatically

## License

MIT
# trigger Fri May 22 21:52:42 IST 2026
