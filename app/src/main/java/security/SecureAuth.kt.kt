package com.example.district.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.district.models.UserProfile

class SecureAuth(private val context: Context) {

    companion object {
        private fun generateSalt(): String {
            val salt = ByteArray(16)
            SecureRandom().nextBytes(salt)
            return Base64.encodeToString(salt, Base64.NO_WRAP)
        }

        fun hashPassword(password: String, salt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val passwordWithSalt = password + salt
            val hash = digest.digest(passwordWithSalt.toByteArray())
            return Base64.encodeToString(hash, Base64.NO_WRAP)
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
        if (password.length < 6) return false

        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        // Сохраняем ВСЕ данные пользователя под его логином
        sharedPrefs.edit()
            .putString("${login}_password_hash", hash)
            .putString("${login}_salt", salt)
            .putString("${login}_display_name", displayName)
            .putString("${login}_house", house)
            .apply()

        // И сразу логиним его
        loginUser(login, displayName, house)
        return true
    }

    // ВХОД
    fun checkPassword(login: String, password: String): Boolean {
        // 1. Если это первый запуск приложения - создаём admin
        val isFirstLaunch = sharedPrefs.getString("admin_password_hash", null) == null

        if (isFirstLaunch && login == "admin") {
            val newSalt = generateSalt()
            val newHash = hashPassword("admin123", newSalt)

            sharedPrefs.edit()
                .putString("admin_password_hash", newHash)
                .putString("admin_salt", newSalt)
                .apply()

            loginUser("admin", "Администратор", "ул. Ленина, 10")
            return password == "admin123"
        }

        // 2. Проверяем admin
        if (login == "admin") {
            val adminHash = sharedPrefs.getString("admin_password_hash", null)
            val adminSalt = sharedPrefs.getString("admin_salt", null)

            if (adminHash != null && adminSalt != null) {
                val inputHash = hashPassword(password, adminSalt)
                if (inputHash == adminHash) {
                    loginUser("admin", "Администратор", "ул. Ленина, 10")
                    return true
                }
            }
            return false
        }

        // 3. Проверяем обычных пользователей
        val storedHash = sharedPrefs.getString("${login}_password_hash", null)
        val salt = sharedPrefs.getString("${login}_salt", null)

        if (storedHash == null || salt == null) {
            return false
        }

        val inputHash = hashPassword(password, salt)
        if (inputHash == storedHash) {
            // Получаем данные пользователя
            val displayName = sharedPrefs.getString("${login}_display_name", login)
            val house = sharedPrefs.getString("${login}_house", "")

            loginUser(login, displayName ?: login, house ?: "")
            return true
        }

        return false
    }

    // Вспомогательный метод для входа пользователя
    private fun loginUser(login: String, displayName: String, house: String) {
        sharedPrefs.edit()
            .putString("user_login", login)
            .putString("user_display_name", displayName)
            .putString("user_house", house)
            .apply()
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
        sharedPrefs.edit()
            .remove("user_login")
            .remove("user_display_name")
            .apply()
    }
    
}