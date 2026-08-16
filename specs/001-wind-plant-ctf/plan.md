# Implementation Plan: Wind Plant CTF Capture

**Branch**: `001-wind-plant-ctf` | **Date**: 2026-08-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-wind-plant-ctf/spec.md` plus Spring Boot / Spring AI / hub tool constraints from `/speckit-plan` arguments.

## Summary

Build a parameter-free Spring Boot console application that captures the wind-power CTF flag by driving the hub `windpower` task through a Spring AI `plantTool`, using OpenRouter (`inclusionai/ling-3.0-flash` by default). Each process run performs up to N configuration sessions (`start` → gather reports → code-built schedule when parseable → per-item unlock codes → chronological `config` batch → `turbinecheck` → `done`), with the LLM orchestrating tools and gap-filling only when parsing fails. Pitch values MUST come from hub docs/turbine payloads. Logging covers all tool and model I/O to console and file; process exits on `{FLG:...}` or exhausted attempts.

## Technical Context

**Language/Version**: Java 23 (JDK `C:\tools\jdk-23.0.2`)

**Primary Dependencies**: Spring Boot 4.1.0, Spring AI 2.0.0 (`spring-ai-bom`, `spring-ai-starter-model-openai`), Maven (`pl.tomaszko:s04e02`), Jackson, SLF4J/Logback, Spring `RestClient` for hub HTTP

**Storage**: N/A (in-memory attempt state only); log files on disk

**Testing**: JUnit 5 + Spring Boot Test; unit tests for schedule rules and hub payload mapping; optional WireMock/MockWebServer for hub; ChatClient tests with mocked `ChatModel` where practical

**Target Platform**: Windows/Linux console JVM process

**Project Type**: Console / one-shot Spring Boot application (`web-application-type=none`)

**Performance Goals**: Complete a successful hub session within the hub’s 40-second window; overall process finishes within attempt budget (default 5 starts)

**Constraints**: No CLI args; secrets only via env (`HUB_API_KEY`, `OPENROUTER_API_KEY`); OpenRouter only for LLM; verbose tool + model logging; plantTool is sole plant interface; storm when wind ≥ turbine strength; one unlock code per config item; chronological batch; necessary hours only

**Scale/Scope**: Single local operator process; one CTF scenario (`task=windpower`)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Status | Notes |
|------|--------|-------|
| I. Parameter-Free Console Entry | PASS | Env + `application.yml` / prompt template only; no CLI args |
| II. One-Shot Autonomous Run | PASS | `ApplicationRunner` drives capture then `SpringApplication.exit` |
| III. OpenRouter LLM for Decisions | PASS | OpenAI starter pointed at OpenRouter; key from `OPENROUTER_API_KEY` |
| IV. Flag-Seeking Action Loop | PASS | Tool-calling loop until `{FLG:...}` or attempts exhausted |
| V. Minimal Surface Area | PASS | Single Maven module; no HTTP server; only hub + OpenRouter clients |
| Runtime: console only, no HTTP server | PASS | `spring.main.web-application-type=none` |
| Runtime: secrets via environment | PASS | `HUB_API_KEY`, `OPENROUTER_API_KEY` |

**Post-Phase 1 re-check**: PASS — design remains a single console module with ChatClient + PlantTool + config properties; no extra services or UI.

## Project Structure

### Documentation (this feature)

```text
specs/001-wind-plant-ctf/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── plant-tool.md
│   ├── hub-windpower.md
│   ├── application-config.md
│   └── system-prompt.md
└── tasks.md                 # created by /speckit-tasks (not this command)
```

### Source Code (repository root)

```text
pom.xml
src/main/java/pl/tomaszko/s04e02/
├── S04e02Application.java
├── config/
│   ├── AppProperties.java
│   ├── AiConfig.java
│   └── HubClientConfig.java
├── hub/
│   ├── HubClient.java
│   ├── HubWindpowerRequest.java
│   └── HubWindpowerResponse.java
├── tools/
│   └── PlantTool.java
├── schedule/
│   └── TurbineScheduleBuilder.java   # deterministic storm/production rules
├── agent/
│   ├── CaptureAgent.java
│   └── PromptFactory.java
├── logging/
│   ├── ToolExecutionLogger.java
│   └── ModelCommunicationLogger.java
└── runner/
    └── CaptureApplicationRunner.java
src/main/resources/
├── application.yml
├── logback-spring.xml
└── prompts/
    └── system-prompt.st              # StringTemplate / resource template
src/test/java/pl/tomaszko/s04e02/
├── schedule/
├── hub/
└── tools/
logs/                                 # runtime log directory (gitignored)
```

**Structure Decision**: Single Maven module console app under `pl.tomaszko.s04e02`, aligned with constitution minimal surface area.

## Complexity Tracking

> No constitution violations requiring justification. Spring Boot + Spring AI are mandated by the plan input and satisfy OpenRouter + tool-calling needs without additional frameworks.
