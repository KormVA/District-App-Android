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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertEditorScreen(
    advert: Advert? = null, // null = создание, не null = редактирование
    onBack: () -> Unit,
    onSave: (Advert) -> Unit,
    favoritesViewModel: FavoritesViewModel
) {
    val context = LocalContext.current
    val auth = SecureAuth(context)
    val currentUser = auth.getCurrentUser()
    val scrollState = rememberScrollState()

    // Состояния формы
    var title by remember { mutableStateOf(advert?.title ?: "") }
    var description by remember { mutableStateOf(advert?.description ?: "") }
    var price by remember { mutableStateOf(advert?.price?.replace(" ₽", "") ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            advert?.category?.let {
                Category.values().find { cat -> cat.title == it } ?: Category.OTHER
            } ?: Category.OTHER
        )
    }
    var phone by remember { mutableStateOf(advert?.phone ?: "+7 ") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isEditMode = advert != null
    val screenTitle = if (isEditMode) "Редактировать объявление" else "Создать объявление"
    val buttonText = if (isEditMode) "Сохранить изменения" else "Создать объявление"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(screenTitle, fontSize = 18.sp) },
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
                text = screenTitle,
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
                label = { Text("Название товара*") },
                placeholder = { Text("Например: iPhone 13 Pro") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.isBlank()
            )

            // Описание
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание*") },
                placeholder = { Text("Опишите состояние, характеристики...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                isError = description.isBlank()
            )

            // Цена
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Цена*") },
                placeholder = { Text("0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Text("₽", color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                isError = price.isBlank()
            )

            // Категория
            Text("Категория*", style = MaterialTheme.typography.labelMedium)
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
                label = { Text("Контактный телефон*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phone.isBlank() || phone == "+7 "
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

                    if (isEditMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ Режим редактирования",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка сохранения
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
                    if (phone.isBlank() || phone == "+7 ") {
                        errorMessage = "Введите контактный телефон"
                        return@Button
                    }

                    val userHouse = currentUser?.house ?: "Не указан"
                    val userLogin = currentUser?.login ?: ""

                    val updatedAdvert = if (isEditMode) {
                        // Редактирование существующего
                        advert!!.copy(
                            title = title,
                            description = description,
                            price = "$price ₽",
                            category = selectedCategory.title,
                            phone = phone
                        )
                    } else {
                        // Создание нового
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val currentDate = dateFormat.format(Date())

                        Advert(
                            id = (favoritesViewModel.allAdverts.maxOfOrNull { it.id } ?: 0) + 1,
                            title = title,
                            description = description,
                            price = "$price ₽",
                            category = selectedCategory.title,
                            author = currentUser?.displayName ?: "Вы",
                            phone = phone,
                            date = currentDate,
                            house = userHouse,
                            isFavorite = false,
                            ownerLogin = userLogin,
                            canEdit = true
                        )
                    }

                    // Сохраняем в ViewModel
                    if (isEditMode) {
                        favoritesViewModel.updateAdvert(updatedAdvert)
                    } else {
                        favoritesViewModel.addNewAdvert(updatedAdvert)
                    }

                    onSave(updatedAdvert)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(if (isEditMode) Icons.Default.Edit else Icons.Default.Add,
                    contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText)
            }

            // Кнопка удаления (только в режиме редактирования)
            if (isEditMode) {
                OutlinedButton(
                    onClick = {
                        advert?.let {
                            favoritesViewModel.removeAdvert(it.id)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Удалить объявление")
                }
            }

            Text(
                "* - обязательные поля",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}