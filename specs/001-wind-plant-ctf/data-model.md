# Data Model: Wind Plant CTF Capture

## Entities

### CaptureRun

In-process lifecycle for one JVM execution.

| Field | Type | Rules |
|-------|------|-------|
| attemptIndex | int | 1-based; increments when a new hub `start` is required |
| maxAttempts | int | From `app.plant.max-session-attempts` (default 5) |
| status | enum | `RUNNING`, `SUCCESS`, `SETUP_FAILED`, `ATTEMPTS_EXHAUSTED` |
| flag | string? | `{FLG:...}` when success |
| failureReason | string? | Set on setup failure or exhaustion |

**Transitions**: `RUNNING` → `SUCCESS` | `SETUP_FAILED` | `ATTEMPTS_EXHAUSTED`

### ConfigurationSession

One hub service window after `start`.

| Field | Type | Rules |
|-------|------|-------|
| startedAt | instant | Local clock for diagnostics (hub enforces 40s) |
| active | boolean | False when hub reports session over / after `done` |
| attemptIndex | int | Links to CaptureRun |

**Transitions**: created on successful `start` → expired/rejected → discarded; success ends run

### WeatherForecastReport

| Field | Type | Rules |
|-------|------|-------|
| items | list of WeatherPoint | From async `get(weather)` + `getResult` |
| consumed | boolean | Each queued report retrieved once |

### WeatherPoint

| Field | Type | Rules |
|-------|------|-------|
| occurrenceDate | date | Hour-aligned schedule uses date + hour |
| occurrenceHour | int | 0–23; minutes/seconds always 0 in configs |
| windMs | number | Wind speed m/s |
| (other) | any | Pass-through fields from hub if present |

**Validation**: Occurrence time only (no end time). Windstorm iff `windMs >= turbineStrength`.

### TurbineReport

| Field | Type | Rules |
|-------|------|-------|
| strengthMs | number | Max safe wind; storm threshold |
| idlePitchAngle | number | Required from hub docs/turbine payloads (FR-021); missing → fail attempt |
| productionPitchAngle | number | Required from hub docs/turbine payloads (FR-021); missing → fail attempt |
| raw | object/string | Original hub payload for LLM/debug |

### UnlockCodeRequest / UnlockCode

| Field | Type | Rules |
|-------|------|-------|
| startDate | string/date | Required by hub `unlockCodeGenerator` |
| startHour | int | Required |
| windMs | number | Required |
| pitchAngle | number | Required |
| unlockCode | string | Distinct per config batch item; never reuse across items |

Async: generate via `unlockCodeGenerator`, collect via `getResult` (`sourceFunction` identifies origin).

### ConfigPoint

| Field | Type | Rules |
|-------|------|-------|
| startDate | string | Required |
| startHour | int | Minutes/seconds conceptually 0 |
| pitchAngle | number | Idle pitch for storms; max-production pitch for best safe hour |
| turbineMode | enum | `idle` \| `production` |
| unlockCode | string | Per-item unique |
| windMs | number? | Used when generating unlock code |

### ConfigurationBatch

| Field | Type | Rules |
|-------|------|-------|
| configs | list of ConfigPoint | Necessary hours only: storm idles (incl. post-reset re-idle) + best safe production through first power |
| order | chronological | Ascending by date then hour |

**Batch composition rules**:
1. Storm hours: `windMs >= strength` → `turbineMode=idle`
2. After storm at hour H, defaults reset at H+1; if later storm at/after H+1, idle again
3. Best production: max `windMs` among hours with `windMs < strength`
4. Omit calm/unused hours not required for (1)–(3)
5. Prefer single hub `config` call with `configs` array

### HubQueuedResult

| Field | Type | Rules |
|-------|------|-------|
| sourceFunction | string | e.g. weather, turbinecheck, unlockCodeGenerator |
| payload | object/string | One item removed from hub queue per `getResult` |

### HubFailureClass

| Value | Meaning |
|-------|---------|
| SESSION_OVER | New start required; consume session attempt |
| CONFIG_REJECTED | New start required; consume session attempt |
| RETRYABLE | Keep session; e.g. getResult empty |
| SUCCESS | Continue / flag path |

### Flag

| Field | Type | Rules |
|-------|------|-------|
| value | string | Matches `\{FLG:[^}]+\}` extracted from `done` response |

## Relationships

```text
CaptureRun 1──* ConfigurationSession
ConfigurationSession ── obtains ── WeatherForecastReport, TurbineReport
TurbineReport + WeatherForecastReport ── build ── ConfigurationBatch
ConfigurationBatch 1──* ConfigPoint 1──1 UnlockCode
ConfigurationSession ── done ── Flag?
HubFailureClassifier ── classifies ── HubFailureClass
```

## Validation Summary (from spec)

- Windstorm: `windMs >= turbine.strengthMs`
- Safe production: `windMs < turbine.strengthMs`
- Pitch: idle and production angles required from hub docs/turbine payloads; missing → fail attempt (FR-021)
- Times: hour precision only
- Unlock: one code per ConfigPoint, before `config` send
- Attempts: increment only when new `start` required
- Async poll: default 500ms interval, backoff cap 2000ms; stop on result, session-over, or 40s budget (FR-011)
- Schedule: code builds when structured parse succeeds; LLM gap-fill only otherwise (FR-018)
- Setup missing keys/hub unreachable before first start: fail run without consuming attempt
- Glossary: Capture run = one JVM process; Session attempt = one hub start→done/expiry
