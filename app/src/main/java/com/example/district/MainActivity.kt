package com.example.district
// Project reopened from GitHub - все работает!(2)
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.district.ui.theme.DistrictTheme
import com.example.district.ui.auth.SecureLoginScreen

// Модель данных для объявления (пока заглушка)
data class Advert(
    val id: Int,
    val title: String,
    val description: String,
    val author: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DistrictTheme {
                // 🔐 Состояние: вошёл ли пользователь
                var isLoggedIn by remember { mutableStateOf(false) }

                if (isLoggedIn) {
                    // ✅ ЕСЛИ ВОШЁЛ: показываем твой старый интерфейс
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MainScreen(
                            onLogout = {
                                isLoggedIn = false  // ← КНОПКА ВЫХОДА
                            }
                        )
                    }
                } else {
                    // 🔐 ЕСЛИ НЕ ВОШЁЛ: показываем экран входа
                    SecureLoginScreen(
                        onLoginSuccess = {
                            // При успешном входе меняем состояние
                            isLoggedIn = true
                        }
                    )
                }
            }
        }
    }
}

// Главный экран с навигацией
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {  // ← ДОБАВИЛИ ПАРАМЕТР ДЛЯ ВЫХОДА
    // Состояние для текущей вкладки
    var selectedTab by remember { mutableStateOf(0) }

    // Заглушка для списка объявлений
    val adverts = remember {
        listOf(
            Advert(1, "Продаю велосипед", "Хороший велосипед, новый", "Сосед №1"),
            Advert(2, "Продам шуруповерт", "1500 рублей - новый!", "Сосед №2"),
            Advert(3, "Отдам котят", "2 месяца, приучены к лотку", "Сосед №3"),
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("District") },  // ← ДОБАВИЛИ НАЗВАНИЕ
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Объявления") },
                    label = { Text("Объявления") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = "Сообщения") },
                    label = { Text("Сообщения") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Профиль") },
                    label = { Text("Профиль") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        // Содержимое в зависимости от выбранной вкладки
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> AdvertsScreen(adverts = adverts)
                1 -> MessagesScreen()
                2 -> ProfileScreen(onLogout = onLogout)  // ← ПЕРЕДАЛИ ФУНКЦИЮ ВЫХОДА
            }
        }
    }
}

// Экран с объявлениями
@Composable
fun AdvertsScreen(adverts: List<Advert>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Свежие объявления",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Список объявлений
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(adverts) { advert ->
                AdvertCard(advert = advert)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка добавления (пока заглушка)
        Button(
            onClick = { /* TODO: открыть форму добавления */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Добавить объявление")
        }
    }
}

// Карточка объявления
@Composable
fun AdvertCard(advert: Advert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = advert.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = advert.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "От: ${advert.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

// Экран сообщений (заглушка)
@Composable
fun MessagesScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Здесь будут личные сообщения", style = MaterialTheme.typography.bodyLarge)
    }
}

// Экран профиля С КНОПКОЙ ВЫХОДА
@Composable
fun ProfileScreen(onLogout: () -> Unit) {  // ← ПРИНИМАЕМ ФУНКЦИЮ ВЫХОДА
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "👤 Профиль",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Добро пожаловать в District!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // КНОПКА ВЫХОДА
        Button(
            onClick = onLogout,  // ← ВЫЗЫВАЕМ ФУНКЦИЮ ВЫХОДА
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Person, contentDescription = "Выйти")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выйти из аккаунта")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Информация о security демо
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔐 Security демо:",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Базовая аутентификация\n• Безопасный выход\n• Управление сессией",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    DistrictTheme {
        MainScreen(onLogout = {})  // ← ПЕРЕДАЕМ ПУСТУЮ ФУНКЦИЮ ДЛЯ ПРЕВЬЮ
    }
}