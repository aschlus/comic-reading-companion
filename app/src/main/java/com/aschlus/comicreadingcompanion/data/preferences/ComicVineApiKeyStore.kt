package com.aschlus.comicreadingcompanion.data.preferences

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "comic_vine_api_key_encryption_key"
private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_LENGTH_BYTES = 12
private const val AUTH_TAG_LENGTH_BITS = 128

private val Context.comicVineApiKeyDataStore:
        DataStore<Preferences> by preferencesDataStore(
            name = "comic_vine_api_key"
        )

private val encryptedApiKeyPreference =
    stringPreferencesKey("encrypted_api_key")

class ComicVineApiKeyStore(
    context: Context
) {
    private val dataStore =
        context.applicationContext.comicVineApiKeyDataStore

    suspend fun save(apiKey: String) {
        val normalizedKey = apiKey.trim()

        require(normalizedKey.isNotEmpty()) {
            "Comic Vine API key cannot be blank"
        }

        val encryptedValue = encrypt(normalizedKey)

        dataStore.edit { preferences ->
            preferences[encryptedApiKeyPreference] = encryptedValue
        }
    }

    suspend fun get(): String? {
        val encryptedValue =
            dataStore.data.first()[encryptedApiKeyPreference]
                ?: return null

        return decrypt(encryptedValue)
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(encryptedApiKeyPreference)
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateEncryptionKey()
        )

        val encryptedBytes =
            cipher.doFinal(
                plainText.toByteArray(Charsets.UTF_8)
            )

        // Store the randomly generated IV alongside the
        // ciphertext so it can be recovered during decryption
        val payload = cipher.iv + encryptedBytes

        return Base64.encodeToString(
            payload,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(encodedValue: String): String {
        val payload =
            Base64.decode(encodedValue, Base64.NO_WRAP)

        require(payload.size > IV_LENGTH_BYTES) {
            "Stored Comic Vine API key data is invalid"
        }

        val iv =
            payload.copyOfRange(0, IV_LENGTH_BYTES)

        val encryptedBytes =
            payload.copyOfRange(
                IV_LENGTH_BYTES,
                payload.size
            )

        val cipher =
            Cipher.getInstance(CIPHER_TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getExistingEncryptionKey(),
            GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv)
        )

        val plainBytes =
            cipher.doFinal(encryptedBytes)

        return String(plainBytes, Charsets.UTF_8)
    }

    private fun getOrCreateEncryptionKey(): SecretKey {
        val keyStore = loadKeyStore()

        val existingKey =
            keyStore.getKey(KEY_ALIAS, null) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build()
        )

        return keyGenerator.generateKey()
    }

    private fun getExistingEncryptionKey(): SecretKey {
        return loadKeyStore()
            .getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException(
                "The saved Comic Vine API key cannot be " +
                "decrypted on this device. Clear it and " +
                "enter the API key again."
            )
    }

    private fun loadKeyStore(): KeyStore {
        return KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    }
}