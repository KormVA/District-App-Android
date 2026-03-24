package com.example.district.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.district.data.remote.api.ApiService
import com.example.district.data.remote.model.ProfileUpdate

@Composable
fun ProfileEditScreen(
    apiService: ApiService,
    onBack: () -> Unit
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(apiService)
    )

    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var phone by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var phoneVisible by remember { mutableStateOf(false) }
    var telegramVisible by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        profile?.let {
            phone = it.phone ?: ""
            telegram = it.telegram ?: ""
            phoneVisible = it.phoneVisible
            telegramVisible = it.telegramVisible
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Редактировать профиль", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator()
        }

        error?.let {
            Text("Ошибка: $it", color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Телефон") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telegram,
            onValueChange = { telegram = it },
            label = { Text("Telegram (без @)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = phoneVisible,
                onCheckedChange = { phoneVisible = it }
            )
            Text("Показывать телефон другим")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = telegramVisible,
                onCheckedChange = { telegramVisible = it }
            )
            Text("Показывать Telegram другим")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.updateProfile(
                    ProfileUpdate(
                        phone = phone.ifEmpty { null },
                        telegram = telegram.ifEmpty { null },
                        phoneVisible = phoneVisible,
                        telegramVisible = telegramVisible
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Назад")
        }
    }
}