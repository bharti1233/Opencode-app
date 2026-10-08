# Build

CI-only. No local builds (project policy).

## Debug

Push to `main` (or open a PR) → `android` workflow → `gradle testDebugUnitTest assembleDebug` → `app-debug` artifact (`app/build/outputs/apk/debug/*.apk`).

## Release

Unsigned release is NOT shipped (no fake-signed artifacts). To produce one:

1. Create a keystore; add to GitHub Secrets: `KEYSTORE_B64`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`.
2. Extend `.github/workflows/android.yml` with a signed `assembleRelease` job.
3. Tag; verify `apksigner verify` in CI.

Toolchain: AGP 8.5.2, Kotlin 2.0.20, JDK 17 (Temurin), compileSdk/targetSdk 34, minSdk 26, Gradle 8.10.2 (setup-gradle, no wrapper binary in repo).
