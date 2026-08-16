# Contract: Hub windpower verify API

## Endpoint

- **Method**: `POST`
- **URL**: configurable, default `https://hub.ag3nts.org/verify`
- **Property**: `app.hub.verify-url`
- **Auth**: `apikey` field = `HUB_API_KEY` environment variable (not logged in full)

## Request envelope

```json
{
  "apikey": "<HUB_API_KEY>",
  "task": "windpower",
  "answer": {
    "action": "<action>",
    "...": "action-specific fields"
  }
}
```

## Actions (from hub help)

### `start`

```json
{ "action": "start" }
```

Starts service window and task state. Begins ~40s configuration interval.

### `get`

```json
{ "action": "get", "param": "weather" | "turbinecheck" | "powerplantcheck" | "documentation" }
```

- `weather`, `turbinecheck`, `powerplantcheck`: queue work; fetch with `getResult`
- `documentation`: returned directly

### `getResult`

```json
{ "action": "getResult" }
```

Returns one completed queued response including `sourceFunction`. Item removed from queue. Random order when multiple pending. Each result consumable once.

### `config`

Single:

```json
{
  "action": "config",
  "startDate": "...",
  "startHour": 0,
  "pitchAngle": 0,
  "turbineMode": "idle" | "production",
  "unlockCode": "..."
}
```

Batch (preferred):

```json
{
  "action": "config",
  "configs": [
    {
      "startDate": "...",
      "startHour": 0,
      "pitchAngle": 0,
      "turbineMode": "idle" | "production",
      "unlockCode": "..."
    }
  ]
}
```

### `unlockCodeGenerator`

```json
{
  "action": "unlockCodeGenerator",
  "startDate": "...",
  "startHour": 0,
  "windMs": 0,
  "pitchAngle": 0
}
```

Async; collect via `getResult`.

### `done`

```json
{ "action": "done" }
```

Validates final configuration. On success, response contains `{FLG:...}`.

## Operational notes (hub)

- Run `start` first
- Run `turbinecheck` (via `get`) before `done`
- Use `getResult` for queued outputs

## Response handling

- Parse JSON when possible; keep raw string for logging and flag extraction
- Flag pattern: `\{FLG:[^}]+\}`
- Treat explicit session-expired / configuration-invalid messages as attempt failures requiring a new `start`
