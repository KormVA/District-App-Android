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
import com.example.district.data.remote.api.ApiService
import com.example.district.data.remote.model.CreateAdRequest
import retrofit2.HttpException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvertEditorScreen(
    advert: Advert? = null,
    apiService: ApiService,
    onBack: () -> Unit,
    onSave: (Advert) -> Unit,
    favoritesViewModel: FavoritesViewModel
) {
    val context = LocalContext.current
    val auth = SecureAuth(context)
    val currentUser = auth.getCurrentUser()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

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

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название товара*") },
                placeholder = { Text("Например: iPhone 13 Pro") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.isBlank()
            )

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

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Контактный телефон*") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = phone.isBlank() || phone == "+7 "
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Информация о продавце", style = MaterialTheme.typography.labelMedium)
                    Text("Имя: ${currentUser?.displayName ?: "Вы"}")
                    Text("Адрес: ${currentUser?.address ?: "Не указан"}")

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

            Button(
                onClick = {
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

                    val userAddress = currentUser?.address ?: ""
                    val userLogin = currentUser?.login ?: ""

                    scope.launch {
                        isLoading = true
                        errorMessage = null

                        try {
                            val response = apiService.createAd(
                                CreateAdRequest(
                                    title = title,
                                    description = description,
                                    price = price.toDoubleOrNull() ?: 0.0
                                )
                            )

                            val newAdvert = Advert(
                                id = response.id,
                                title = response.title,
                                description = response.description,
                                price = response.price.toString(),
                                category = selectedCategory.title,
                                author = currentUser?.displayName ?: "Вы",
                                phone = phone,
                                date = response.created_at.take(10),
                                imageUrl = "",
                                isFavorite = false,
                                address = response.address,
                                ownerLogin = userLogin,
                                canEdit = true,
                                telegram = null,
                                phoneVisible = true,
                                telegramVisible = false
                            )

                            if (isEditMode) {
                                favoritesViewModel.updateAdvert(newAdvert)
                            } else {
                                favoritesViewModel.addNewAdvert(newAdvert)
                            }

                            onSave(newAdvert)

                        } catch (e: HttpException) {
                            when (e.code()) {
                                401 -> errorMessage = "Не авторизован"
                                400 -> errorMessage = "Неверные данные"
                                else -> errorMessage = "Ошибка сервера (${e.code()})"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Ошибка сети: ${e.message}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Icon(if (isEditMode) Icons.Default.Edit else Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(buttonText)
            }

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
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}