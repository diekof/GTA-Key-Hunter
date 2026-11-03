package com.diekof.gtakeyhunter
import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("hunter", Context.MODE_PRIVATE)
    var channel: String
        get() = prefs.getString("channel", "") ?: ""
        set(value) { prefs.edit().putString("channel", value.trim()).apply() }
    var lastEvent: String
        get() = prefs.getString("event", "Aguardando") ?: "Aguardando"
        set(value) { prefs.edit().putString("event", value.take(160)).apply() }
    private fun key(): SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey("hunter_aes", null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder("hunter_aes", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generator.generateKey()
    }
    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
    }
    private fun decrypt(value: String): String = try {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0,12)))
        String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)))
    } catch (_: Exception) { "" }
    fun add(codes: List<String>, source: String): Int {
        val arr = JSONArray(prefs.getString("history", "[]"))
        var count = 0
        codes.forEach { code ->
            val hash = MessageDigest.getInstance("SHA-256").digest(code.toByteArray()).joinToString("") { "%02x".format(it) }
            if ((0 until arr.length()).none { arr.getJSONObject(it).optString("hash") == hash }) {
                arr.put(JSONObject().put("hash",hash).put("secret",encrypt(code)).put("source",source.take(80)).put("time",System.currentTimeMillis()))
                count++
            }
        }
        prefs.edit().putString("history",arr.toString()).apply()
        return count
    }
    fun recent(): List<String> {
        val arr = JSONArray(prefs.getString("history","[]"))
        return (arr.length()-1 downTo maxOf(0,arr.length()-20)).mapNotNull { i ->
            val obj=arr.getJSONObject(i); val code=decrypt(obj.optString("secret"))
            if (code.length==12) "****-****-${code.takeLast(4)} • ${obj.optString("source")}" else null
        }
    }
    fun clear() { prefs.edit().remove("history").apply() }
}