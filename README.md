# Opencode App — Android-native port

Behavioral port of OpenCode's coding-agent architecture (agent loop, model-driven tool selection, permissions, sessions) to Kotlin + Jetpack Compose. Audited from opencode-1.18.35 (see `PHASE-0-*.md`, `PHASE-1-*.md`).

## Screens

Projects (create/open) — Chat (real agent loop, streaming, permission dialog) — Term (live shell, same workspace) — Edit (browse, numbered view, diff accept/reject) — Settings (provider/model + Keystore API key).

## Status

All 22 phases executed; CI green; debug APK artifact per build. Details: FINAL-REPORT.md. Compatibility: FINAL-OPENCODE-COMPATIBILITY-AUDIT.md. Limits: ANDROID-LIMITATIONS.md.

## Credentials

Never committed. API keys in Keystore (in-app Settings); pushes via env PAT + Basic auth. Audit: `grep -rn "ghp_" app/src` must show no real secrets.

## Build

GitHub Actions only (see BUILD.md, CI-CD.md). No local builds.
