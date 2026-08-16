<!--
Sync Impact Report
- Version change: (uninitialized template) → 1.0.0
- Modified principles:
  - [PRINCIPLE_1_NAME] → I. Parameter-Free Console Entry
  - [PRINCIPLE_2_NAME] → II. One-Shot Autonomous Run
  - [PRINCIPLE_3_NAME] → III. OpenRouter LLM for Decisions
  - [PRINCIPLE_4_NAME] → IV. Flag-Seeking Action Loop
  - [PRINCIPLE_5_NAME] → V. Minimal Surface Area
- Added sections: Runtime Constraints, Development Workflow
- Removed sections: none (placeholders replaced)
- Follow-up TODOs: none
-->

# s04e02 CTF Agent Constitution

## Core Principles

### I. Parameter-Free Console Entry
The application MUST be a console program that accepts no command-line
parameters. Configuration MUST come from environment variables or fixed
project defaults. Invocation MUST be a single start command with no flags,
positional args, or interactive prompts required to begin the run.

Rationale: The exercise is a fixed CTF scenario; CLI parsing adds surface
area without value.

### II. One-Shot Autonomous Run
Each process start MUST perform one end-to-end attempt: run the action
sequence, evaluate outcomes, and exit. The program MUST NOT wait for
ongoing user input after start. Success is finding and reporting the flag;
failure MUST exit with a clear console message.

Rationale: This is a one-time exercise runner, not a long-lived service
or REPL.

### III. OpenRouter LLM for Decisions
All LLM calls MUST go through the OpenRouter API. API credentials MUST
NOT be hard-coded; they MUST be read from the environment. Non-LLM work
(HTTP, parsing, orchestration) MAY use ordinary code, but decision-making
that needs language-model capability MUST use OpenRouter.

Rationale: The exercise goal depends on LLM reasoning via a single,
explicit provider.

### IV. Flag-Seeking Action Loop
The runtime MUST execute a series of actions, inspect each result, and
continue until the flag is found or the attempt is exhausted. Each step
MUST feed observations back into the next decision. The program MUST
stop when the flag is identified and MUST surface that flag on stdout.

Rationale: Capture-the-flag success is defined by discovering the flag
through iterative act-and-inspect cycles.

### V. Minimal Surface Area
Implement only what the CTF run needs. Prefer a single process and a
small module layout over frameworks, daemons, or unused abstractions.
If a dependency or layer is not required to call OpenRouter, run actions,
or detect the flag, it MUST NOT be added.

Rationale: Bare-minimum scope keeps the exercise focused and maintainable.

## Runtime Constraints

- Delivery form: console application only (no GUI, no HTTP server).
- CLI: zero command-line parameters; no required interactive setup after launch.
- LLM provider: OpenRouter API exclusively for model access.
- Secrets: OpenRouter API key and any related tokens via environment only.
- Lifecycle: one process run equals one autonomous CTF attempt.
- Output: progress and final flag (or failure reason) on the console.

## Development Workflow

- Spec Kit artifacts (spec, plan, tasks) MUST stay aligned with this
  constitution before implementation work proceeds.
- Changes that introduce CLI args, interactive setup, alternate LLM
  providers, or multi-run daemon behavior REQUIRE a constitution amendment
  first.
- Prefer readable orchestration code over premature abstraction.

## Governance

This constitution supersedes conflicting informal practices for this
project. Amendments MUST update `.specify/memory/constitution.md`, bump
`CONSTITUTION_VERSION` using semantic versioning (MAJOR for incompatible
principle removals/redefinitions, MINOR for new or materially expanded
guidance, PATCH for clarifications), set **Last Amended** to the amendment
date (ISO YYYY-MM-DD), and record impact in the Sync Impact Report comment
at the top of this file.

Compliance: plans, specs, and reviews MUST verify that proposed work still
meets Principles I–V and Runtime Constraints. Complexity beyond the
flag-seeking one-shot console flow MUST be justified in the plan or rejected.

**Version**: 1.0.0 | **Ratified**: 2026-08-16 | **Last Amended**: 2026-08-16
