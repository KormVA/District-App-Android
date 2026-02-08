package com.example.district.security

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.district.models.UserProfile

class SecureAuth(private val context: Context) {

    companion object {
        // Константы для PBKDF2
        private const val PBKDF2_ITERATIONS = 310_000  // Рекомендуемое количество итераций для PBKDF2
        private const val SALT_LENGTH = 16            // Длина соли в байтах
        private const val HASH_LENGTH = 256           // Длина хэша в битах (32 байта)
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

        private fun generateSalt(): String {
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)
            return Base64.encodeToString(salt, Base64.NO_WRAP)
        }

        fun hashPassword(password: String, salt: String): String {
            try {
                // Декодируем соль из Base64
                val saltBytes = Base64.decode(salt, Base64.NO_WRAP)

                // Создаём спецификацию для PBKDF2
                val spec = PBEKeySpec(
                    password.toCharArray(),
                    saltBytes,
                    PBKDF2_ITERATIONS,
                    HASH_LENGTH
                )

                // Получаем фабрику и генерируем ключ
                val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                val hash = factory.generateSecret(spec).encoded

                // Возвращаем хэш в Base64
                return Base64.encodeToString(hash, Base64.NO_WRAP)

            } catch (e: Exception) {
                // Логируем ошибку
                Log.e("SecureAuth", "PBKDF2 failed: ${e.message}", e)
                throw RuntimeException("Password hashing failed", e)
            }
        }
    }

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

    // РЕГИСТРАЦИЯ нового пользователя
    fun registerUser(login: String, password: String, displayName: String, house: String): Boolean {
        if (password.length < 8) {
            Log.w("SecureAuth", "Password too short: ${password.length} chars")
            return false
        }

        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        // Сохраняем ВСЕ данные пользователя под его логином
        sharedPrefs.edit()
            .putString("${login}_password_hash", hash)
            .putString("${login}_salt", salt)
            .putString("${login}_display_name", displayName)
            .putString("${login}_house", house)
            .apply()

        Log.d("SecureAuth", "User registered: $login with PBKDF2 hash")

        // И сразу логиним его
        loginUser(login, displayName, house)
        return true
    }

    // ВХОД
    fun checkPassword(login: String, password: String): Boolean {
        // 1. Проверяем admin (только если настроен)
        if (login == "admin") {
            val adminHash = sharedPrefs.getString("admin_password_hash", null)
            val adminSalt = sharedPrefs.getString("admin_salt", null)

            if (adminHash != null && adminSalt != null) {
                val inputHash = hashPassword(password, adminSalt)
                if (inputHash == adminHash) {
                    loginUser("admin", "Администратор", "ул. Ленина, 10")
                    Log.d("SecureAuth", "Admin login successful with PBKDF2")
                    return true
                }
            }
            return false
        }

        // 2. Проверяем обычных пользователей
        val storedHash = sharedPrefs.getString("${login}_password_hash", null)
        val salt = sharedPrefs.getString("${login}_salt", null)

        if (storedHash == null || salt == null) {
            Log.w("SecureAuth", "User not found or missing credentials: $login")
            return false
        }

        val inputHash = hashPassword(password, salt)
        if (inputHash == storedHash) {
            // Получаем данные пользователя
            val displayName = sharedPrefs.getString("${login}_display_name", login)
            val house = sharedPrefs.getString("${login}_house", "")

            loginUser(login, displayName ?: login, house ?: "")
            Log.d("SecureAuth", "User login successful: $login with PBKDF2")
            return true
        }

        Log.w("SecureAuth", "Password mismatch for user: $login")
        return false
    }

    // Вспомогательный метод для входа пользователя
    private fun loginUser(login: String, displayName: String, house: String) {
        sharedPrefs.edit()
            .putString("user_login", login)
            .putString("user_display_name", displayName)
            .putString("user_house", house)
            .apply()
        Log.d("SecureAuth", "User logged in: $login ($displayName)")
    }

    // ПОЛУЧИТЬ текущего пользователя
    fun getCurrentUser(): UserProfile? {
        val login = sharedPrefs.getString("user_login", null) ?: return null
        val displayName = sharedPrefs.getString("user_display_name", "") ?: ""
        val house = sharedPrefs.getString("user_house", "") ?: ""

        return UserProfile(
            login = login,
            displayName = displayName,
            house = house,
            phone = ""
        )
    }

    // НОВАЯ ФУНКЦИЯ: Проверить, является ли пользователь владельцем объявления
    fun isCurrentUserOwner(advertOwnerLogin: String): Boolean {
        val currentUserLogin = sharedPrefs.getString("user_login", null)
        return currentUserLogin == advertOwnerLogin
    }

    // ВЫЙТИ
    fun logout() {
        val currentUser = sharedPrefs.getString("user_login", null)
        sharedPrefs.edit()
            .remove("user_login")
            .remove("user_display_name")
            .apply()
        Log.d("SecureAuth", "User logged out: $currentUser")
    }

    // НОВАЯ ФУНКЦИЯ: Миграция хэшей (для существующих пользователей)
    fun migrateUserToPBKDF2(login: String, oldPassword: String): Boolean {
        val oldHash = sharedPrefs.getString("${login}_password_hash", null)
        val oldSalt = sharedPrefs.getString("${login}_salt", null)

        if (oldHash == null || oldSalt == null) {
            Log.e("SecureAuth", "Cannot migrate: user data not found for $login")
            return false
        }

        // Проверяем старый пароль (SHA-256)
        val digest = MessageDigest.getInstance("SHA-256")
        val passwordWithSalt = oldPassword + oldSalt
        val calculatedOldHash = digest.digest(passwordWithSalt.toByteArray())
        val calculatedOldHashBase64 = Base64.encodeToString(calculatedOldHash, Base64.NO_WRAP)

        if (calculatedOldHashBase64 != oldHash) {
            Log.e("SecureAuth", "Cannot migrate: password incorrect for $login")
            return false
        }

        // Создаём новый хэш с PBKDF2
        val newSalt = generateSalt()
        val newHash = hashPassword(oldPassword, newSalt)

        // Обновляем данные пользователя
        sharedPrefs.edit()
            .putString("${login}_password_hash", newHash)
            .putString("${login}_salt", newSalt)
            .apply()

        Log.d("SecureAuth", "User migrated to PBKDF2: $login")
        return true
    }
}