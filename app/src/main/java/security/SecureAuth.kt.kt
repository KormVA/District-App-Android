package com.example.district.security

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.district.models.UserProfile
import kotlinx.coroutines.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.random.Random

class SecureAuth(private val context: Context) {

    companion object {
        private const val PBKDF2_ITERATIONS = 310_000
        private const val SALT_LENGTH = 16
        private const val HASH_LENGTH = 256
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"

        // Rate limiting
        private const val BLOCK_TIME = 300_000L // 5 минут
        private const val MAX_ATTEMPTS = 5
        private const val GLOBAL_MAX_ATTEMPTS_PER_HOUR = 100
        private const val MIN_RESPONSE_TIME_MS = 150L // Минимальное время ответа для защиты от timing attacks

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

    // Coroutine scope для фоновых операций
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ========== RATE LIMITING ==========
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

    // ========== ГЛОБАЛЬНЫЙ RATE LIMITING ==========
    private fun checkGlobalRateLimit(): Boolean {
        val currentHour = System.currentTimeMillis() / 3600000 // час
        val lastHour = sharedPrefs.getLong("global_rate_hour", 0)
        val attempts = sharedPrefs.getInt("global_rate_attempts", 0)

        return if (lastHour != currentHour) {
            // Новый час - сбрасываем счетчик
            sharedPrefs.edit()
                .putLong("global_rate_hour", currentHour)
                .putInt("global_rate_attempts", 1)
                .apply()
            true
        } else {
            if (attempts < GLOBAL_MAX_ATTEMPTS_PER_HOUR) {
                sharedPrefs.edit().putInt("global_rate_attempts", attempts + 1).apply()
                true
            } else {
                false
            }
        }
    }

    private fun incrementGlobalAttempts() {
        val currentHour = System.currentTimeMillis() / 3600000
        val lastHour = sharedPrefs.getLong("global_rate_hour", 0)
        val attempts = sharedPrefs.getInt("global_rate_attempts", 0)

        if (lastHour != currentHour) {
            sharedPrefs.edit()
                .putLong("global_rate_hour", currentHour)
                .putInt("global_rate_attempts", 1)
                .apply()
        } else {
            sharedPrefs.edit().putInt("global_rate_attempts", attempts + 1).apply()
        }
    }

    // ========== ЛОГИРОВАНИЕ БЕЗОПАСНОСТИ ==========
    private fun logSecurityEvent(event: String, login: String, details: Map<String, Any> = emptyMap()) {
        scope.launch {
            val eventData = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "event" to event,
                "login" to login,
                "attempts" to getAttempts(login),
                "global_attempts" to sharedPrefs.getInt("global_rate_attempts", 0),
                "device_id" to getDeviceId()
            ) + details

            // Сохраняем в отдельный лог
            val logKey = "security_log_${System.currentTimeMillis()}_${Random.nextInt(1000)}"
            sharedPrefs.edit().putString(logKey, eventData.toString()).apply()

            // Если слишком много попыток - шлем сигнал
            if (getAttempts(login) >= MAX_ATTEMPTS * 2) {
                notifyAdmin("Brute force attack detected on $login")
            }

            // Очищаем старые логи (оставляем только 100 последних)
            cleanupOldLogs()
        }
    }

    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    private fun notifyAdmin(message: String) {
        // Здесь можно отправить push, email, telegram и т.д.
        Log.e("SECURITY_ALERT", message)
        // TODO: Реализовать отправку уведомления админу
    }

    private fun cleanupOldLogs() {
        val allKeys = sharedPrefs.all.keys
        val logKeys = allKeys.filter { it.startsWith("security_log_") }.sorted()

        if (logKeys.size > 100) {
            val keysToDelete = logKeys.take(logKeys.size - 100)
            sharedPrefs.edit().apply {
                keysToDelete.forEach { remove(it) }
            }.apply()
        }
    }

    // ========== ОСНОВНАЯ ЛОГИКА С ЗАЩИТОЙ ОТ TIMING ATTACKS ==========
    suspend fun checkPassword(login: String, password: String): Boolean = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        var result = false

