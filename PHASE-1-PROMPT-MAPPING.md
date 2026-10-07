# Phase 1 — Prompt Mapping

Base per model (`session/system.ts:provider()`): default.txt (fallback), anthropic.txt, beast.txt (legacy gpt), gpt.txt, gpt-astra.txt, codex.txt, gemini.txt, kimi.txt, meta.txt ({{MODEL_NAME}}), trinity.txt, copilot-gpt-5.txt + plan.txt / plan-reminder-anthropic.txt / plan-mode.txt / build-switch.txt reminders via SessionReminders.

Dynamic: environment (`You are powered by…<env>`), skills appendix, mcp appendix, instruction.ts system (AGENTS.md/CLAUDE.md + config instructions), user.system override.

Specialists: explore.txt (read-only search), compaction.txt (structured summary), summary.txt (PR-style), title.txt (<=50ch), generate.txt (agent JSON synth).

Android: verbatim assets under `app/src/main/assets/prompts/` (this skeleton ships `default.txt` placeholder + full set follows in Phase 8); `PromptBuilder.kt` assembles [base, env, instructions, mcp, skills, reminders].
