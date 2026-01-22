package com.example.district.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.district.security.SecureAuth

@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val context = LocalContext.current
    val auth = SecureAuth(context)

    // ВОТ ВАЖНО: получаем реального пользователя!
    val currentUser = auth.getCurrentUser()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "👤 Профиль",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ПОКАЗЫВАЕМ РЕАЛЬНОЕ ИМЯ!
        Text(
            text = currentUser?.displayName ?: "Гость",
            style = MaterialTheme.typography.titleLarge
        )

        // ПОКАЗЫВАЕМ РЕАЛЬНЫЙ ДОМ!
        currentUser?.house?.takeIf { it.isNotBlank() }?.let { house ->
            Text(
                text = "🏠 $house",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // ПОКАЗЫВАЕМ РЕАЛЬНЫЙ ЛОГИН!
        Text(
            text = "@${currentUser?.login ?: "guest"}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { /* редактирование профиля */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Редактировать")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Редактировать профиль")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                auth.logout()
                onLogout()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Выйти")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выйти из аккаунта")
        }
    }
}