# Feature Specification: Wind Plant CTF Capture

**Feature Branch**: `001-wind-plant-ctf`

**Created**: 2026-08-16

**Status**: Draft

**Input**: User description: "I'm building application that's solely purpose is to win capture the flag game. To capture the flag application following must properly set up wind power plant and generate power. Setting up power plant consist of properly configuring wind turbin blades so turbin produces power when wind is strong enough. At the same time turbin must be configured to idle when there is a windstorm expected. Windstorm is when wind speed is higher than wind turbin strength. To configure power plant the llm tool: plantTool is available. [full configuration flow, timing limits, attempt limits, async report tips, and shutdown behavior as provided]"

## Clarifications

### Session 2026-08-16

- Q: Should the application mainly use the language model to invent the turbine configuration values, or mainly use fixed logic that only calls the model when interpreting messy report text? → A: Fixed rules drive schedule/idle/production; model only helps interpret reports when needed
- Q: Which failures should count against the attempt limit? → A: Only failures that require resending start (including session timeout and incorrect configuration); other in-session recoverable issues do not consume an attempt
- Q: If the plant service or required credentials are missing when the application launches, what should it do? → A: Exit immediately with a clear setup/configuration error; do not consume an attempt
- Q: How much should the application print to the console while a capture attempt is running? → A: Verbose: print essentially every plant request/response
- Q: When forecast wind speed is exactly equal to the turbine’s strength, should that hour be treated as a windstorm (idle) or as safe production wind? → A: Windstorm when wind ≥ turbine strength; equal must idle
- Q: When during an attempt should the application obtain the unlock/signing code used on configuration entries? → A: Before sending the configuration command; each config batch item requires its own separate unlock code
- Q: After a windstorm, when does the roughly one-hour turbine reset-to-default clock start for deciding whether idle must be configured again? → A: One hour after the windstorm hour (hour-aligned occurrence time + 1h); forecast items have occurrence time only, no end time
- Q: Which hours must appear as configuration batch items? → A: Only necessary hours: each relevant storm (including post-reset re-idle) plus the best safe production hour through first power
- Q: In what order should configuration batch items be sent to the plant? → A: Chronological by configuration hour (earliest first)
- Q: How must idle and max-production pitch angles be obtained? → A: MUST read pitch (or equivalent) from hub documentation / turbine payloads; if missing, fail the attempt and retry (new start)
- Q: Who authors the configuration schedule relative to the LLM? → A: Hybrid — code builds the batch when reports parse cleanly; LLM fills gaps only when parsing fails; model is not the primary author when structured data is available

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Capture the Flag via Correct Plant Setup (Priority: P1)

An operator starts the console application with no arguments. The application
autonomously configures the wind power plant so the turbine produces power at
the best safe wind opportunity and idles safely during every windstorm. While
running, it prints essentially every plant request and response. When the plant
accepts the configuration, the application receives a flag string in the form
`{FLG:...}`, prints it, and exits.

**Why this priority**: Capturing the flag is the sole purpose of the product.

**Independent Test**: Run the application once against the plant configuration
service; success is printing a `{FLG:...}` value and exiting without further
operator input.

**Acceptance Scenarios**:

1. **Given** the plant configuration service is available and credentials are
   configured, **When** the operator starts the application, **Then** the
   application completes the configuration flow and prints a `{FLG:...}`
   string before exiting.
2. **Given** a successful plant validation after the done step, **When** the
   flag appears in the done response, **Then** the application shuts down
   immediately after printing the flag.

---

### User Story 2 - Respect Session Time and Retry Until Limit (Priority: P2)

If a configuration session exceeds 40 seconds, or the plant rejects the
turbine configuration (insufficient power production or unsafe behavior in a
windstorm), the application treats that run as a failed attempt, starts a
fresh session from the beginning, and continues until the flag is captured or
the attempt limit is reached.

**Why this priority**: Without retry handling, a single timeout or wrong
configuration permanently fails the exercise.

**Independent Test**: Simulate a timed-out or rejected session and confirm the
application restarts the full start-to-done flow and stops after the
configured attempt limit.

**Acceptance Scenarios**:

1. **Given** any plant command reports that the configuration session is over
   because the 40-second limit was exceeded, **When** the application detects
   that failure, **Then** it increments the attempt counter and restarts the
   full flow from start.
