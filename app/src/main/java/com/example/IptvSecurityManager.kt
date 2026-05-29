package com.example

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object IptvSecurityManager {
    private const val TAG = "CineFoldSecurity"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "CineFoldSecureCryptoKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (entry != null) {
            return entry.secretKey
        }

        Log.d(TAG, "Generating new secure cryptographic key in Android Keystore")
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plain text string using AES-GCM and prepends the unique IV (Initialization Vector)
     * before returning the final Base64-encoded string.
     */
    fun encrypt(plainText: String): String? {
        if (plainText.isEmpty()) return ""
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv ?: throw IllegalStateException("Cipher did not generate IV")
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Format: Byte containing IV length, followed by original IV, then ciphertext bytes
            val combined = ByteArray(1 + iv.size + cipherBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(cipherBytes, 0, combined, 1 + iv.size, cipherBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Secure encryption failed safely", e)
            null
        }
    }

    /**
     * Extracts IV and decrypts AES-GCM ciphertext from Base64 string.
     */
    fun decrypt(encryptedBase64: String): String? {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            // Quick check if input is plain JSON rather than a Base64 encrypted block
            val trimmed = encryptedBase64.trim()
            if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
                return null // Let caller parse legacy as-is
            }

            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < 2) return null
            
            val ivSize = combined[0].toInt()
            if (ivSize <= 0 || ivSize > 32 || combined.size < 1 + ivSize) return null
            
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)
            
            val cipherTextSize = combined.size - 1 - ivSize
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, 1 + ivSize, cipherText, 0, cipherTextSize)
            
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Secure decryption failed (or invalid format)", e)
            null
        }
    }
}
