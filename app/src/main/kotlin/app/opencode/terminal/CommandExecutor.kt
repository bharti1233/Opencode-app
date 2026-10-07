package app.opencode.terminal

// Port of tool/shell.ts lifecycle (cwd, env, streaming, exit code, cancel).
// ProcessBuilder implementation lands in Phase 7; no local execution in this phase.

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)

interface CommandExecutor {
    suspend fun run(command: String, workdir: String, timeoutMs: Long = 120_000): CommandResult
    fun cancel(sessionId: String)
}

interface TerminalSession {
    val id: String
    val workdir: String
    fun sendInput(text: String)
    fun cancel()
}

interface TerminalManager {
    fun open(workdir: String): TerminalSession
    fun list(): List<TerminalSession>
}