2. **Given** the plant rejects the turbine configuration as incorrect (or any
   other failure that requires a new start), **When** the attempt fails,
   **Then** the application increments the attempt counter and restarts the
   full flow from start.
3. **Given** the attempt limit (default 5, configurable without CLI args) is
   reached without a flag, **When** the last failed attempt completes,
   **Then** the application exits immediately with a clear failure message and
   does not start another session.

---

### User Story 3 - Build Safe, Power-Maximizing Schedules from Forecasts (Priority: P3)

The application obtains turbine capability data and weather forecasts, waits
for asynchronous reports when needed, and builds a signed configuration batch
that: idles the turbine for every windstorm (wind greater than or equal to
turbine strength), targets maximum production at the strongest safe wind
strictly below that strength, uses hour-precision times (minutes and seconds
set to 0), re-applies idle protection one hour after each storm occurrence
(storm hour + 1h) when another storm follows, and covers only necessary
schedule entries through the first moment power can be produced (relevant
storm idles including post-reset re-idle, plus best safe production)—not calm
or unused hours.

**Why this priority**: Correct schedule logic is what makes plant validation
succeed; it depends on P1 orchestration being present.

**Independent Test**: Provide sample turbine and forecast reports and verify
the resulting configuration batch encodes idle for storms, max production for
best safe wind, hour-aligned times, post-storm re-idle when needed, and a
distinct signing code per batch item from the unlock-code step.

**Acceptance Scenarios**:

1. **Given** a forecast containing windstorm periods, **When** the
   configuration batch is prepared, **Then** those periods set the turbine to
   idle with a pitch that produces no power.
2. **Given** safe wind periods strictly below turbine strength, **When** the
   batch is prepared, **Then** it includes a max-production setup at the
   strongest safe wind opportunity.
3. **Given** wind exactly equal to turbine strength, **When** the batch is
   prepared, **Then** that hour is treated as a windstorm and configured idle.
4. **Given** a windstorm at hour H and another storm at or after H+1h (when
   defaults reset), **When** the batch is prepared, **Then** idle protection is
   configured again for the later storm.
5. **Given** asynchronous weather or turbine reports were ordered, **When**
   results are not yet ready, **Then** the application retrieves them after
   waiting/polling and consumes each generated report only once.
6. **Given** a configuration batch with multiple setup items, **When** unlock
   codes are obtained, **Then** each item has its own distinct code obtained
   before the configuration command is sent.
7. **Given** a forecast with calm hours between storms and the first safe
   production hour, **When** the batch is prepared, **Then** it omits
   configuration items for those calm/unused hours and still includes required
   storm idles and the best safe production setup.
8. **Given** multiple configuration items in one batch, **When** the batch is
   sent, **Then** items are ordered chronologically by configuration hour.
9. **Given** hub documentation/turbine payloads omit idle or max-production
   pitch, **When** the schedule cannot be completed, **Then** the attempt fails
   and restarts from start without inventing pitch values.

---

### Edge Cases

- Session expires mid-flow: every subsequent command indicates the session is
  over; the attempt fails and the full flow restarts.
- Any outcome that forces a new start command counts as one consumed attempt;
  recoverable in-session issues (for example waiting longer for an async
  report while the session is still valid) do not.
- A report is requested again after it was already retrieved: the service may
  return another report in random order or none for that identity; the
  application must not rely on retrieving the same report twice.
- Multiple pending reports exist: retrieval order is random; the application
  must match content to the intended report type rather than assume order.
- No safe production window exists before session timeout: the attempt fails
  and retries within the attempt limit.
- Attempt limit exhausted without a flag: process exits with failure, no
  further plant commands.
- Flag acquired on an early attempt: process exits immediately; remaining
  attempt budget is unused.
- Plant service or required credentials missing at launch: process exits
  immediately with a setup error; no attempt is consumed.
- Wind speed equal to turbine strength: treated as windstorm; must idle (not
  production).
- Forecast items carry occurrence time only (no end time); post-storm default
  reset is evaluated at storm hour + 1 hour.
- Idle or max-production pitch missing from hub documentation/turbine payloads:
  fail the current attempt and restart from start.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST run as a console application that accepts no
  command-line parameters and starts the capture flow immediately on launch.
- **FR-002**: System MUST use the plant configuration tool (`plantTool`) as the
  sole interface for plant session, forecast, turbine, configuration, check,
  and completion operations.
