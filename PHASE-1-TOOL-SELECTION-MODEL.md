# Phase 1 — Tool Selection Model

No keyword routing. Selection = model inference over: system instructions + agent instructions + project context + conversation + tool definitions/descriptions/schemas + permissions (deny hides tool) + model caps (toolcall flag).

Kotlin duty: expose correct tools (registry + MCP, sorted keys, `toolChoice`), validate schema (repair -> `invalid` on miss), enforce permission (`ask`, doom-loop guard after 3 identical calls), execute, return accurate result, update context, continue loop. (`session/tools.ts`, `llm/request.ts:resolveTools`, `llm.ts:streamText`, `processor.ts:358`.)
