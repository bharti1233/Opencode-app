package app.opencode.model

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// API keys at rest: AES-256-GCM, key in Android Keystore, ciphertext in
// private prefs. Never logged; only read to build auth headers.

class SecureKeys(
    context: Context,
    private val prefs: SharedPreferences =
        context.getSharedPreferences("opencode_keys", Context.MODE_PRIVATE),
) {
    private val appContext = context.applicationContext

    fun get(providerId: String): String? {
        val raw = prefs.getString("key:$providerId", null) ?: return null
        return try {
            decrypt(loadOrCreateKey(), Base64.decode(raw, Base64.NO_WRAP)).decodeToString()
        } catch (_: Exception) {
            null
        }
    }

    fun set(providerId: String, key: String) {
        val blob = Base64.encodeToString(encrypt(loadOrCreateKey(), key.toByteArray()), Base64.NO_WRAP)
        prefs.edit().putString("key:$providerId", blob).apply()
    }

    fun clear(providerId: String) {
        prefs.edit().remove("key:$providerId").apply()
    }

    private fun loadOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return gen.generateKey()
    }

    companion object {
        private const val ALIAS = "opencode_api_keys"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128

        // Pure crypto (JVM-testable with a software key); IV prepended to ciphertext.
        fun encrypt(key: SecretKey, plain: ByteArray): ByteArray {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.ENCRYPT_MODE, key)
            return c.iv + c.doFinal(plain)
        }

        fun decrypt(key: SecretKey, blob: ByteArray): ByteArray {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, blob, 0, IV_BYTES))
            return c.doFinal(blob, IV_BYTES, blob.size - IV_BYTES)
        }
    }
}
