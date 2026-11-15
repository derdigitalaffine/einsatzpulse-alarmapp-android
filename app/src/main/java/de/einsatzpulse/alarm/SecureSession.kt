package de.einsatzpulse.alarm

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureSession(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ep_alarm_session", Context.MODE_PRIVATE)
    private val alias = "ep_alarm_device_auth_v1"
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build())
        return generator.generateKey()
    }
    val server: String get() = prefs.getString("server", "") ?: ""
    val token: String get() {
        val value = prefs.getString("credential", null) ?: return ""
        return try {
            val raw = Base64.decode(value, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, 12))
            String(cipher.doFinal(raw, 12, raw.size - 12), Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }
    val linked: Boolean get() = server.startsWith("https://") && token.startsWith("epna_")
    val fingerprint: String get() = AlarmIdentity.fingerprint(server,token)
    fun save(server: String, credential: String) {
        require(credential.startsWith("epna_"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val combined = cipher.iv + cipher.doFinal(credential.toByteArray(Charsets.UTF_8))
        prefs.edit().putString("server", normalizeServerAddress(server))
            .putString("credential", Base64.encodeToString(combined, Base64.NO_WRAP)).apply()
    }
    fun clear() { prefs.edit().clear().apply() }
}
