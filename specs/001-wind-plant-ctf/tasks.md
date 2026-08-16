# Tasks: Wind Plant CTF Capture

**Input**: Design documents from `/specs/001-wind-plant-ctf/`

**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included only where design artifacts require verification (schedule rules per US3 independent test / quickstart V3; hub mapping). No full TDD suite.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Single Maven module at repository root: `src/main/java/pl/tomaszko/s04e02/`, `src/main/resources/`, `src/test/java/pl/tomaszko/s04e02/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the Spring Boot console Maven project and empty package layout from plan.md

- [ ] T001 Create Maven package directories `src/main/java/pl/tomaszko/s04e02/{config,hub,tools,schedule,agent,logging,runner}/`, `src/main/resources/prompts/`, `src/test/java/pl/tomaszko/s04e02/{schedule,hub,tools}/`, and `logs/` placeholder guidance via `.gitignore`
- [ ] T002 Create root `pom.xml` with coordinates `pl.tomaszko:s04e02`, Java 23, Spring Boot `4.1.0`, Spring AI BOM `2.0.0`, dependencies `spring-boot-starter`, `spring-ai-starter-model-openai`, Jackson, Logback, and `spring-boot-starter-test` (no StringTemplate/ST4)
- [ ] T003 [P] Add console Spring Boot entrypoint `src/main/java/pl/tomaszko/s04e02/S04e02Application.java` with `@SpringBootApplication` and `SpringApplication.run` (no CLI args)
- [ ] T004 [P] Add `src/main/resources/application.yml` per `contracts/application-config.md` (`web-application-type=none`, OpenRouter mapping, `app.hub.*`, `app.llm.*`, `app.plant.max-session-attempts`, poll intervals, failure message patterns, `app.prompt.system-template-location`, logging file name)
- [ ] T005 [P] Add `src/main/resources/logback-spring.xml` with console + rolling file appenders writing to `logs/s04e02.log` and dedicated loggers `pl.tomaszko.s04e02.tools` / `pl.tomaszko.s04e02.llm`
- [ ] T006 [P] Ensure root `.gitignore` ignores `target/`, `logs/`, IDE files, and local secret overlays (e.g. `application-local.yml`)

**Checkpoint**: `mvn -q -DskipTests validate` (or compile once sources exist) resolves the module skeleton

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configuration, hub HTTP client, logging helpers, and domain shells that every user story needs

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T007 Implement typed config binding `src/main/java/pl/tomaszko/s04e02/config/AppProperties.java` (`@ConfigurationProperties("app")`) covering hub URL/task, LLM model/base URL, max session attempts, getResult poll intervals, session-over/config-rejected message patterns, and system prompt template location
- [ ] T008 [P] Implement `src/main/java/pl/tomaszko/s04e02/config/HubClientConfig.java` providing `RestClient`/`RestClient.Builder` bean pointed at `app.hub.verify-url`
- [ ] T009 [P] Implement `src/main/java/pl/tomaszko/s04e02/config/AiConfig.java` wiring Spring AI OpenAI client to OpenRouter (`OPENROUTER_API_KEY`, base URL, model from `app.llm.*`) and preparing `ChatClient.Builder` for tool registration
- [ ] T010 [P] Create hub DTOs `src/main/java/pl/tomaszko/s04e02/hub/HubWindpowerRequest.java` and `src/main/java/pl/tomaszko/s04e02/hub/HubWindpowerResponse.java` matching `contracts/hub-windpower.md` envelope (`apikey`, `task`, `answer`)
- [ ] T011 Implement `src/main/java/pl/tomaszko/s04e02/hub/HubClient.java` POST helper that injects `HUB_API_KEY` + `task=windpower`, sends action payloads, and never logs the full apikey
- [ ] T012 [P] Implement `src/main/java/pl/tomaszko/s04e02/logging/ToolExecutionLogger.java` to log every plantTool action, redacted parameters, and hub result/error
- [ ] T013 [P] Implement `src/main/java/pl/tomaszko/s04e02/logging/ModelCommunicationLogger.java` (advisor/interceptor) to log system prompt, tool definitions, user messages, and model responses under `pl.tomaszko.s04e02.llm`
- [ ] T014 Create in-memory run state types in `src/main/java/pl/tomaszko/s04e02/agent/` (or dedicated model package) for `CaptureRun` / attempt status enum per `data-model.md` (`RUNNING`, `SUCCESS`, `SETUP_FAILED`, `ATTEMPTS_EXHAUSTED`, flag, failureReason)
- [ ] T015 Implement launch-time setup validation (missing `HUB_API_KEY` / `OPENROUTER_API_KEY` or unreachable hub before first `start`) that exits with `SETUP_FAILED` without consuming an attempt, invokable from the runner

**Checkpoint**: Foundation ready — hub client, config, and logging can be used by US1–US3

---

## Phase 3: User Story 1 - Capture the Flag via Correct Plant Setup (Priority: P1) 🎯 MVP

**Goal**: Parameter-free console run drives a code-sequenced `plantTool` path (with LLM gap-fill assist) until `{FLG:...}` is printed and the process exits

**Independent Test**: With credentials and live/mock hub, start with no args; observe verbose plant I/O and either a printed `{FLG:...}` exit or a clear failure (full retry loop is US2)

### Implementation for User Story 1

- [ ] T016 [P] [US1] Implement Spring AI `src/main/java/pl/tomaszko/s04e02/tools/PlantTool.java` exposing hub actions `start`, `get`, `getResult`, `config` (single + `configs` batch), `unlockCodeGenerator`, `done` per `contracts/plant-tool.md`, delegating to `HubClient` and logging via `ToolExecutionLogger`
- [ ] T017 [P] [US1] Add system prompt resource `src/main/resources/prompts/system-prompt.txt` from `contracts/system-prompt.md` / `contracts/application-config.md` (flow order, storm ≥ strength, unlock-per-item, pitch from hub, hybrid schedule guidance, sequencer ownership; no secrets)
- [ ] T018 [US1] Implement `src/main/java/pl/tomaszko/s04e02/agent/PromptFactory.java` loading classpath text from `app.prompt.system-template-location` and substituting `{{maxAttempts}}`, `{{model}}`, `{{verifyUrl}}` (no ST4 dependency)
- [ ] T019 [US1] Implement `src/main/java/pl/tomaszko/s04e02/agent/CaptureAgent.java` with a **code-driven session sequencer** that calls `PlantTool` in FR-003 order; use ChatClient for unstructured interpretation / FR-018 gap-fill only, not for reordering critical steps; register model logging advisor; stop on `{FLG:...}` or terminal session failure
- [ ] T020 [US1] Implement `src/main/java/pl/tomaszko/s04e02/runner/CaptureApplicationRunner.java` as `ApplicationRunner`: validate setup (T015), invoke `CaptureAgent`, print flag or failure, then `SpringApplication.exit` with exit code `0` on flag success and non-zero on setup failure or exhausted attempts
- [ ] T021 [US1] Add flag extraction helper used by agent/runner (regex `\{FLG:[^}]+\}`) so success path prints the flag exactly once and shuts down immediately (FR-016)

**Checkpoint**: MVP path can start a session via sequencer, call plant tools, and exit on flag (schedule correctness refined in US3; retries in US2)

---

## Phase 4: User Story 2 - Respect Session Time and Retry Until Limit (Priority: P2)

**Goal**: Failed sessions that require a new `start` consume attempts; process retries until flag or configured limit (default 5)

**Independent Test**: Simulate hub “session over” / rejected config; confirm attemptIndex increments, new `start`, and stop with clear message at `app.plant.max-session-attempts`

### Implementation for User Story 2

- [ ] T022 [US2] Implement `src/main/java/pl/tomaszko/s04e02/hub/HubFailureClassifier.java` per `contracts/hub-windpower.md` failure classification; wire into `CaptureAgent` so session-over / rejected-config → fail session attempt + new `start` (FR-013, FR-014); load default message patterns from `AppProperties`
- [ ] T023 [US2] Implement attempt budget loop using `app.plant.max-session-attempts`: increment only when a new `start` is required; do not increment for in-session recoverable waits (async polling / RETRYABLE) (FR-014, FR-015)
- [ ] T024 [US2] On exhausted attempts, set `ATTEMPTS_EXHAUSTED`, print a clear failure reason, and exit immediately with non-zero code without further plant calls (FR-017) in `src/main/java/pl/tomaszko/s04e02/runner/CaptureApplicationRunner.java`
- [ ] T025 [US2] Ensure early flag success exits immediately with code `0` without starting remaining attempts, and that pre-first-start setup failures still never burn the attempt counter (FR-016, FR-019)

**Checkpoint**: US1 capture path + US2 retry/limit behavior both independently verifiable

---

## Phase 5: User Story 3 - Build Safe, Power-Maximizing Schedules from Forecasts (Priority: P3)

**Goal**: Deterministic `TurbineScheduleBuilder` builds chronological necessary-only batches (storm idle ≥ strength, best safe production, post-storm re-idle, per-item unlocks, hub pitches); LLM gap-fills only when parsing fails

**Independent Test**: Feed fixture turbine + forecast data; assert idle for storms (including equality), production at best safe hour, chronological order, no calm fillers, missing pitch → fail attempt

### Tests for User Story 3

- [ ] T026 [P] [US3] Add unit tests for storm/production/order/omit-calm/re-idle rules in `src/test/java/pl/tomaszko/s04e02/schedule/TurbineScheduleBuilderTest.java` (quickstart V3)
- [ ] T027 [P] [US3] Add hub payload mapping / action envelope tests in `src/test/java/pl/tomaszko/s04e02/hub/HubClientTest.java` (or tools package) covering redacted logging expectations where practical (quickstart V4)

### Implementation for User Story 3

- [ ] T028 [P] [US3] Implement schedule domain types (`WeatherPoint`, `WeatherForecastReport`, `TurbineReport`, `ConfigPoint`, `ConfigurationBatch`, unlock request/result) under `src/main/java/pl/tomaszko/s04e02/schedule/` per `data-model.md`
- [ ] T029 [US3] Implement `src/main/java/pl/tomaszko/s04e02/schedule/TurbineScheduleBuilder.java`: windstorm when `windMs >= strength`; best production `max windMs < strength`; hour-aligned times; re-idle after storm hour + 1h when needed; necessary hours only; chronological order (FR-004–FR-010)
- [ ] T030 [US3] Add parsers that extract `strengthMs`, `idlePitchAngle`, and `productionPitchAngle` from hub `documentation` / turbine payloads; if pitches missing, fail the attempt (no invented constants) (FR-021) in `src/main/java/pl/tomaszko/s04e02/schedule/` (or hub package)
- [ ] T031 [US3] Wire hybrid authorship (FR-018): when structured parse succeeds, sequencer uses `TurbineScheduleBuilder` output for unlock+config batch; LLM only gap-fills unparseable portions — update `CaptureAgent` / `PlantTool` integration and keep `system-prompt.txt` aligned
- [ ] T032 [US3] Implement `src/main/java/pl/tomaszko/s04e02/hub/AsyncResultPoller.java` that polls `getResult` using `app.plant.get-result-poll-interval-ms` (default 500) with backoff cap `get-result-poll-max-interval-ms` (default 2000), matches `sourceFunction`, and never re-fetches a consumed report (FR-011, FR-012)
- [ ] T033 [US3] Implement per-item unlock via `unlockCodeGenerator` + `AsyncResultPoller` before batch `config` in `src/main/java/pl/tomaszko/s04e02/schedule/UnlockCodeService.java` (FR-007)

**Checkpoint**: All three user stories independently functional; schedule rules covered by unit tests

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Logging completeness, packaging, and quickstart validation across stories

- [ ] T034 [P] Verify verbose plant request/response logging on every `PlantTool` call (FR-020) and model I/O logging paths in `src/main/java/pl/tomaszko/s04e02/logging/`
- [ ] T035 [P] Confirm `mvn -q -DskipTests package` produces a runnable jar and `mvn spring-boot:run` starts with zero CLI feature flags per `quickstart.md`
- [ ] T036 Run quickstart validation scenarios V2 (missing credentials) and V3 (schedule unit tests); document any live-hub V1 caveats only if needed inside existing `specs/001-wind-plant-ctf/quickstart.md` (no new docs unless gaps found)
- [ ] T037 Final constitution pass (v1.0.1): no CLI args, no HTTP server, secrets only via env, one capture per process with ≤N hub sessions, single-module surface area; fix any drift in `application.yml` / runner exit behavior

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational — MVP capture path
- **User Story 2 (Phase 4)**: Depends on Foundational; builds on US1 agent/runner loop
- **User Story 3 (Phase 5)**: Depends on Foundational; integrates with US1 tool/agent path; schedule builder can be developed in parallel with US2 once models exist
- **Polish (Phase 6)**: Depends on desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: After Phase 2 — no dependency on US2/US3 for a basic sequenced capture attempt
- **User Story 2 (P2)**: After Phase 2 — requires US1 agent/runner to attach attempt accounting + classifier
- **User Story 3 (P3)**: After Phase 2 — `TurbineScheduleBuilder` is independently testable; production wiring depends on US1 `PlantTool`/`CaptureAgent`

### Within Each User Story

- Models/DTOs before services/builders
- Tool/client before agent orchestration
- Core success path before retry/schedule hardening
- Story complete before treating the next priority as done

### Parallel Opportunities

- Phase 1: T003–T006 can run in parallel after T001/T002
- Phase 2: T008–T010, T012–T013 can run in parallel after T007 scaffolding exists
- Phase 3: T016 and T017 in parallel; then T018→T019→T020→T021
- Phase 5: T026–T028 in parallel; T029 depends on T028; T030 parallelizable with T029; T031–T033 after builder + PlantTool

---

## Parallel Example: User Story 1

```text
# After Phase 2 completes, launch in parallel:
Task: "Implement PlantTool in src/main/java/pl/tomaszko/s04e02/tools/PlantTool.java"
Task: "Add system-prompt.txt in src/main/resources/prompts/system-prompt.txt"

