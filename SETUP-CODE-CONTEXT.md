# Code-aware analysis — how it works now

## Is client git-url → starter → Redis the right way?

**Yes for service identity.** Each microservice owns its repo URL; the starter registers it; the backend caches it in Redis and uses it during Analyze.

```
client application.yml (logspectra.code-source.git-url)
        ↓ starter startup POST
logspectra-backend /api/v1/services/code-registry
        ↓
Redis key: logspectra:code-registry:{projectId}:{service}
        ↓ on Analyze
RedisServiceSourceLocator → LocalFilesystemCodeContextProvider
        ↓
snippets appended to LLM prompt
```

**Caveat:** `local-path` must be a path the **backend host** can read (same laptop or shared volume). A path inside a client Docker container is useless to the backend unless shared. Prefer `git-url` + clone-on-backend later.

## Where is RAG?

**Not used yet.** Today we do **deterministic retrieval**:
1. Parse stack frames / logger names from the log
2. Map class → `src/main/java/.../File.java`
3. Read ±N lines around the error line

RAG (embeddings + vector search) would be a **future** `CodeContextProvider` implementation (`provider: rag`) that queries an index built in CI. See section below.

## Client config (per service)

```yaml
logspectra:
  service-name: payment-service
  project-id: <uuid>
  code-source:
    git-url: https://github.com/you/payment-service.git
    default-branch: main
    local-path: /Users/you/Developer/project/payment-service   # optional, backend-visible
  registry:
    enabled: true
    backend-url: http://localhost:8081
    token: ""   # must match logspectra.ai.registry.token on backend if set
```

## Backend

- Endpoint (public, optional token): `POST /api/v1/services/code-registry`
- Redis TTL: 30 days
- Analyze path: Redis first, YAML fallback

## Verify

```bash
# After client boot
redis-cli GET 'logspectra:code-registry:<projectId>:payment-service'

# Or
curl -s -X POST http://localhost:8081/api/v1/services/code-registry \
  -H 'Content-Type: application/json' \
  -d '{"projectId":"...","serviceName":"payment-service","gitUrl":"https://...","localPath":"/path/on/backend"}'
```

Then Analyze an ERROR with a stack in the UI.

## GitHub Contents (no clone) — preferred when App is installed

When `github.app.enabled=true` in **logspectra-backend**, it registers a `RemoteSourceReader`.
`logspectra-ai` then cascades: local disk if present, else GitHub Contents API (in-memory).

```yaml
logspectra:
  ai:
    code-context:
      provider: local   # cascades to github when RemoteSourceReader exists
      # or: provider: github   # remote only
```

Requires only `git-url` (+ optional `default-branch`, `source-roots`) in the registry — **no local-path**.

## Remediation PRs (GitHub App)

After analysis with `createRemediationPr=true`, backend opens a branch + PR (source patches when the model returns `filePath`/`newContent`).
See `logspectra-backend/SETUP-GITHUB-APP.md`.

## Optional: git clone

Still available via `provider: git` + `local-path` after a manual/JGit clone. Prefer Contents API for zero disk.

## Later: RAG

1. CI: embed `src/main/java` chunks → vector DB keyed by `projectId + service + commit`
2. New `RagCodeContextProvider implements CodeContextProvider`
3. Set `logspectra.ai.code-context.provider=rag`
