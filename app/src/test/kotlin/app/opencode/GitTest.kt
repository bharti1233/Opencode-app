package app.opencode

import app.opencode.git.GitClient
import app.opencode.git.GitError
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GitTest {
    @get:Rule val tmp = TemporaryFolder()

    private suspend fun initRepo(): GitClient {
        val g = GitClient(tmp.root.path)
        val setup = app.opencode.terminal.ProcessCommandExecutor()
        setup.run("git init -b main", tmp.root.path, 10_000)
        setup.run("git config user.email t@t", tmp.root.path, 10_000)
        setup.run("git config user.name t", tmp.root.path, 10_000)
        File(tmp.root, "a.txt").writeText("v1")
        return g
    }

    @Test fun statusCommitLog() = runBlocking {
        val g = initRepo()
        assertEquals("?? a.txt", g.status().lines().firstOrNull { it.contains("a.txt") })
        g.add("a.txt")
        g.commit("first")
        assertEquals("", g.status())
        assertTrue(g.log().contains("first"))
    }

    @Test fun diffBranch() = runBlocking {
        val g = initRepo()
        g.add("a.txt")
        g.commit("first")
        File(tmp.root, "a.txt").writeText("v2")
        assertTrue(g.diff().contains("v2"))
        assertTrue(g.branch().contains("main"))
    }

    @Test fun cloneLocal() = runBlocking {
        val src = tmp.newFolder("src")
        val setup = app.opencode.terminal.ProcessCommandExecutor()
        setup.run("git init -b main", src.path, 10_000)
        setup.run("git config user.email t@t", src.path, 10_000)
        setup.run("git config user.name t", src.path, 10_000)
        java.io.File(src, "f.txt").writeText("x")
        setup.run("git add f.txt && git commit -m init", src.path, 10_000)
        val dst = java.io.File(tmp.root, "dst")
        val g = GitClient.clone(src.path, dst)
        assertEquals(true, g.isRepo())
        assertTrue(g.log().contains("init"))
    }

    @Test fun notARepo() = runBlocking {
        val g = GitClient(tmp.newFolder("empty").path)
        assertEquals(false, g.isRepo())
        try {
            g.log()
            org.junit.Assert.fail("expected GitError")
        } catch (e: GitError) {
            assertTrue((e.message ?: "").isNotEmpty())
        }
    }
}
