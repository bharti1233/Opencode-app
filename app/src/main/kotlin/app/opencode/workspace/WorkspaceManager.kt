package app.opencode.workspace

import java.io.File

// Port of project/project.ts fromDirectory/upsert: projects live under one base
// dir; everything outside it is flagged external (tool layer then requires
// external_directory, enforced in ToolRunner).

data class Project(val name: String, val dir: File, val external: Boolean)

class WorkspaceManager(private val base: File) {
    fun list(): List<Project> =
        (base.listFiles() ?: emptyArray()).filter { it.isDirectory }
            .map { Project(it.name, it, external = false) }.sortedBy { it.name }

    /** Creates a project; names are confined to a safe subset. */
    fun create(name: String): Project {
        require(name.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"))) { "bad project name: $name" }
        val dir = File(base, name)
        dir.mkdirs()
        return Project(name, dir, external = false)
    }

    /** Opens any existing dir; outside base => external (permission-gated tools). */
    fun open(dir: File): Project? {
        if (!dir.isDirectory) return null
        val canon = dir.canonicalPath
        val underBase = canon == base.canonicalPath ||
            canon.startsWith(base.canonicalPath + File.separator)
        return Project(dir.name, dir, external = !underBase)
    }

    fun base(): File = base
}
