package com.example.myduka

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

object KeyStoreHelper {
    private const val KEY_ALIAS = "PIN_SALT_KEY"

    fun getOrCreateSecretKey(): SecretKey {
        val ks = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        ks.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        return KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            .apply { init(spec) }
            .generateKey()
    }

    fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        return cipher.iv to cipher.doFinal(data)
    }

    fun decrypt(iv: ByteArray, ciphertext: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(128, iv)
            )
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw IllegalStateException("Key unavailable", e)
        }
    }
}

object PinManager {
    private const val KEY_SALT_IV   = "pin_salt_iv"
    private const val KEY_SALT_DATA = "pin_salt_data"
    private const val KEY_HASH      = "pin_hash"
    private const val ITERATIONS    = 50_000
    private const val KEY_LENGTH    = 256

    private fun getPrefs(context: Context): SharedPreferences {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Must be signed in to access PIN prefs")
        val uid     = user.uid
        val name    = "admin_prefs_$uid"                  // admin tag
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    fun hasPin(context: Context): Boolean {
        return getPrefs(context).contains(KEY_HASH)
    }

    @SuppressLint("ApplySharedPref")
    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val (iv, ciphertext) = KeyStoreHelper.encrypt(salt)
        val hash = pbkdf2(pin, salt)

        getPrefs(context).edit()
            .putString(KEY_SALT_IV,   Base64.encodeToString(iv, Base64.NO_WRAP))
            .putString(KEY_SALT_DATA, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .putString(KEY_HASH,      Base64.encodeToString(hash, Base64.NO_WRAP))
            .apply()
    }

    fun checkPin(context: Context, attempt: String): Boolean {
        val prefs = getPrefs(context)
        return try {
            val ivB64   = prefs.getString(KEY_SALT_IV,   null) ?: return false
            val dataB64 = prefs.getString(KEY_SALT_DATA, null) ?: return false
            val hashB64 = prefs.getString(KEY_HASH,      null) ?: return false

            val iv         = Base64.decode(ivB64,   Base64.NO_WRAP)
            val ciphertext = Base64.decode(dataB64, Base64.NO_WRAP)
            val salt       = KeyStoreHelper.decrypt(iv, ciphertext)
            val expected   = Base64.decode(hashB64, Base64.NO_WRAP)
            val attemptHash = pbkdf2(attempt, salt)

            MessageDigest.isEqual(expected, attemptHash)
        } catch (e: Exception) {
            getPrefs(context).edit().clear().apply()
            false
        }
    }

    private fun pbkdf2(pass: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pass.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
    }
}