- **FR-003**: System MUST execute each attempt in this order: start
  configuration session; order weather forecast; get turbine report; retrieve
  forecast report; obtain unlock/signing codes and prepare/send a wind-turbine
  configuration batch; execute turbine checked; send done. Unlock codes MUST be
  obtained before the configuration command is sent. Orchestration MUST prefer a
  code-driven session sequencer (or equivalent deterministic control flow) that
  invokes `plantTool` actions in this order. The LLM MAY choose arguments and
  gap-fill unstructured text, but MUST NOT reorder, skip, or invent alternate
  critical steps while a session is active.
- **FR-004**: System MUST treat a windstorm as any period where wind speed is
  greater than or equal to turbine strength, and MUST configure idle (no power
  production) with a pitch angle obtained per FR-021 for every such period in
  the batch.
- **FR-005**: System MUST identify the best safe production opportunity
  (strongest wind still strictly below turbine strength) and configure the
  turbine for maximum production performance at that opportunity using the
  production pitch from FR-021.
- **FR-006**: System MUST send configuration entries as one batch per attempt
  whenever possible to minimize session time, and MUST order batch items
  chronologically by configuration hour (earliest first).
- **FR-007**: System MUST obtain a distinct custom signing code via the
  unlock-code generator for each configuration batch item and include that
  item’s own signature on that setup. A single shared code MUST NOT be reused
  across multiple batch items.
- **FR-008**: System MUST set configuration times with minutes and seconds
  equal to 0 (hour precision only).
- **FR-009**: System MUST treat each weather forecast item as an occurrence at a
  single hour-aligned time (no end time). After a windstorm occurrence, the
  turbine is assumed to reset to defaults one hour later (storm hour + 1h). If
  another windstorm is expected at or after that reset, the system MUST re-apply
  idle protection for that later storm.
- **FR-010**: System MUST include configuration coverage through the first
  moment when power can be produced by sending only necessary batch items:
  idle setups for each relevant windstorm occurrence in that span (including
  post-reset re-idle storms) and a max-production setup at the best safe
  production hour. The system MUST NOT add configuration items for calm or
  otherwise unused hours that are not required for storm safety or that first
  production opportunity.
- **FR-011**: System MUST handle asynchronous plant operations by ordering work
  first and later retrieving results via `getResult`. While waiting, the system
  MUST poll at a configurable interval defaulting to **500ms**, with optional
  backoff up to **2000ms**, and MUST stop polling for that item when (a) a matching
  result arrives, (b) the hub reports the configuration session is over, or
  (c) the session’s 40-second budget is exhausted. There is no separate
  application-level max-wait beyond the hub session window.
- **FR-012**: System MUST retrieve each generated report at most once and MUST
  correctly interpret reports even when multiple results arrive in random order.
- **FR-013**: System MUST complete an entire start-to-done attempt within 40
  seconds of session time; if the limit is exceeded, the attempt MUST be
  abandoned and restarted from scratch.
- **FR-014**: System MUST count a failed attempt whenever a new configuration
  session must be started (resending start), including when the 40-second
  session limit is exceeded or when turbine configuration is rejected for
  insufficient power or unsafe storm behavior. Recoverable problems that do
  not invalidate the current session MUST NOT increment the attempt counter.
- **FR-015**: System MUST allow a configurable maximum number of full-flow
  attempts defaulting to 5, provided without command-line parameters (for
  example environment or project defaults). There is no separate limit on the
  number of individual plant-tool calls within an attempt.
- **FR-016**: System MUST recognize a successful done response containing a
  `{FLG:...}` flag string, print that flag to the console, and shut down
  immediately.
- **FR-017**: System MUST shut down immediately when the attempt limit is
  reached without acquiring a flag, and MUST report a clear failure reason on
  the console.
- **FR-018**: System MUST build the configuration batch with ordinary fixed
  logic (`TurbineScheduleBuilder` or equivalent) whenever turbine and forecast
  data parse into structured values. Language-model assistance MUST be limited
  to (a) orchestrating `plantTool` calls and (b) interpreting unstructured or
  ambiguous plant text only when fixed parsing is insufficient. When structured
  data is available, the model MUST NOT be the primary author of the
  configuration batch. Hybrid gap-fill by the model is allowed only for the
  unparseable portions, and MUST remain aligned with the project constitution
  for how model access is provided.
