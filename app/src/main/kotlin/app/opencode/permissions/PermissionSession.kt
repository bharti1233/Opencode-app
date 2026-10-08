package app.opencode.permissions

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Port of permission/index.ts ask/reply: ALLOW passes, DENY blocks, ASK
// suspends on showDialog. ALLOW_ALWAYS appends an allow rule (like `always`).

enum class Decision { ALLOW_ONCE, ALLOW_ALWAYS, DENY }

data class PermissionRequest(val permission: String, val input: String)

class PermissionSession(val rules: MutableList<Rule>) {
    private val mutex = Mutex()

    suspend fun ask(
        permission: String,
        input: String,
        showDialog: suspend (PermissionRequest) -> Decision,
    ): Boolean = mutex.withLock {
        when (PermissionEngine.resolve(rules, permission, input)) {
            Verdict.ALLOW -> true
            Verdict.DENY -> false
            Verdict.ASK -> when (showDialog(PermissionRequest(permission, input))) {
                Decision.DENY -> false
                Decision.ALLOW_ONCE -> true
                Decision.ALLOW_ALWAYS -> {
                    rules += Rule(permission, input, Action.ALLOW)
                    true
                }
            }
        }
    }
}
