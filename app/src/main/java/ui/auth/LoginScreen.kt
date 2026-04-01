package com.example.district.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.district.data.remote.api.ApiService
import com.example.district.data.remote.model.LoginRequest
import com.example.district.security.SecureAuth
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.net.ssl.SSLException
import android.util.Log

@Composable
fun LoginScreen(
    apiService: ApiService,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    var lockUntil by remember { mutableStateOf(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "District — Авторизация",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                errorMessage = null
            },
            label = { Text("Логин") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (showPassword) "Скрыть пароль" else "Показать пароль"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val now = System.currentTimeMillis()
                if (now < lockUntil) {
                    val remaining = (lockUntil - now) / 1000
                    errorMessage = "Слишком много попыток. Подождите ${remaining} секунд"
                    return@Button
                }

                if (username.isBlank() || password.isBlank()) {
                    errorMessage = "Заполните все поля"
                    return@Button
                }

                scope.launch {
                    isLoading = true
                    errorMessage = null

                    try {
                        val startTime = System.currentTimeMillis()

                        val response = apiService.login(LoginRequest(username, password))

                        val elapsed = System.currentTimeMillis() - startTime
                        if (elapsed < 300) {
                            kotlinx.coroutines.delay(300 - elapsed)
                        }

                        val auth = SecureAuth(context)
                        auth.saveToken(response.access_token)
                        auth.saveUser(response.user.username, response.user.display_name ?: response.user.username, response.user.address)

                        // ДОБАВЛЕНО: сохраняем информацию о пользователе
                        val user = response.user
                        auth.saveUser(user.username, user.display_name ?: user.username, user.address.toString())

                        // log.d("TOKEN_TEST", "Сохранён: ${response.access_token.take(20)}...")

                        val savedToken = auth.getToken()
                        // log.d("TOKEN_TEST", "Проверка сохранения: ${savedToken?.take(20)}...")

                        attempts = 0
                        lockUntil = 0
                        onLoginSuccess()

                    } catch (e: HttpException) {
                        attempts++
                        if (attempts >= 5) {
                            lockUntil = System.currentTimeMillis() + 300000
                        }

                        when (e.code()) {
                            401 -> errorMessage = "Неверный логин или пароль"
                            else -> errorMessage = "Ошибка сервера (${e.code()})"
                        }
                    } catch (e: SSLException) {
                        errorMessage = "Ошибка безопасности соединения"
                    } catch (e: Exception) {
                        errorMessage = "Ошибка сети. Проверьте подключение"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && System.currentTimeMillis() >= lockUntil
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Вход...")
            } else {
                Text("Войти")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onNavigateToRegister,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "Регистрация"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Создать аккаунт")
        }
    }
}