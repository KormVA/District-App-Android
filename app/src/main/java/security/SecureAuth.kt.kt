package com.example.district.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecureAuth(private val context: Context) {

    companion object {
        // Генерация случайной "соли" для каждого пароля
        private fun generateSalt(): String {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            return Base64.encodeToString(salt, Base64.NO_WRAP)
        }

        // Хэширование пароля с солью
        fun hashPassword(password: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val passwordWithSalt = password + salt
            val hash = digest.digest(passwordWithSalt.toByteArray())
            return Base64.encodeToString(hash, Base64.NO_WRAP)
        }
    }

    // Безопасное хранилище (шифруется автоматически)
    private val sharedPrefs by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "secure_auth",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // 1. Сохранить пароль при первой установке
    fun setupPassword(password: String): Boolean {
        if (password.length < 6) return false

        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        sharedPrefs.edit()
            .putString("password_hash", hash)
            .putString("salt", salt)
            .apply()

        return true
    }

    // 2. Проверить пароль
    fun checkPassword(login: String, password: String): Boolean {
        val storedHash = sharedPrefs.getString("password_hash", null)
        val salt = sharedPrefs.getString("salt", null)


        if (storedHash == null || salt == null) {
            // Устанавливаем пароль по умолчанию
            setupPassword("admin123")
            // Теперь проверяем заново
            return checkPassword(login, password)
        }

        val inputHash = hashPassword(password, salt)
        return login == "admin" && inputHash == storedHash
    }

    // 3. Очистить сохраненный пароль
    fun clearPassword() {
        sharedPrefs.edit()
            .remove("password_hash")
            .remove("salt")
            .apply()
    }
}