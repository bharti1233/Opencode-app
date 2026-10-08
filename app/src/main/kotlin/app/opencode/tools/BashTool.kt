package app.opencode.tools

import app.opencode.terminal.CommandExecutor
import java.io.File

// Port of tool/shell.ts (params command/timeout?/workdir?/description?).
// Command-string scanning for file patterns is deferred (Phase 7 scope-out):
// permission patterns match the full command and the user decides.

class BashTool(private val exec: CommandExecutor, root: File) : RootedTool(root) {
    override val name = "bash"
    override val description = "Run a shell command"
    override val inputSchemaJson =
        """{"type":"object","properties":{"command":{"type":"string"},"timeout":{"type":"number"},"workdir":{"type":"string"},"description":{"type":"string"}},"required":["command"]}"""
    override val permission = "bash"
    override val requiredArgs = listOf("command")
    override fun target(args: Map<String, String>) = args["command"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val wd = resolve(args["workdir"] ?: ".")
        if (isExternal(wd.path)) return ToolResult("", error = "external directory needs permission: ${wd.path}")
        val timeout = args["timeout"]?.toLongOrNull() ?: 120_000L
        val r = exec.run(args.getValue("command"), wd.path, timeout)
        val sb = StringBuilder(r.stdout)
        if (r.stderr.isNotEmpty()) sb.append("\n[stderr]\n").append(r.stderr)
        if (r.exitCode != 0) sb.append("\n[exit ${r.exitCode}]")
        return ToolResult(sb.toString().trim(), error = if (r.exitCode == -1 && r.stdout.isEmpty()) r.stderr else null)
    }
}
