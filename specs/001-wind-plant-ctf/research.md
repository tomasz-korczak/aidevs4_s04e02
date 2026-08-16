# Research: Wind Plant CTF Capture

## Decision: Spring Boot 4.1.0 + Spring AI 2.0.0 + Java 23

- **Decision**: Use Spring Boot `4.1.0` (latest stable) with Spring AI `2.0.0` (latest stable GA) on JDK 23 (`C:\tools\jdk-23.0.2`).
- **Rationale**: Spring AI 2.0.0 requires Spring Boot 4.0.x / 4.1.x. Boot 4.1.0 supports Java 17–26, so JDK 23 is valid. Matches “latest stable Boot + latest Spring AI compatible with that Boot.”
- **Alternatives considered**:
  - Spring Boot 3.5.x + Spring AI 1.0.0 — older stack; forgoes Spring AI 2.0 GA features.
  - Spring Boot 4.0.7 + Spring AI 2.0.0 — valid but not the newest Boot line.

## Decision: OpenRouter via Spring AI OpenAI-compatible starter

- **Decision**: Depend on `spring-ai-starter-model-openai` and point it at OpenRouter:
  - `spring.ai.openai.api-key=${OPENROUTER_API_KEY}`
  - `spring.ai.openai.base-url=https://openrouter.ai/api` (or `/api/v1` if required by the OpenAI SDK path in Spring AI 2.0)
  - `spring.ai.openai.chat.options.model=inclusionai/ling-3.0-flash` (overridable via `app.llm.model`)
- **Rationale**: OpenRouter exposes an OpenAI-compatible HTTP API; Spring AI has no separate OpenRouter starter. This is the documented integration pattern.
- **Alternatives considered**: Raw RestClient to OpenRouter — loses ChatClient, advisors, and `@Tool` integration.

## Decision: Console app with `web-application-type=none`

- **Decision**: Use Spring Boot without serving HTTP (`spring.main.web-application-type=none`). Prefer `spring-boot-starter` + `RestClient`/`RestClient.Builder` for hub calls; avoid embedded Tomcat as a server. Entry via `ApplicationRunner` / `CommandLineRunner`, then `SpringApplication.exit`.
- **Rationale**: Constitution requires console-only, no HTTP server. Parameter-free launch via Maven/`java -jar` with env vars only.
- **Alternatives considered**: `spring-boot-starter-web` with Tomcat — violates constitution unless web type is disabled; still heavier.

## Decision: Agentic ChatClient + hybrid schedule authorship (FR-018)

- **Decision**: Expose hub operations as Spring AI `PlantTool`. Drive the run with `ChatClient` tool calling. **`TurbineScheduleBuilder` (code) builds the configuration batch whenever turbine/forecast data parse cleanly.** The LLM orchestrates `plantTool` and may gap-fill only unparseable portions. Encode CTF rules in the system prompt as a safety net. Pitch angles MUST come from hub documentation/turbine payloads; missing pitch fails the attempt (FR-021).
- **Rationale**: User requires Spring AI tool calling; Spec FR-018 (clarified hybrid) keeps code as primary schedule author when data is structured.
- **Alternatives considered**:
  - Pure deterministic orchestrator with no LLM — conflicts with constitution OpenRouter principle and user Spring AI requirement.
  - LLM invents all pitches / always authors batch — conflicts with FR-018/FR-021.

## Decision: Hub verify API as sole plant transport

- **Decision**: `PlantTool` POSTs to configurable `app.hub.verify-url` (default `https://hub.ag3nts.org/verify`) with body:
  `{ "apikey": "${HUB_API_KEY}", "task": "windpower", "answer": { "action": "...", ... } }`.
- **Rationale**: Provided exercise contract. Actions: `start`, `get` (+ `param`), `getResult`, `config` (single or `configs` batch), `unlockCodeGenerator`, `done`. Async: `get` for weather/turbinecheck/powerplantcheck and `unlockCodeGenerator` require later `getResult`. Documentation `get` returns inline.
- **Alternatives considered**: Direct plant endpoints — none provided.

## Decision: Attempt limit vs hub 40s session

- **Decision**: Application-level `app.plant.max-session-attempts` (default 5) counts each time a new `start` is required. Hub enforces 40s per session; on session-over / rejected config, restart from `start` until flag or attempt budget exhausted.
- **Rationale**: Matches spec FR-013–FR-015 and clarifications.
- **Alternatives considered**: Counting every tool call — rejected by spec.

## Decision: Logging (console + file + tool + model I/O)

- **Decision**: Logback console + rolling file appenders. Log every `plantTool` invocation (parameters + result). Log model traffic (system prompt, tool definitions, user messages, model response) via ChatClient advisors / logging interceptor at DEBUG/INFO under dedicated loggers (e.g. `pl.tomaszko.s04e02.llm`, `pl.tomaszko.s04e02.tools`).
- **Rationale**: Explicit user requirement and FR-020 verbose plant I/O.
- **Alternatives considered**: Console-only — insufficient for post-mortem of failed attempts.

## Decision: Maven coordinates and configuration surface

- **Decision**: `groupId=pl.tomaszko`, `artifactId=s04e02`. Parameterize: `HUB_API_KEY`, `OPENROUTER_API_KEY`, max attempts, HTTP URLs (hub + OpenRouter base), system prompt template, LLM model name.
- **Rationale**: User-provided build and configuration rules; constitution forbids CLI args for these.
- **Alternatives considered**: Hard-coded secrets/URLs — rejected.

## Decision: Align “turbine checked” with hub `get` + `turbinecheck`

- **Decision**: Spec’s “execute turbine checked” maps to hub `get` with `param=turbinecheck` (and `getResult` as needed) before `done`, per hub notes.
- **Rationale**: Hub help has no separate `checked` action.
- **Alternatives considered**: Inventing a non-existent action — would fail at runtime.
