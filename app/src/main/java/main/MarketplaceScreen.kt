package com.example.district.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.district.models.Advert
import com.example.district.models.Category
import com.example.district.security.SecureAuth
import com.example.district.viewmodels.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen() {
    val favoritesViewModel: FavoritesViewModel = viewModel()
    val context = LocalContext.current
    val auth = SecureAuth(context)
    var showFilter by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Получаем текущего пользователя и его дом
    val currentUser = auth.getCurrentUser()
    val currentUserHouse = currentUser?.house ?: ""

    // Получаем ВСЕ товары
    val allAdverts = favoritesViewModel.allAdverts

    // Фильтруем: сначала по дому, потом по категориям/избранному
    val filteredAdverts = allAdverts.filter { advert ->
        // 1. Фильтр по дому (самый важный!)
        advert.house == currentUserHouse &&
                // 2. Фильтр по категории (если выбрана)
                (selectedCategory == null ||
                        selectedCategory == "Все товары" ||
                        advert.category == selectedCategory) &&
                // 3. Фильтр по избранному (если включён)
                (!favoritesViewModel.showFavoritesOnly || advert.isFavorite)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp)
    ) {
        // Заголовок и кнопки
        TopAppBar(
            title = {
                // Показываем дом пользователя в заголовке
                Text(
                    text = if (favoritesViewModel.showFavoritesOnly)
                        "⭐ Избранное в ${currentUserHouse.takeIf { it.isNotBlank() } ?: "вашем доме"}"
                    else if (currentUserHouse.isNotBlank())
                        "District • $currentUserHouse"
                    else
                        "District Товары",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            },
            actions = {
                // Кнопка избранного с бейджем
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                ) {
                    IconButton(
                        onClick = { favoritesViewModel.toggleShowFavorites() }
                    ) {
                        Icon(
                            if (favoritesViewModel.showFavoritesOnly) Icons.Filled.Favorite
                            else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Избранное",
                            tint = if (favoritesViewModel.showFavoritesOnly)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Бейдж количества избранных
                    val favoritesCount = favoritesViewModel.allAdverts.count { it.isFavorite }
                    if (favoritesCount > 0) {
                        Badge(
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(favoritesCount.toString())
                        }
                    }
                }

                // Кнопка фильтра
                IconButton(
                    onClick = { showFilter = !showFilter }
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                }
            }
        )

        // Фильтр категорий
        if (showFilter) {
            FilterCategories(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = if (it == "Все товары") null else it
                    showFilter = false
                }
            )
        }

        // Информация о текущем фильтре
        if (currentUserHouse.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🏠 $currentUserHouse",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${filteredAdverts.size} объявлений",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Список товаров
        if (currentUser == null) {
            // Пользователь не авторизован
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = "Не авторизован",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text("Войдите, чтобы видеть объявления")
                }
            }
        } else if (filteredAdverts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        if (favoritesViewModel.showFavoritesOnly) Icons.Outlined.FavoriteBorder
                        else Icons.Default.Home,
                        contentDescription = "Нет товаров",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (favoritesViewModel.showFavoritesOnly)
                            "Нет избранных товаров в вашем доме"
                        else "В вашем доме пока нет объявлений",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (favoritesViewModel.showFavoritesOnly)
                            "Добавляйте товары в избранное ❤️"
                        else "Будьте первым, кто разместит объявление!",
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(filteredAdverts) { advert ->
                    AdvertCard(
                        advert = advert,
                        onFavoriteClick = {
                            favoritesViewModel.toggleFavorite(advert.id)
                        },
                        isFavorite = favoritesViewModel.isFavorite(advert.id)
                    )
                }
            }
        }
    }
}

@Composable
fun AdvertCard(
    advert: Advert,
    onFavoriteClick: () -> Unit,
    isFavorite: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Открыть детали */ },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Первая строка: Избранное + Заголовок + Цена
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Кнопка избранного
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite
                        else Icons.Outlined.FavoriteBorder,
                        contentDescription = "В избранное",
                        tint = if (isFavorite)
                            MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.outline
                    )
                }

                // Заголовок
                Text(
                    text = advert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Цена
                Text(
                    text = advert.price,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Категория (теперь показываем как тег)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = advert.category,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Описание
            Text(
                text = advert.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Подвал: автор и дата
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👤 ${advert.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = advert.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { /* Позвонить */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Позвонить")
                }

                OutlinedButton(
                    onClick = { /* Написать */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Написать")
                }
            }
        }
    }
}

@Composable
fun FilterCategories(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Выберите категорию:",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Все категории + "Все товары"
            val categories = listOf("Все товары") + Category.values().map { it.title }

            categories.forEach { category ->
                CategoryFilterItem(
                    title = category,
                    isSelected = selectedCategory == category ||
                            (selectedCategory == null && category == "Все товары"),
                    onClick = {
                        onCategorySelected(category)
                    }
                )
            }
        }
    }
}

@Composable
fun CategoryFilterItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp
        )

        if (isSelected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Выбрано",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 4.dp))
}