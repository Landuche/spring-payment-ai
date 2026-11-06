# DIO Spring Boot - Final Project 05: Spring AI (budgeting)

## Introduction

This final module applies Spring AI in a budgeting API while preserving the same layered architecture used across the track.

The goal is to integrate AI capabilities without bypassing domain and use case boundaries.

## Code Context

The project processes voice commands to create and query financial transactions.

Primary flow:

1. Client uploads an audio file.
2. Audio is transcribed into text.
3. The model selects an application tool/use case.
4. The use case persists or queries transaction data.
5. The final response is converted to audio.

## Project Structure

The repository holds the API and the mobile app side by side:

```text
Spring-Ai/
├── backend/                      # API (this module)
└── frontend/                     # Expo app (React Native + TypeScript)
```

Backend layout:

```text
backend/
├── compose.yml                   # Postgres + API
├── Dockerfile
├── build.gradle
├── .env.example                  # Database, JWT and AI provider variables
└── src/
    ├── main/
    │   ├── java/dio/budgeting/
    │   │   ├── domain/           # Entities, value objects, business rules and repository contracts
    │   │   ├── application/      # Use cases, shared by REST and AI tool calling
    │   │   │   ├── output/       # Results returned by use cases
    │   │   │   └── port/         # Interfaces implemented by the infrastructure
    │   │   └── infrastructure/
    │   │       ├── http/         # Controllers and error handling
    │   │       │   ├── request/
    │   │       │   ├── response/
    │   │       │   └── exception/
    │   │       ├── persistence/  # JPA adapters
    │   │       │   ├── entity/
    │   │       │   └── repository/
    │   │       ├── security/     # JWT authentication
    │   │       └── config/       # Spring configuration
    │   └── resources/
    │       ├── application.properties
    │       ├── db/migration/     # Flyway migrations
    │       └── prompts/          # Claudinho's system prompt
    └── test/
        └── java/dio/budgeting/   # Unit, controller and context tests
```

Dependencies point inward: `infrastructure` depends on `application`, which depends on `domain`, never the other way around.

Mobile layout:

```text
mobile/
└── src/
    ├── app/                      # Screens (expo-router); (tabs)/ holds the bottom tabs
    ├── components/               # UI components (chat bubble, forms, dialogs, toasts)
    ├── hooks/                    # Reusable hooks (notifications)
    ├── lib/                      # API client, types, formatting, voice and speech helpers
    └── theme/                    # Colors and spacing
```

## Module-Specific Topics

### Speech-to-text

- Uses `TranscriptionModel` for audio transcription.
- Model settings are configured in `application.properties`.

### Tool calling

- `ChatClient` registers use-case tools.
- `@Tool` methods expose business capabilities to the model.

### Text-to-speech

- `TextToSpeechModel` produces MP3 output from final text.
- AI endpoint returns generated audio.

## Spring AI Documentation

- Spring AI Reference: https://docs.spring.io/spring-ai/reference/index.html
- ChatModel API: https://docs.spring.io/spring-ai/reference/api/chatmodel.html
- ChatClient API: https://docs.spring.io/spring-ai/reference/api/chatclient.html
- Tools API: https://docs.spring.io/spring-ai/reference/api/tools.html
- Audio Transcriptions API: https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html
- Audio Speech API: https://docs.spring.io/spring-ai/reference/api/audio/speech.html

## Shared Architecture References

Common architecture concepts are documented in the root README:

- [DDD layers](../README.md#ddd-layered-architecture)
- [Class vs record](../README.md#java-class-vs-java-record-in-domain-modeling)
- [Strong typed identifiers](../README.md#strong-typed-identifiers)
- [Repository pattern](../README.md#repository-pattern)
- [Use cases and Clean Architecture](../README.md#use-cases-and-clean-architecture)
- [Docker Compose support](../README.md#docker-compose-support-in-development)

## How to Run

### Application

Copy `.env.example` to `.env` and fill it in. The AI provider takes only two variables and works with any
OpenAI-compatible API (OpenAI, Groq, ...):

```properties
AI_BASE_URL=https://api.groq.com/openai   # or https://api.openai.com
AI_API_KEY=your_api_key_here
```

Then start the database and the API:

```bash
docker compose up -d --build
```

### Tests

```bash
./gradlew test          # PowerShell/CMD: .\gradlew.bat test
```

Requirements: a running Docker daemon. No installed database, `.env` or environment variables are needed: the
context test (`BudgetingApplicationTests`) starts a throwaway Postgres with Testcontainers and runs the Flyway
migrations against it. If no JDK 25 is installed, Gradle downloads it on the first run.

No test calls the real AI provider, so running the suite costs no tokens.
The HTML report is generated in `build/reports/tests/test/index.html`.

## Notes

- Educational final project focused on AI plus architectural discipline.
