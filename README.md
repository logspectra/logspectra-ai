# logspectra-ai

Code-aware analysis library for LogSpectra. Used by **logspectra-backend** (not by client microservices).

## What it does

1. Maps `service` name → local source checkout (config).
2. Parses stack traces / logger names from logs.
3. Reads nearby source snippets from disk.
4. Enriches the LLM analysis prompt via `LogSpectraAiFacade`.

LLM HTTP calls (OpenAI / Claude / Ollama) still run in the backend today; this module owns **code context + prompt enrichment**.

## Install locally

```bash
cd logspectra-ai
mvn clean install
```

## Config (in logspectra-backend `application.yml`)

```yaml
logspectra:
  ai:
    enabled: true
    code-context:
      enabled: true
      provider: local   # or git (falls back to local-path)
      max-snippets: 8
      context-lines: 20
      services:
        gateway-service:
          local-path: ${user.home}/Developer/project/gateway-service
          git-url: https://github.com/you/gateway-service.git
          source-roots:
            - src/main/java
```

See `SETUP-CODE-CONTEXT.md` for git clone / RAG next steps.
