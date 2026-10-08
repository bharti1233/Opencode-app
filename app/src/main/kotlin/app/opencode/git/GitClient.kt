package app.opencode.git

import app.opencode.terminal.CommandExecutor
import app.opencode.terminal.ProcessCommandExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Port of git/index.ts CLI wrapper. No dedicated agent tools: like the
// original, the agent uses bash for git; this client serves the UI +
// workspace layer. Requires a git binary (Termux `pkg install git`);
// every failure surfaces as GitError, never silent.

class GitError(message: String) : Exception(message)

class GitClient(
    private val workdir: String,
    private val exec: CommandExecutor = ProcessCommandExecutor(),
) {
    private suspend fun git(vararg args: String): String = withContext(Dispatchers.IO) {
        val cmd = (listOf("git", "--no-optional-locks", "-c", "core.quotepath=false") + args)
            .joinToString(" ") { if (it.contains(' ')) "'$it'" else it }
        val r = exec.run(cmd, workdir, 60_000)
        if (r.exitCode != 0) throw GitError((r.stderr.ifEmpty { r.stdout }).trim().ifEmpty { "git failed" })
        r.stdout.trim()
    }

    suspend fun status(): String = git("status", "--porcelain")
    suspend fun diff(ref: String? = null): String =
        if (ref == null) git("diff") else git("diff", ref)
    suspend fun log(limit: Int = 20): String = git("log", "--oneline", "-$limit")
    suspend fun add(path: String): String = git("add", path)
    suspend fun commit(message: String): String = git("commit", "-m", message)
    suspend fun branch(): String = git("branch")
    suspend fun checkout(ref: String): String = git("checkout", ref)
    suspend fun stash(): String = git("stash")
    suspend fun pull(): String = git("pull")
    suspend fun push(): String = git("push")
    suspend fun isRepo(): Boolean = try {
        git("rev-parse", "--git-dir")
        true
    } catch (_: GitError) {
        false
    }

    companion object {
        /** Clone, then strip any token-bearing URL back to the clean remote. */
        suspend fun clone(
            url: String,
            into: java.io.File,
            cleanRemote: String? = null,
            exec: CommandExecutor = ProcessCommandExecutor(),
        ): GitClient {
            val parent = into.parentFile ?: throw GitError("no parent dir")
            val r = exec.run("git clone $url ${into.name}", parent.path, 120_000)
            if (r.exitCode != 0) throw GitError(r.stderr.ifEmpty { r.stdout }.trim())
            val client = GitClient(into.path, exec)
            if (cleanRemote != null) client.git("remote", "set-url", "origin", cleanRemote)
            return client
        }
    }
}
