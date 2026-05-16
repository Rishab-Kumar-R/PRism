# AI Code Reviewer

A GitHub App that automatically reviews pull requests using Google Gemini AI. Built with Quarkus, designed to run as a native binary on AWS Lambda with near-zero cold start time.

## How It Works

1. A pull request is opened on a repository where the app is installed
2. GitHub sends a webhook event to the app
3. The app fetches the PR diff from GitHub
4. The diff is sent to Gemini AI for review
5. The AI review is posted as a comment on the PR
6. The review is saved to the database for history

## Tech Stack

- **[Quarkus](https://quarkus.io/)** — Kubernetes-native Java framework
- **[Quarkus GitHub App](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/index.html)** — GitHub webhook handling and GitHub API
- **[LangChain4j + Gemini](https://docs.quarkiverse.io/quarkus-langchain4j/dev/index.html)** — AI integration
- **[Hibernate ORM with Panache](https://quarkus.io/guides/hibernate-orm-panache)** — persistence
- **[AWS Lambda HTTP](https://quarkus.io/guides/aws-lambda-http)** — serverless deployment

## Project Structure

```
src/main/java/dev/rishabkumar/
├── review/
│   ├── ReviewRecord.java        # JPA entity for review history
│   └── ReviewRepository.java    # Database operations
├── ai/
│   ├── CodeReviewAI.java        # LangChain4j AI service interface
│   └── GeminiReviewService.java # Gemini wrapper service
└── github/
    ├── GitHubService.java        # Fetch diff, post comments
    └── PullRequestHandler.java   # Webhook event listener
```

## Prerequisites

- Java 21
- A [GitHub App](https://docs.github.com/en/apps/creating-github-apps/registering-a-github-app/registering-a-github-app) with Pull Request read/write permissions
- A [Gemini API key](https://aistudio.google.com/app/apikey)
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/) for local development

## Configuration

Copy `.env.example` to `.env` and fill in your values:

```properties
GEMINI_API_KEY=your_gemini_api_key
GITHUB_APP_ID=your_github_app_id
GITHUB_APP_PRIVATE_KEY=your_pem_private_key_content
```

## Running Locally

```bash
./gradlew quarkusDev
```

## Building Native Binary

```bash
./gradlew build -Dquarkus.native.enabled=true
```

## Deploying to AWS Lambda

```bash
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
sam deploy
```
