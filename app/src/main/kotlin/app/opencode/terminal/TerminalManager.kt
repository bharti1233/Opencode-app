package app.opencode.terminal

import java.io.File

// Interactive shell behind the same workspace the agent uses (Phase 7 UI lands
// in Phase 14; the session mechanics are real now).

class TerminalSessionImpl(
    override val id: String,
    override val workdir: String,
    private val proc: Process,
    private val untrack: () -> Unit,
) : TerminalSession {
    private val buf = StringBuilder()

    init {
        proc.inputStream.bufferedReader().forEachLineAsync { line ->
            synchronized(buf) {
                buf.append(line).append('\n')
                if (buf.length > 100_000) buf.delete(0, buf.length - 100_000)
            }
        }
    }

    fun output(): String = synchronized(buf) { buf.toString() }

    fun isAlive(): Boolean = proc.isAlive

    override fun sendInput(text: String) {
        proc.outputStream.write((text + "\n").toByteArray())
        proc.outputStream.flush()
    }

    override fun cancel() {
        proc.destroyForcibly()
        untrack()
        try {
            proc.outputStream.close()
        } catch (_: Exception) {
        }
    }

    private fun java.io.BufferedReader.forEachLineAsync(fn: (String) -> Unit) {
        kotlin.concurrent.thread(isDaemon = true, name = "term-$id") {
            try {
                forEachLine(fn)
            } catch (_: Exception) {
            }
        }
    }
}

class ProcTerminalManager(private val exec: ProcessCommandExecutor = ProcessCommandExecutor()) :
    TerminalManager {
    private val sessions = mutableMapOf<String, TerminalSessionImpl>()
    private var n = 0

    @Synchronized
    override fun open(workdir: String): TerminalSession {
        // -i: interactive flush-before-read; without a PTY a plain sh may
        // block-buffer stdout and the reader thread would see nothing.
        val proc = ProcessBuilder("sh", "-i").directory(File(workdir)).start()
        val id = "term${++n}"
        exec.track(id, proc)
        val s = TerminalSessionImpl(id, workdir, proc) {
            exec.untrack(id)
            synchronized(this) { sessions.remove(id) }
        }
        sessions[id] = s
        return s
    }

    @Synchronized
    override fun list(): List<TerminalSession> = sessions.values.toList()
}
