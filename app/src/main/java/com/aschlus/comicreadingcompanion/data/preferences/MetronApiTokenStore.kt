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
private const val KEY_ALIAS = "metron_api_token_encryption_key"
private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_LENGTH_BYTES = 12
private const val AUTH_TAG_LENGTH_BITS = 128

private val Context.metronApiTokenDataStore:
        DataStore<Preferences> by preferencesDataStore(
            name = "metron_api_token"
        )

private val encryptedTokenPreference =
    stringPreferencesKey("encrypted_api_token")

class MetronApiTokenStore(context: Context) {

    private val dataStore =
        context.applicationContext.metronApiTokenDataStore

    suspend fun save(token: String) {
        val normalizedToken = token.trim()

        require(normalizedToken.isNotEmpty()) {
            "Metron API token cannot be blank"
        }

        val encryptedValue = encrypt(normalizedToken)

        dataStore.edit { preferences ->
            preferences[encryptedTokenPreference] = encryptedValue
        }
    }

    suspend fun get(): String? {
        val encryptedValue =
            dataStore.data.first()[encryptedTokenPreference]
                ?: return null

        return decrypt(encryptedValue)
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(encryptedTokenPreference)
        }
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateEncryptionKey()
        )

        val encryptedBytes =
            cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val payload = cipher.iv + encryptedBytes

        return Base64.encodeToString(
            payload,
            Base64.NO_WRAP
        )
    }

    private fun decrypt(encodeValue: String): String {
        val payload = Base64.decode(encodeValue, Base64.NO_WRAP)

        require(payload.size > IV_LENGTH_BYTES) {
            "Stored Metron API token data is invalid"
        }

        val iv = payload.copyOfRange(0, IV_LENGTH_BYTES)
        val encryptedBytes =
            payload.copyOfRange(IV_LENGTH_BYTES, payload.size)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getExistingEncryptionKey(),
            GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv)
        )

        return String(
            cipher.doFinal(encryptedBytes),
            Charsets.UTF_8
        )
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
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
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
                "The saved Metron API token cannot be decrypted " +
                "on this device. Clear it and enter the token again."
            )
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
}