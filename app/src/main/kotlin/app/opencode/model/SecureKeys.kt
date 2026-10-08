package app.opencode.model

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

// API keys at rest. Never logged; only read to build auth headers.

class SecureKeys(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "opencode_keys",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun get(providerId: String): String? = prefs.getString("key:$providerId", null)

    fun set(providerId: String, key: String) {
        prefs.edit().putString("key:$providerId", key).apply()
    }

    fun clear(providerId: String) {
        prefs.edit().remove("key:$providerId").apply()
    }
}