        try {
            // Проверка глобального rate limiting
            if (!checkGlobalRateLimit()) {
                logSecurityEvent("GLOBAL_RATE_LIMIT_EXCEEDED", login)
                return@withContext false
            }

            val currentTime = System.currentTimeMillis()

            // ========== ПРОВЕРКА БЛОКИРОВКИ ==========
            val lockTime = getLockUntil(login)
            if (lockTime > currentTime) {
                val remaining = (lockTime - currentTime) / 1000
                logSecurityEvent("ACCOUNT_BLOCKED", login, mapOf("remaining_seconds" to remaining))
                return@withContext false
            } else if (lockTime > 0) {
                resetAttempts(login)
                logSecurityEvent("ACCOUNT_UNBLOCKED", login)
            }

            // ========== ПОЛУЧАЕМ ТЕКУЩЕЕ КОЛИЧЕСТВО ПОПЫТОК ==========
            val attempts = getAttempts(login)

            // ========== АДМИН (С ЗАЩИТОЙ) ==========
            if (login == "admin") {
                val adminHash = sharedPrefs.getString("admin_password_hash", null)
                val adminSalt = sharedPrefs.getString("admin_salt", null)

                var adminSuccess = false

                if (adminHash != null && adminSalt != null) {
                    val inputHash = hashPassword(password, adminSalt)
                    if (inputHash == adminHash) {
                        resetAttempts(login)
                        loginUser("admin", "Администратор", "ул. Ленина, 10")
                        logSecurityEvent("ADMIN_LOGIN_SUCCESS", login)
                        adminSuccess = true
                    }
                }

                if (!adminSuccess) {
                    // Неудача — увеличиваем счётчик
                    val newAttempts = attempts + 1
                    setAttempts(login, newAttempts)
                    incrementGlobalAttempts()

                    logSecurityEvent("ADMIN_LOGIN_FAILED", login, mapOf("new_attempts" to newAttempts))

                    if (newAttempts >= MAX_ATTEMPTS) {
                        setLockUntil(login, currentTime + BLOCK_TIME)
                        logSecurityEvent("ADMIN_ACCOUNT_BLOCKED", login, mapOf("attempts" to newAttempts))
                    }
                }

                result = adminSuccess
            } else {
                // ========== ОБЫЧНЫЕ ПОЛЬЗОВАТЕЛИ ==========
                val storedHash = sharedPrefs.getString("${login}_password_hash", null)
                val salt = sharedPrefs.getString("${login}_salt", null)

                if (storedHash == null || salt == null) {
                    // Пользователь не найден - но мы все равно делаем фейковую проверку!
                    val fakeSalt = generateSalt()
                    val fakeHash = hashPassword("fake_password_that_never_matches", fakeSalt)
                    hashPassword(password, fakeSalt) // Тратим время как при реальной проверке

                    incrementGlobalAttempts()
                    logSecurityEvent("USER_NOT_FOUND", login)
                    result = false
                } else {
                    val inputHash = hashPassword(password, salt)
                    val success = inputHash == storedHash

                    if (success) {
                        resetAttempts(login)
                        val displayName = sharedPrefs.getString("${login}_display_name", login)
                        val house = sharedPrefs.getString("${login}_house", "")
                        loginUser(login, displayName ?: login, house ?: "")
                        logSecurityEvent("LOGIN_SUCCESS", login)
                        result = true
                    } else {
                        val newAttempts = attempts + 1
                        setAttempts(login, newAttempts)
                        incrementGlobalAttempts()

                        logSecurityEvent("LOGIN_FAILED", login, mapOf("new_attempts" to newAttempts))

                        if (newAttempts >= MAX_ATTEMPTS) {
                            setLockUntil(login, currentTime + BLOCK_TIME)
                            logSecurityEvent("ACCOUNT_BLOCKED", login, mapOf("attempts" to newAttempts))
                        }
                        result = false
                    }
                }
            }
        } catch (e: Exception) {
            logSecurityEvent("ERROR", login, mapOf("error" to (e.message ?: "unknown")))
            result = false
        } finally {
            // ========== ЗАЩИТА ОТ TIMING ATTACKS ==========
            val elapsed = System.currentTimeMillis() - startTime
            if (elapsed < MIN_RESPONSE_TIME_MS) {
                delay(MIN_RESPONSE_TIME_MS - elapsed)
            }
        }

        return@withContext result
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

    fun cleanup() {
        scope.cancel()
    }
}