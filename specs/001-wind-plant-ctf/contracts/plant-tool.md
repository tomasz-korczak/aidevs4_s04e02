# Contract: plantTool (Spring AI function tool)

## Purpose

LLM-callable tool that executes wind-power hub actions. All plant I/O goes through this tool.

## Tool surface

**Bean**: `PlantTool`  
**Registration**: Spring AI `@Tool` / `@Bean` ToolCallback provider on `ChatClient`

Recommended method shape (implementation may use one dispatcher or multiple `@Tool` methods):

| Tool name | Arguments | Behavior |
|-----------|-----------|----------|
| `plantTool` (or `plantStart`, `plantGet`, …) | `action` + action-specific fields | POST hub verify; return raw JSON/text; log params + result |

### Actions (must match hub)

| action | Required args | Notes |
|--------|---------------|-------|
| `start` | (none) | Opens 40s session; required first |
| `get` | `param`: `weather` \| `turbinecheck` \| `powerplantcheck` \| `documentation` | weather/turbinecheck/powerplantcheck are async → `getResult`; documentation returns inline |
| `getResult` | (none) | Returns one queued item with `sourceFunction`; removes from queue |
| `config` | either single point fields **or** `configs[]` | Prefer batch `configs`; each point needs `unlockCode` |
| `unlockCodeGenerator` | `startDate`, `startHour`, `windMs`, `pitchAngle` | Async → `getResult` |
| `done` | (none) | Validates; success body contains `{FLG:...}` |

### Config point fields

- `startDate`, `startHour`, `pitchAngle`, `turbineMode` (`production`|`idle`), `unlockCode`
- Batch: `{ "configs": [ { ...point }, ... ] }` ordered chronologically

## Logging contract

Every invocation MUST log:
- Tool name / action
- Parameters (secrets redacted: never log full `apikey`)
- Hub response body (or error)

## Error semantics

- Missing env / hub unreachable before any `start`: surface setup failure to runner (no attempt increment)
- Hub “session over” / timeout messaging: treat as failed attempt → new `start`
- Incorrect configuration on check/`done`: failed attempt → new `start`
