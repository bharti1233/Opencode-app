package app.opencode.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.opencode.permissions.Decision
import app.opencode.permissions.PermissionRequest

// Port of the permission.asked UI. No hard-coded security decisions — the
// verdict comes from PermissionEngine via PermissionSession; this only renders it.

@Composable
fun PermissionDialog(request: PermissionRequest, onDecision: (Decision) -> Unit) {
    AlertDialog(
        onDismissRequest = { onDecision(Decision.DENY) },
        title = { Text("Agent wants: ${request.permission}") },
        text = { Text(request.input) },
        confirmButton = {
            Row {
                TextButton(onClick = { onDecision(Decision.DENY) }) { Text("Deny") }
                TextButton(onClick = { onDecision(Decision.ALLOW_ONCE) }) { Text("Allow") }
                TextButton(onClick = { onDecision(Decision.ALLOW_ALWAYS) }) { Text("Always") }
            }
        },
    )
}
