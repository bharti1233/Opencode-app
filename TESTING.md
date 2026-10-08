# Testing

CI-only (`gradle testDebugUnitTest assembleDebug` on GitHub Actions; no local runs per policy).

Strategy: pure-logic unit tests, zero network/device: permission matching, retry math, request JSON shapes, both SSE parsers, tool roundtrips on temp dirs, deny/ask/external gates, process echo/exit/timeout, live interactive marker, git on temp repos, Room mapping roundtrips, subagent flows with scripted FakeClient, JSON-RPC framing + fake-server + MockWebServer fetch.

Device-dependent code (Keystore load, Room I/O, interactive UI) is kept thin behind tested pure cores (encrypt/decrypt, SessionTables, PermissionSession).
