package app.opencode.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

// Port of tool/shell.ts execution half: cwd, env inherit, stdout/stderr split,
// exit code, timeout kill. Android deviations: `sh -c` (no $SHELL assumption),
// no process-group kill (plain destroy), one-shot runs enforce timeout but are
// only cancellable via TerminalManager-tracked sessions.

class ProcessCommandExecutor : CommandExecutor {
    private val tracked = ConcurrentHashMap<String, Process>()

    override suspend fun run(command: String, workdir: String, timeoutMs: Long): CommandResult =
        withContext(Dispatchers.IO) {
            val dir = File(workdir)
            if (!dir.isDirectory) return@withContext CommandResult(-1, "", "no such directory: $workdir")
            val proc = try {
                ProcessBuilder("sh", "-c", command).directory(dir).start()
            } catch (e: Exception) {
                return@withContext CommandResult(-1, "", e.message ?: "spawn failed")
            }
            try {
                val out = ByteArrayOutputStream()
                val err = ByteArrayOutputStream()
                val t1 = thread(isDaemon = true) { proc.inputStream.copyTo(out) }
                val t2 = thread(isDaemon = true) { proc.errorStream.copyTo(err) }
                try {
                    proc.outputStream.close()
                } catch (_: Exception) {
                }
                if (!proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                    proc.destroyForcibly()
                    t1.join(2000)
                    t2.join(2000)
                    return@withContext CommandResult(124, out.toString(), err.toString() + "\n[timed out]")
                }
                t1.join()
                t2.join()
                CommandResult(proc.exitValue(), out.toString(), err.toString())
            } finally {
                proc.destroyForcibly()
            }
        }

    override fun cancel(sessionId: String) {
        tracked[sessionId]?.destroyForcibly()
    }

    internal fun track(id: String, proc: Process) {
        tracked[id] = proc
    }

    internal fun untrack(id: String) {
        tracked.remove(id)
    }
}
