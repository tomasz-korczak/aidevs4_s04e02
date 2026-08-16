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
- Classify failures per **Failure classification** below; log the raw body when classifying

## Failure classification (application)

Treat a hub response as **session invalid** (consume session attempt, require new `start`) when any of:

1. **Session over / timeout**: message or body (case-insensitive) contains
   `session is over`, `configuration session is over`, `service window`, or
   equivalent hub wording that the 40s window ended; OR explicit timeout/expired
   error code if present in the JSON `code`/`message` fields.
2. **Rejected configuration**: `turbinecheck` / `done` (or `config`) response
   indicates incorrect/unsafe configuration, insufficient power, or storm-safety
   failure (match on hub `message` text and/or non-success `code` excluding
   “queued / accepted / help” successes).

Treat as **in-session recoverable** (do not increment attempt): empty queue /
“no result yet” on `getResult`, transient parse gaps while session still valid.

Exact hub strings may vary; implement matching as configurable substrings in
`app.plant.session-over-message-patterns` and `app.plant.config-rejected-message-patterns`
with the defaults in `contracts/application-config.md`, and log the raw body when classifying.
