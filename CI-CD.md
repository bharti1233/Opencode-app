# CI/CD

Workflow `.github/workflows/android.yml` on push/PR: checkout -> JDK 17 (Temurin) -> setup-gradle 8.10.2 (no wrapper binary) -> `testDebugUnitTest assembleDebug` -> upload `app-debug` APK. No setup-android (runners ship a preinstalled SDK; the v3 action's stale cmdline-tools download broke run 1).

Policy per phase: implement -> test (CI) -> review -> compare vs original -> fix -> commit -> push -> inspect Actions -> diagnose from logs -> fix cause -> repeat until green -> verify APK artifact exists. Blind re-runs prohibited. See IMPLEMENTATION-LOG.md for all 31 runs.
