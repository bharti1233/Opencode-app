package app.opencode.skill

import app.opencode.tools.AgentTool
import app.opencode.tools.ToolResult
import java.io.File

// Port of skill/discovery.ts + tool/skill.ts: SKILL.md files with `name` +
// `description` frontmatter; executing a skill loads its body into context.

data class SkillDef(val name: String, val description: String, val file: File)

fun parseSkill(file: File): SkillDef? {
    val text = try {
        file.readText()
    } catch (_: Exception) {
        return null
    }
    val front = Regex("^---\\s*\\n(.*?)\\n---\\s*\\n", RegexOption.DOT_MATCHES_ALL).find(text)
        ?: return null
    val map = front.groupValues[1].lines()
        .mapNotNull { line ->
            val i = line.indexOf(':')
            if (i < 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
        }.toMap()
    val name = map["name"] ?: return null
    return SkillDef(name, map["description"] ?: "", file)
}

fun discoverSkills(dirs: List<File>): List<SkillDef> =
    dirs.flatMap { dir ->
        dir.walkTopDown().filter { it.isFile && it.name == "SKILL.md" }.mapNotNull { parseSkill(it) }.toList()
    }.distinctBy { it.name }

class SkillTool(private val dirs: List<File>) : AgentTool {
    override val name = "skill"
    override val description = "Load a SKILL.md into context"
    override val inputSchemaJson =
        """{"type":"object","properties":{"name":{"type":"string"}},"required":["name"]}"""
    override val permission = "skill"
    override val requiredArgs = listOf("name")
    override fun target(args: Map<String, String>) = args["name"]

    override suspend fun execute(args: Map<String, String>): ToolResult {
        val skill = discoverSkills(dirs).find { it.name == args["name"] }
            ?: return ToolResult("", error = "unknown skill: ${args["name"]}")
        return ToolResult(skill.file.readText())
    }
}
