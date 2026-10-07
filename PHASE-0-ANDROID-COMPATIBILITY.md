# Phase 0 — Android Compatibility

- Shell (`tool/shell.ts`, `core/shell.ts`): params command/timeout?/workdir?; cwd resolve(instance.directory); shell select `$SHELL` else /bin/sh|zsh|bash, fish/nu denied; spawn `ChildProcess` detached (non-win), env merge, streaming ring buffer + trunc-file spill, race exit/abort/timeout, `process.kill(-pid)` group kill. ANDROID: prefer `$PREFIX/bin/bash` -> `sh` fallback; `detached:false` + plain kill (no -pid); no `/etc/shells`, `$SHELL` assumptions.
- FS (`read/edit/glob/grep/apply_patch`, `core/fs-util.ts` NodeFileSystem): portable except win32 normalize; read caps 2000 lines/50KB/2000 chars/line, binary detect; edit BOM/CRLF-preserving + per-file semaphore + format + LSP diagnostics (non-fatal). ANDROID: KEEP logic; scope to app workspace + SAF external dirs; no sdcard chmod/symlink assumptions.
- ripgrep (`core/ripgrep/binary.ts`, `core/ripgrep.ts`): PLATFORM keys darwin/linux/win32 only -> throws on android; thin `rg --files/--json` wrapper, limit 100. ANDROID: require system `rg` (`pkg install ripgrep`) or vendor aarch64 binary.
- Watcher (`core/filesystem/watcher.ts`): `@parcel/watcher-<platform>-<arch>` (win32/fs-events/inotify); already false-safe (`hasNativeBinding()==false` -> no-op). ANDROID: keep disabled / poll fallback.
- Bun-isms: `bun:sqlite` vs `node:sqlite` dual exists — boot on Node path; `Bun.hash` (skill/discovery.ts:113) -> `node:crypto`; wasm (`tree-sitter` bash/powershell.wasm via resolveWasm/fileURLToPath) needs Node-compatible loader; `bunfig.toml`, `bun --bun script/build.ts`. ANDROID: Node path only (Kotlin side: Room/SQLite, no bun).
- LSP (`lsp/launch.ts`, `lsp/server.ts` ~30 servers): downloads are linux/darwin/win32 x64/arm64 glibc builds; all failures `catch->undefined` (fail-soft). ANDROID: default `lsp:false`, or Termux pkgs.
- Format (`format/`): `which()`-gated, missing binary = no-op. ANDROID: keep no-op default.
- TUI (opentui/solid, needs TTY), desktop (electron): NOT portable — serve via native Compose UI instead.
- Global paths (`Global.Path.{bin,cache}` under `$HOME`): remap to app files/cache dirs; Termux `$PREFIX` differs.
