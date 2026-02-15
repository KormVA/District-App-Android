package com.example.district.security

import android.content.Context
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.district.models.UserProfile

class SecureAuth(private val context: Context) {

    companion object {
        private const val PBKDF2_ITERATIONS = 310_000
        private const val SALT_LENGTH = 16
        private const val HASH_LENGTH = 256
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

        // Новые константы для rate limiting
        private const val BLOCK_TIME = 300_000L // 5 минут
        private const val MAX_ATTEMPTS = 5

        private fun generateSalt(): String {
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)
            return Base64.encodeToString(salt, Base64.NO_WRAP)
        }

        fun hashPassword(password: String, salt: String): String {
            try {
                val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
                val spec = PBEKeySpec(
                    password.toCharArray(),
                    saltBytes,
                    PBKDF2_ITERATIONS,
                    HASH_LENGTH
                )
                val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                val hash = factory.generateSecret(spec).encoded
                return Base64.encodeToString(hash, Base64.NO_WRAP)
            } catch (e: Exception) {
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

    // Новые вспомогательные функции для работы с попытками в SharedPreferences
    private fun getAttempts(login: String): Int {
        return sharedPrefs.getInt("${login}_attempts", 0)
    }

    private fun setAttempts(login: String, attempts: Int) {
        sharedPrefs.edit().putInt("${login}_attempts", attempts).apply()
    }

    private fun getLockUntil(login: String): Long {
        return sharedPrefs.getLong("${login}_lock_until", 0L)
    }

    private fun setLockUntil(login: String, lockTime: Long) {
        sharedPrefs.edit().putLong("${login}_lock_until", lockTime).apply()
    }

    private fun resetAttempts(login: String) {
        sharedPrefs.edit()
            .remove("${login}_attempts")
            .remove("${login}_lock_until")
            .apply()
    }

    fun registerUser(login: String, password: String, displayName: String, house: String): Boolean {
        if (password.length < 8) {
            Log.w("SecureAuth", "Password too short: ${password.length} chars")
            return false
        }

        val salt = generateSalt()
        val hash = hashPassword(password, salt)

        sharedPrefs.edit()
            .putString("${login}_password_hash", hash)
            .putString("${login}_salt", salt)
            .putString("${login}_display_name", displayName)
            .putString("${login}_house", house)
            .apply()

        Log.d("SecureAuth", "User registered: $login with PBKDF2 hash")
        loginUser(login, displayName, house)
        return true
    }

    fun checkPassword(login: String, password: String): Boolean {
        val currentTime = System.currentTimeMillis()

        // ========== 1. ПРОВЕРКА БЛОКИРОВКИ ==========
        val lockTime = getLockUntil(login)
        if (lockTime > currentTime) {
            val remaining = (lockTime - currentTime) / 1000
            Log.w("SecureAuth", "User $login is blocked for ${remaining} seconds")
            return false
        } else if (lockTime > 0) {
            // Время блокировки истекло — сбрасываем
            resetAttempts(login)
            Log.d("SecureAuth", "User $login unblocked")
        }

        // ========== 2. ПОЛУЧАЕМ ТЕКУЩЕЕ КОЛИЧЕСТВО ПОПЫТОК ==========
        val attempts = getAttempts(login)

        // ========== 3. АДМИН ==========
        if (login == "admin") {
            val adminHash = sharedPrefs.getString("admin_password_hash", null)
            val adminSalt = sharedPrefs.getString("admin_salt", null)

            if (adminHash != null && adminSalt != null) {
                val inputHash = hashPassword(password, adminSalt)
                if (inputHash == adminHash) {
                    // Сброс при успехе
                    resetAttempts(login)
                    loginUser("admin", "Администратор", "ул. Ленина, 10")
                    Log.d("SecureAuth", "Admin login successful")
                    return true
                }
            }

            // Неудача — увеличиваем счётчик
            val newAttempts = attempts + 1
            setAttempts(login, newAttempts)

            // Проверяем не превысили ли лимит
            if (newAttempts >= MAX_ATTEMPTS) {
                setLockUntil(login, currentTime + BLOCK_TIME)
                Log.w("SecureAuth", "User $login blocked for 5 minutes (reached $newAttempts failed attempts)")
            } else {
                Log.w("SecureAuth", "Admin login failed (attempt $newAttempts/$MAX_ATTEMPTS)")
            }
            return false
        }

        // ========== 4. ОБЫЧНЫЕ ПОЛЬЗОВАТЕЛИ ==========
        val storedHash = sharedPrefs.getString("${login}_password_hash", null)
        val salt = sharedPrefs.getString("${login}_salt", null)

        if (storedHash == null || salt == null) {
            Log.w("SecureAuth", "User not found: $login")
            return false
        }

        val inputHash = hashPassword(password, salt)
        val success = inputHash == storedHash

        if (success) {
            // Сброс при успехе
            resetAttempts(login)
            val displayName = sharedPrefs.getString("${login}_display_name", login)
            val house = sharedPrefs.getString("${login}_house", "")
            loginUser(login, displayName ?: login, house ?: "")
            Log.d("SecureAuth", "Login successful: $login")
            return true
        } else {
            // Неудача — увеличиваем счётчик
            val newAttempts = attempts + 1
            setAttempts(login, newAttempts)

            // Проверяем не превысили ли лимит
            if (newAttempts >= MAX_ATTEMPTS) {
                setLockUntil(login, currentTime + BLOCK_TIME)
                Log.w("SecureAuth", "User $login blocked for 5 minutes (reached $newAttempts failed attempts)")
            } else {
                Log.w("SecureAuth", "Login failed for $login (attempt $newAttempts/$MAX_ATTEMPTS)")
            }
            return false
        }
    }

    private fun loginUser(login: String, displayName: String, house: String) {
        sharedPrefs.edit()
            .putString("user_login", login)
            .putString("user_display_name", displayName)
            .putString("user_house", house)
            .apply()
        Log.d("SecureAuth", "User logged in: $login ($displayName)")
    }

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

    fun isCurrentUserOwner(advertOwnerLogin: String): Boolean {
        val currentUserLogin = sharedPrefs.getString("user_login", null)
        return currentUserLogin == advertOwnerLogin
    }

    fun logout() {
        val currentUser = sharedPrefs.getString("user_login", null)
        sharedPrefs.edit()
            .remove("user_login")
            .remove("user_display_name")
            .apply()
        Log.d("SecureAuth", "User logged out: $currentUser")
    }

    fun migrateUserToPBKDF2(login: String, oldPassword: String): Boolean {
        val oldHash = sharedPrefs.getString("${login}_password_hash", null)
        val oldSalt = sharedPrefs.getString("${login}_salt", null)

        if (oldHash == null || oldSalt == null) {
            Log.e("SecureAuth", "Cannot migrate: user data not found for $login")
            return false
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val passwordWithSalt = oldPassword + oldSalt
        val calculatedOldHash = digest.digest(passwordWithSalt.toByteArray())
        val calculatedOldHashBase64 = Base64.encodeToString(calculatedOldHash, Base64.NO_WRAP)

        if (calculatedOldHashBase64 != oldHash) {
            Log.e("SecureAuth", "Cannot migrate: password incorrect for $login")
            return false
        }

        val newSalt = generateSalt()
        val newHash = hashPassword(oldPassword, newSalt)

        sharedPrefs.edit()
            .putString("${login}_password_hash", newHash)
            .putString("${login}_salt", newSalt)
            .apply()

        Log.d("SecureAuth", "User migrated to PBKDF2: $login")
        return true
    }
}