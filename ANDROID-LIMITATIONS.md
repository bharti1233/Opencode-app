# Android Limitations

- No PTY: interactive shells are pipes (`sh -i` for flush behavior); full terminal emulation (colors, curses) unsupported.
- Process survival: Doze/App kill ends long runs; no foreground-service yet — prefer short commands; timeouts enforced client-side.
- No ripgrep/LSP/format binaries: pure-Kotlin search; diagnostics via model + build output.
- Files: app-private projects dir full access; outside dirs need SAF (picker pending) or path-open (gated by external_directory).
- Shell: `sh -c` only; no `$SHELL`/fish/nu; no process-group kill.
- Signed release: no keystore wired; debug APK only.
- Tokens: no on-device usage accounting yet, so no auto-compaction (overflow surfaces as failure).
