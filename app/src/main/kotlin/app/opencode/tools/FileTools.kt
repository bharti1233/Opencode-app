package app.opencode.tools

import java.io.File
import java.nio.file.FileSystems

// Port of tool/read.ts + write.ts + edit.ts + glob.ts + grep.ts (filesystem half).
// Rooted at a workspace dir; targets outside it need external_directory ALLOW
// (enforced in ToolRunner). ripgrep binary comes later; matching is pure Kotlin.

abstract class RootedTool(val root: File) : AgentTool {
    override fun target(args: Map<String, String>): String? = null
    override fun isExternal(target: String): Boolean {
        val dest = if (File(target).isAbsolute) File(target) else File(root, target)
        return !dest.canonicalPath.startsWith(root.canonicalPath + File.separator) &&
            dest.canonicalPath != root.canonicalPath
    }

    protected fun resolve(path: String): File =
        if (File(path).isAbsolute) File(path) else File(root, path)
}

class ReadTool(root: File) : RootedTool(root) {
    override val name = "read"
    override val description = "Read a file or list a directory"
    override val inputSchemaJson =
        """{"type":"object","properties":{"filePath":{"type":"string"},"offset":{"type":"number"},"limit":{"type":"number"}},"required":["filePath"]}"""
    override val permission = "read"
    override val requiredArgs = listOf("filePath")
    override fun target(args: Map<String, String>) = args["filePath"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val f = resolve(args.getValue("filePath"))
        if (!f.exists()) return ToolResult("", error = "no such file: ${args["filePath"]}")
        if (f.isDirectory) {
            val names = (f.list() ?: emptyArray()).sorted()
                .joinToString("\n") { if (File(f, it).isDirectory) "$it/" else it }
            return ToolResult(names)
        }
        val bytes = f.readBytes()
        if (bytes.size > 5_000_000) return ToolResult("", error = "file too large")
        val text = bytes.toString(Charsets.UTF_8)
        val lines = text.lines()
        val offset = args["offset"]?.toIntOrNull() ?: 0
        val limit = args["limit"]?.toIntOrNull() ?: 2000
        return ToolResult(lines.drop(offset).take(limit.coerceAtMost(2000)).joinToString("\n"))
    }
}

class WriteTool(root: File) : RootedTool(root) {
    override val name = "write"
    override val description = "Create or overwrite a file"
    override val inputSchemaJson =
        """{"type":"object","properties":{"filePath":{"type":"string"},"content":{"type":"string"}},"required":["filePath","content"]}"""
    override val permission = "edit"
    override val requiredArgs = listOf("filePath", "content")
    override fun target(args: Map<String, String>) = args["filePath"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val f = resolve(args.getValue("filePath"))
        f.parentFile?.mkdirs()
        f.writeText(args.getValue("content"))
        return ToolResult("wrote ${f.path}")
    }
}

class EditTool(root: File) : RootedTool(root) {
    override val name = "edit"
    override val description = "Exact-string file replace"
    override val inputSchemaJson =
        """{"type":"object","properties":{"filePath":{"type":"string"},"oldString":{"type":"string"},"newString":{"type":"string"},"replaceAll":{"type":"boolean"}},"required":["filePath","oldString","newString"]}"""
    override val permission = "edit"
    override val requiredArgs = listOf("filePath", "oldString", "newString")
    override fun target(args: Map<String, String>) = args["filePath"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val f = resolve(args.getValue("filePath"))
        if (!f.isFile) return ToolResult("", error = "no such file: ${args["filePath"]}")
        val text = f.readText()
        val old = args.getValue("oldString")
        val new = args.getValue("newString")
        val replaceAll = args["replaceAll"] == "true"
        val count = text.split(old, ignoreCase = false, limit = 0).size - 1
        if (count == 0) return ToolResult("", error = "oldString not found")
        if (count > 1 && !replaceAll) return ToolResult("", error = "oldString matches $count times; set replaceAll=true")
        f.writeText(if (replaceAll) text.replace(old, new) else text.replaceFirst(old, new))
        return ToolResult("edited ${f.path} ($count replacement${if (count == 1) "" else "s"})")
    }
}

class GlobTool(root: File) : RootedTool(root) {
    override val name = "glob"
    override val description = "Filename glob search"
    override val inputSchemaJson =
        """{"type":"object","properties":{"pattern":{"type":"string"},"path":{"type":"string"}},"required":["pattern"]}"""
    override val permission = "glob"
    override val requiredArgs = listOf("pattern")
    override fun target(args: Map<String, String>) = args["pattern"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val base = args["path"]?.let { resolve(it) } ?: root
        val matcher = FileSystems.getDefault().getPathMatcher("glob:${args.getValue("pattern")}")
        val out = mutableListOf<String>()
        base.walkTopDown().forEach {
            if (out.size >= 100) return@forEach
            val rel = try {
                base.toPath().relativize(it.toPath())
            } catch (_: Exception) {
                return@forEach
            }
            if (it.isFile && (matcher.matches(rel) || matcher.matches(it.toPath().fileName))) {
                out += it.relativeTo(root).path.ifEmpty { it.name }
            }
        }
        return ToolResult(out.sorted().take(100).joinToString("\n"))
    }
}

class GrepTool(root: File) : RootedTool(root) {
    override val name = "grep"
    override val description = "Regex content search"
    override val inputSchemaJson =
        """{"type":"object","properties":{"pattern":{"type":"string"},"path":{"type":"string"},"include":{"type":"string"}},"required":["pattern"]}"""
    override val permission = "grep"
    override val requiredArgs = listOf("pattern")
    override fun target(args: Map<String, String>) = args["pattern"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val base = args["path"]?.let { resolve(it) } ?: root
        val rx = try {
            Regex(args.getValue("pattern"))
        } catch (_: Exception) {
            return ToolResult("", error = "invalid regex")
        }
        val include = args["include"]?.let { FileSystems.getDefault().getPathMatcher("glob:$it") }
        val out = mutableListOf<String>()
        base.walkTopDown().forEach {
            if (out.size >= 100 || !it.isFile || it.length() > 1_000_000) return@forEach
            if (include != null && !include.matches(it.toPath().fileName)) return@forEach
            val text = try {
                it.readText()
            } catch (_: Exception) {
                return@forEach
            }
            text.lines().forEachIndexed { i, line ->
                if (out.size < 100 && rx.containsMatchIn(line)) out += "${it.relativeTo(root).path}:${i + 1}: $line"
            }
        }
        return ToolResult(out.joinToString("\n"))
    }
}
