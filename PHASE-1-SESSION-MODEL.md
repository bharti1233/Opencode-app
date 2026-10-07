# Phase 1 — Session Model

Session row: id/slug/projectID/directory/parentID/title/agent/model/cost/tokens/permission/time{created,updated,compacting,archived}/revert/share (`session/session.ts:59,426`).

Messages: User{id sessionID role agent model format system tools summary} / Assistant{…parentID modelID providerID mode path{cwd,root} cost tokens{input,output,reasoning,cache} time finish error summary} / Parts: text/reasoning/file/agent/compaction/subtask/retry/step-start/step-finish/tool(callID/tool/state+metadata)/patch/snapshot.

Compaction: overflow when total>=usable-reserved; create(auto) appends user+compaction part; process selects tail (preserve_recent_tokens 25%, tail_turns), serializes head (2k trunc), runs tool-less agent, updates tail_start_id, replays overflowed user msg or synthetic Continue; opt-in prune (40k/20k). Summary forked per msg/step.

Android: Room entities mirror rows (`session/Session.kt`); history survives app restart; streaming deltas -> Part updates.
