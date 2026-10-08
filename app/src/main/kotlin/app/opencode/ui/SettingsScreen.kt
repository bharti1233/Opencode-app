package app.opencode.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.opencode.model.ProviderCatalog
import app.opencode.model.SecureKeys

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var cfg by remember { mutableStateOf(loadConfig(ctx)) }
    var key by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    Column(Modifier.padding(16.dp)) {
        Text("Settings")
        TextField(cfg.provider, { cfg = cfg.copy(provider = it) }, label = { Text("provider") })
        TextField(cfg.model, { cfg = cfg.copy(model = it) }, label = { Text("model") })
        Text("known providers: ${ProviderCatalog.bundled.joinToString()}")
        TextField(
            key, { key = it }, label = { Text("api key") },
            visualTransformation = PasswordVisualTransformation(),
        )
        Button(onClick = {
            saveConfig(ctx, cfg)
            if (key.isNotBlank()) {
                SecureKeys(ctx).set(cfg.provider, key)
                key = ""
            }
            status = "saved"
        }) { Text("Save") }
        Text(status)
    }
}
