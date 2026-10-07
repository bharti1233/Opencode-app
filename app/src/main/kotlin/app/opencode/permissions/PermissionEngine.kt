package app.opencode.permissions

// Port of packages/opencode/src/permission/index.ts + core/src/util/wildcard.ts.
// Actions allow|ask|deny; last-match-wins over merged agent+session rulesets; default ask.

enum class Action { ALLOW, ASK, DENY }
enum class Verdict { ALLOW, ASK, DENY }

data class Rule(val permission: String, val pattern: String, val action: Action)

object Wildcard {
    fun match(input: String, pattern: String): Boolean {
        var p = pattern
        // trailing " *" optional (core wildcard.ts)
        if (p.endsWith(" *")) p = p.dropLast(2) + "*"
        val sb = StringBuilder()
        for (c in p) when (c) {
            '*' -> sb.append(".*")
            '?' -> sb.append('.')
            '.', '(', ')', '+', '|', '^', '$', '[', ']', '{', '}', '\\' -> sb.append('\\').append(c)
            else -> sb.append(c)
        }
        return Regex("^${sb}$").containsMatchIn(input.replace('\\', '/'))
    }
}

object PermissionEngine {
    fun resolve(rules: List<Rule>, permission: String, input: String): Verdict {
        val hit = rules.findLast { it.permission == permission && Wildcard.match(input, it.pattern) }
        return when (hit?.action) {
            Action.ALLOW -> Verdict.ALLOW
            Action.DENY -> Verdict.DENY
            else -> Verdict.ASK
        }
    }
}
