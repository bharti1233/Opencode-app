package app.opencode.agent

// Port of session/system.ts environment + instruction.ts assembly (pure half).
// Base prompt texts live in assets/prompts/; providers pick the file (Phase 1 map).

object PromptBuilder {
    fun environment(model: String): String =
        "You are a coding agent powered by $model, running inside an Android app " +
            "with file tools, shell access, and user-approved permissions."

    fun build(base: String, model: String, instructions: List<String> = emptyList()): String =
        (listOf(base, environment(model)) + instructions)
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
}
