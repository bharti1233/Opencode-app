# Opencode App — Android-native port

Behavioral port of OpenCode's coding-agent architecture (agent loop, model-driven tool selection, permissions, sessions) to Kotlin + Jetpack Compose. See `PHASE-0-*.md` and `PHASE-1-*.md`.

Credentials: never committed. Provide API keys in-app (Keystore, Phase 3) and CI via GitHub Secrets.

Build: GitHub Actions only (`android` workflow runs unit tests + `assembleDebug`, uploads APK). No local builds.
