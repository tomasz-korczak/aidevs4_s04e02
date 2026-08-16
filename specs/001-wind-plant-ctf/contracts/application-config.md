# Contract: Application configuration

## Environment variables

| Name | Required | Purpose |
|------|----------|---------|
| `HUB_API_KEY` | Yes | Hub `apikey` field |
| `OPENROUTER_API_KEY` | Yes | Spring AI OpenAI-compatible client → OpenRouter |
| `JAVA_HOME` | Recommended | `C:\tools\jdk-23.0.2` |

No command-line parameters.

## `application.yml` (illustrative)

```yaml
spring:
  main:
    web-application-type: none
  ai:
    openai:
      api-key: ${OPENROUTER_API_KEY}
      base-url: ${app.llm.openrouter-base-url}
      chat:
        options:
          model: ${app.llm.model}

app:
  hub:
    verify-url: https://hub.ag3nts.org/verify
    task: windpower
  llm:
    model: inclusionai/ling-3.0-flash
    openrouter-base-url: https://openrouter.ai/api
  plant:
    max-session-attempts: 5
  prompt:
    system-template-location: classpath:prompts/system-prompt.st

logging:
  file:
    name: logs/s04e02.log
```

## Parameterization checklist

| Concern | Property / env |
|---------|----------------|
| Hub API key | `HUB_API_KEY` |
| OpenRouter API key | `OPENROUTER_API_KEY` |
| plantTool session attempt limit | `app.plant.max-session-attempts` (default 5) |
| HTTP addresses | `app.hub.verify-url`, `app.llm.openrouter-base-url` |
| Main system prompt | `app.prompt.system-template-location` (StringTemplate resource) |
| LLM model name | `app.llm.model` → `spring.ai.openai.chat.options.model` |

## System prompt template requirements

Resource `classpath:prompts/system-prompt.st` MUST instruct the agent to:

1. Call `start` first; respect 40s session; on session-over, restart with new `start` (counts as attempt)
2. Order: start → get weather + turbine data (poll `getResult`) → build necessary configs → unlockCodeGenerator per item → batch `config` chronological → `get` turbinecheck (+ getResult) → `done`
3. Windstorm when wind ≥ turbine strength → idle + non-producing pitch
4. Best production at strongest wind strictly below strength
5. Hour precision only (minutes/seconds 0)
6. After storm at H, reset at H+1; re-idle later storms
7. Necessary hours only; one unlock code per config item; never reuse codes
8. Prefer one batch `config` with `configs`
9. Stop when `{FLG:...}` appears; surface it
10. Obtain idle and max-production pitch from hub documentation/turbine payloads; if either is missing, fail the attempt and call start again (do not invent pitches).
11. When forecast/turbine data is structured, prefer deterministic schedule construction; use model judgment only for unparseable gaps.

Template variables may include attempt limit, verify URL (non-secret), and model name for operator context—never embed API keys in the prompt.
