# Phase 13 — GitHub Integration

Connected since commit 1: `origin https://github.com/bharti1233/Opencode-app.git`, `main`, every phase pushed + CI-verified.

## What landed

- `git/GitClient.clone(url, into, cleanRemote)` — clone then strip token-bearing URLs back to the clean remote (tokens never persist in `.git/config`).
- Auth: PAT via Basic auth (`x-access-token`, env-provided per push); Bearer headers do NOT work for git-over-HTTPS (diagnosed run 0). Provider API keys live in Keystore (Phase 3), never in source/logs.
- Verified clean before every push: remote URL contains no credential; `grep ghp_` hits only the audit-pattern doc.

## Deferred

In-app clone UI (Phase 14 Git screen uses this), credential-helper storage on device, PR API (out of scope for agent core).
