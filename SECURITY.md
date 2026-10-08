# Security

- API keys: Android Keystore AES-256-GCM (SecureKeys). Never logged; headers only.
- Permission rules: plain prefs (not secrets). GitHub PAT: env-provided per push, Basic auth; never in source/config/history (verified pre-push).
- Agent containment: workspace rooting via canonical paths; external targets need explicit allow; deny-by-default ASK verdicts; destructive patterns (e.g. `rm *`) matchable by user rules.
- Binary/network trust: no vendored binaries; OkHttp TLS default; MCP servers user-configured.
- Audit: `grep -rn "ghp_\|sk-" app/src` must be empty of real secrets before any push.
