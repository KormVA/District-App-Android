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

    // РЕГИСТРАЦИЯ нового пользователя (ФИКСИРОВАННАЯ)
    fun registerUser(login: String, password: String, displayName: String, house: String): Boolean {
        if (password.length < 6) return false

        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        // Ключем теперь будет login_пароль, а не просто password_hash
        sharedPrefs.edit()
            .putString("${login}_password_hash", hash)  // ← ФИКС: у каждого свой пароль
            .putString("${login}_salt", salt)           // ← ФИКС: у каждого своя соль
            .putString("user_login", login)             // ← запоминаем кто сейчас вошёл
            .putString("user_display_name", displayName)
            .putString("user_house", house)
            .apply()

        return true
    }

    // ВХОД (ФИКСИРОВАННЫЙ - проверяет любого пользователя)
    fun checkPassword(login: String, password: String): Boolean {
        // 1. Если это первый запуск приложения - создаём admin
        val isFirstLaunch = sharedPrefs.getString("admin_password_hash", null) == null

        if (isFirstLaunch && login == "admin") {
            // Создаём дефолтного пользователя admin
            val newSalt = generateSalt()
            val newHash = hashPassword("admin123", newSalt)

            sharedPrefs.edit()
                .putString("admin_password_hash", newHash)  // ← пароль для admin
                .putString("admin_salt", newSalt)
                .putString("user_login", "admin")           // ← сразу логинимся как admin
                .putString("user_display_name", "Администратор")
                .putString("user_house", "ул. Ленина, 10")
                .apply()

            return password == "admin123"
        }

        // 2. Проверяем пароль для конкретного пользователя
        val storedHash = sharedPrefs.getString("${login}_password_hash", null)
        val salt = sharedPrefs.getString("${login}_salt", null)

        // Если это admin (у него особый ключ)
        if (login == "admin") {
            val adminHash = sharedPrefs.getString("admin_password_hash", null)
            val adminSalt = sharedPrefs.getString("admin_salt", null)

            if (adminHash != null && adminSalt != null) {
                val inputHash = hashPassword(password, adminSalt)
                if (inputHash == adminHash) {
                    // Записываем что сейчас вошёл admin
                    sharedPrefs.edit()
                        .putString("user_login", "admin")
                        .putString("user_display_name", "Администратор")
                        .putString("user_house", "ул. Ленина, 10")
                        .apply()
                    return true
                }
            }
            return false
        }

        // 3. Для обычных пользователей
        if (storedHash == null || salt == null) {
            return false  // пользователь не найден
        }

        val inputHash = hashPassword(password, salt)
        if (inputHash == storedHash) {
            // Записываем что этот пользователь вошёл
            val displayName = sharedPrefs.getString("${login}_display_name", login)
            val house = sharedPrefs.getString("${login}_house", "")

            sharedPrefs.edit()
                .putString("user_login", login)
                .putString("user_display_name", displayName ?: login)
                .putString("user_house", house ?: "")
                .apply()

            return true
        }

        return false
    }

    // ПОЛУЧИТЬ текущего пользователя (без изменений)
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

    // ВЫЙТИ (без изменений)
    fun logout() {
        sharedPrefs.edit()
            .remove("user_login")
            .remove("user_display_name")
            .remove("user_house")
            .apply()
    }

    // УСТАНОВИТЬ пароль (старый метод, можно удалить)
    @Deprecated("Используйте registerUser")
    fun setupPassword(password: String): Boolean {
        return registerUser("admin", password, "Администратор", "ул. Ленина, 10")
    }

    // ОЧИСТИТЬ ВСЁ (для тестов)
    fun clearAll() {
        sharedPrefs.edit().clear().apply()
    }
}