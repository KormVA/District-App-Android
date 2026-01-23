package com.example.district.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.district.models.Advert
import com.example.district.models.Category
import com.example.district.security.SecureAuth
import com.example.district.viewmodels.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAdvertScreen(
    onBack: () -> Unit,
    onCreateSuccess: () -> Unit,
    favoritesViewModel: FavoritesViewModel
) {
    val context = LocalContext.current
    val auth = SecureAuth(context)
    val currentUser = auth.getCurrentUser()
    val scrollState = rememberScrollState()

    // Состояния формы
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.OTHER) }
    var phone by remember { mutableStateOf("+7 ") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Создать объявление", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Заполните информацию о товаре",
                style = MaterialTheme.typography.titleLarge
            )

            // Ошибка
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Название
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название товара") },
                placeholder = { Text("Например: iPhone 13 Pro") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание") },
                placeholder = { Text("Опишите состояние, характеристики...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // Цена
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Цена") },
                placeholder = { Text("0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Text("₽", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            )

            // Категория
            Text("Категория", style = MaterialTheme.typography.labelMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Category.values().forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.title) }
                    )
                }
            }

            // Контактный телефон
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Контактный телефон") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            // Информация о продавце
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Информация о продавце", style = MaterialTheme.typography.labelMedium)
                    Text("Имя: ${currentUser?.displayName ?: "Вы"}")
                    Text("Дом: ${currentUser?.house ?: "Не указан"}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка создания
            Button(
                onClick = {
                    // Валидация
                    if (title.isBlank()) {
                        errorMessage = "Введите название товара"
                        return@Button
                    }
                    if (description.isBlank()) {
                        errorMessage = "Введите описание товара"
                        return@Button
                    }
                    if (price.isBlank()) {
                        errorMessage = "Введите цену"
                        return@Button
                    }

                    // Создаём объявление
                    val userHouse = currentUser?.house ?: "Не указан"
                    val newAdvert = Advert(
                        id = (favoritesViewModel.allAdverts.maxOfOrNull { it.id } ?: 0) + 1,
                        title = title,
                        description = description,
                        price = "$price ₽",
                        category = selectedCategory.title,
                        author = currentUser?.displayName ?: "Вы",
                        phone = phone,
                        date = "Только что",
                        house = userHouse,  // ← ТЕПЕРЬ НЕ ПУСТОЙ!
                        isFavorite = false
                    )

// ОТЛАДКА в лог
                    println("✅ Создано объявление: '${title}' | дом: '$userHouse'")


                    // ВАЖНО: Добавляем в ViewModel!
                    favoritesViewModel.addNewAdvert(newAdvert)

                    onCreateSuccess()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Создать объявление")
            }
        }
    }
}