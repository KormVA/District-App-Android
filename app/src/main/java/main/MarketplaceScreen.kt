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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.district.models.Advert
import com.example.district.models.Category
import com.example.district.viewmodels.FavoritesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen() {
    val favoritesViewModel: FavoritesViewModel = viewModel()
    var showFilter by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Получаем товары из ViewModel
    val adverts = favoritesViewModel.getFilteredAdverts(selectedCategory)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp)
    ) {
        // Заголовок и кнопки
        TopAppBar(
            title = {
                Text(
                    text = if (favoritesViewModel.showFavoritesOnly) "⭐ Избранное"
                    else "District Товары",
                    fontWeight = FontWeight.Bold
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

        // Список товаров
        if (adverts.isEmpty()) {
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
                        else Icons.Default.SearchOff,
                        contentDescription = "Нет товаров",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (favoritesViewModel.showFavoritesOnly)
                            "Нет избранных товаров"
                        else "Товаров не найдено",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (favoritesViewModel.showFavoritesOnly)
                            "Нажимайте ❤️ на товарах чтобы добавить их сюда"
                        else "Попробуйте изменить фильтры",
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
                items(adverts) { advert ->
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

    Divider(modifier = Modifier.padding(vertical = 4.dp))
}