# Then sequentially:
Task: "PromptFactory → CaptureAgent (code sequencer) → CaptureApplicationRunner → flag extraction"
```

## Parallel Example: User Story 3

```text
# Tests + domain in parallel:
Task: "TurbineScheduleBuilderTest in src/test/java/pl/tomaszko/s04e02/schedule/TurbineScheduleBuilderTest.java"
Task: "HubClientTest in src/test/java/pl/tomaszko/s04e02/hub/HubClientTest.java"
Task: "Schedule domain types under src/main/java/pl/tomaszko/s04e02/schedule/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run against hub (or mock) for `{FLG:...}` path
5. Proceed to retries and schedule hardening

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → code-sequenced capture MVP
3. US2 → classifier + attempt limit / session restart reliability
4. US3 → deterministic schedule + poller + unlock + pitch rules → highest chance of hub acceptance
5. Polish → logging + quickstart checks

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. After Foundational:
   - Developer A: US1 PlantTool + agent sequencer + runner
   - Developer B: US3 schedule domain + builder + unit tests (merge into agent after US1)
   - Developer C: US2 classifier + attempt accounting once US1 loop exists
3. Integrate and polish

---

## Notes

- [P] tasks = different files, no incomplete-task dependencies
- [Story] labels map to spec.md user stories US1–US3
- JDK path for local runs: `C:\tools\jdk-23.0.2` (see quickstart.md)
- Commit after each task or logical group when asked
- Stop at any checkpoint to validate the story independently