- **FR-019**: If required credentials or the plant configuration service are
  unavailable at launch (before any start session is established), the system
  MUST exit immediately with a clear setup error on the console and MUST NOT
  increment the attempt counter.
- **FR-020**: System MUST write verbose console output for essentially every
  plant request and response during an attempt, in addition to attempt
  lifecycle messages (start of attempt, success flag, or final failure).
- **FR-021**: System MUST obtain idle and max-production pitch angles (or
  equivalent blade settings) from hub `documentation` and/or turbine-related
  payloads (`turbinecheck` / related reports). If those pitch values cannot be
  determined from hub responses, the system MUST treat the attempt as failed
  and restart from `start` (consuming an attempt per FR-014). The system MUST
  NOT invent pitch constants outside hub-provided guidance.

### Key Entities

- **Configuration Session**: A timed plant-setup attempt with a hard 40-second
  lifetime from start until done or expiry.
- **Turbine Report**: Capability and strength information used to distinguish
  safe wind from windstorm and to choose production settings.
- **Weather Forecast Report**: Ordered asynchronously; contains wind timeline
  items each with an occurrence time only (no end time); used to plan idle and
  production windows; each generated report is consumable once.
- **Configuration Batch**: Hour-aligned, signed schedule of necessary turbine
  setups only—storm idle periods (including post-reset re-idle) and best safe
  production through first power—not calm/unused hours; items ordered
  chronologically by hour.
- **Unlock / Signing Code**: Per-item value required to sign each configuration
  setup before the plant accepts the batch; each batch item has its own code,
  obtained before the configuration command is sent.
- **Flag**: Success token of the form `{FLG:...}` returned when done succeeds.
- **Attempt**: One full start-to-done cycle; an attempt is consumed whenever a
  new start is required after failure; in-session recoverable work does not
  consume an extra attempt.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An operator can start the application with a single no-argument
  launch and, without further interaction, either see a `{FLG:...}` value, a
  clear exhausted-attempts failure, or an immediate clear setup error when the
  plant service or required credentials are missing before any session starts.
- **SC-002**: On a correct configuration path, the application obtains and
  surfaces the flag from the plant’s done response within one successful
  attempt’s 40-second session budget.
- **SC-003**: After any failure that requires resending start (including
  session timeout or incorrect configuration), the application begins a new
  full attempt automatically and never exceeds the configured attempt limit
  (default 5).
- **SC-004**: For forecasts that include windstorms and safe wind windows, a
  successful run produces a plant-accepted schedule that idles through every
  storm and produces power at the strongest safe opportunity.
- **SC-005**: 100% of completed successful runs print the flag and terminate
  immediately afterward; 100% of runs that exhaust attempts terminate without
  leaving a dangling session expectation for the operator.
- **SC-006**: During each attempt, an operator can observe essentially every
  plant request and response on the console before the process exits.

## Out of Scope

- Interactive operator control after launch, GUIs, and HTTP servers
- Submitting the flag to any system other than printing it on the console
- Multi-plant or multi-scenario support beyond this single CTF exercise
- Optimizing for hours after the first successful power-production opportunity
  once required storm safety through that point is covered

## Assumptions

- Glossary: **Capture run** = one JVM process. **Session attempt** = one hub
  `start`→`done` (or expiry); counted by FR-014/FR-015.
- Plant tool operation names and behaviors match the exercise description
  (start, forecast order, turbine report, forecast retrieve, batch configure,
  checked, done, unlock-code generator, async get-result style retrieval).
- “Enough power” and “safely turned off during windstorm” are judged by the
  plant service; the application’s job is to satisfy those rules using turbine
  strength and forecast data.
- Exact idle pitch and max-production blade settings MUST come from hub
  documentation/turbine payloads (FR-021); they are not soft assumptions.
- Schedule authorship is hybrid per FR-018: code builds when data parses;
  LLM gap-fills only when parsing fails.
- Windstorm threshold is wind ≥ turbine strength (clarified); equal strength
  must idle even if earlier exercise wording said only “higher than.”
- Attempt limit is configurable via environment or project defaults, not CLI
  arguments, preserving the parameter-free console constitution.
- Network access to the plant configuration service and required secrets are
  available in the runtime environment before launch.
- One process run may include multiple session attempts up to the limit, then exits;
  the operator starts a new process only if they want another overall capture run.
- Process exit codes: `0` on flag success; non-zero on setup failure or exhausted
  session-attempt budget (exact non-zero values are an implementation choice).
