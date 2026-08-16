# Quickstart: Wind Plant CTF Capture

## Prerequisites

- JDK 23 at `C:\tools\jdk-23.0.2` (`JAVA_HOME` set)
- Maven 3.9+
- Environment variables:
  - `HUB_API_KEY` — hub verify apikey
  - `OPENROUTER_API_KEY` — OpenRouter key
- Network access to `https://hub.ag3nts.org/verify` and OpenRouter

See [contracts/application-config.md](./contracts/application-config.md) and [contracts/hub-windpower.md](./contracts/hub-windpower.md).

## Setup

```powershell
$env:JAVA_HOME = "C:\tools\jdk-23.0.2"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:HUB_API_KEY = "<your-hub-key>"
$env:OPENROUTER_API_KEY = "<your-openrouter-key>"
```

From repository root (after implementation exists):

```powershell
mvn -q -DskipTests package
```

Optional overrides (still no CLI feature flags—use Spring config only):

```powershell
$env:APP_PLANT_MAX_SESSION_ATTEMPTS = "5"
# or --spring.application.json / application-local.yml if added later without becoming required CLI args
```

## Run

```powershell
mvn spring-boot:run
# or
java -jar target/s04e02-*.jar
```

Expected console + `logs/s04e02.log` behavior:

1. Setup fails fast if keys missing (no attempt burn)
2. Verbose plantTool request/response lines
3. Model I/O logs (system prompt, tools, user, response)
4. One of:
   - Success: printed `{FLG:...}` then process exit
   - Failure: clear exhausted-attempts (or setup) message then exit

## Validation scenarios

### V1 — Happy path (live hub)

1. Set both API keys
2. Run the app with no arguments
3. **Expect**: within attempt budget, `done` yields `{FLG:...}`; app exits

### V2 — Missing credentials

1. Unset `HUB_API_KEY` or `OPENROUTER_API_KEY`
2. Run
3. **Expect**: immediate setup error; attempt counter unused

### V3 — Schedule rules (unit)

1. Run unit tests for `TurbineScheduleBuilder` with fixture forecast + turbine strength
2. **Expect**: idle for `wind >= strength`; production at best `wind < strength`; chronological order; no calm filler hours; storm at H implies re-idle consideration at/after H+1

### V4 — plantTool contract (unit/integration)

1. Mock hub HTTP
2. Invoke tool actions `start`, `get`, `getResult`, `unlockCodeGenerator`, `config` batch, `done`
3. **Expect**: payloads match [hub-windpower.md](./contracts/hub-windpower.md); logs contain params (redacted apikey) and results

### V5 — Attempt accounting

1. Simulate hub “session over” after `start`
2. **Expect**: new `start`, attemptIndex increments, stops at `app.plant.max-session-attempts`

## References

- Domain entities: [data-model.md](./data-model.md)
- Tool contract: [contracts/plant-tool.md](./contracts/plant-tool.md)
- Research decisions: [research.md](./research.md)
- Spec: [spec.md](./spec.md)
