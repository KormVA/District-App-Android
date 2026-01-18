package com.example.district.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.district.models.Advert
import com.example.district.models.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen() {
    var showFilter by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Показывать все товары или фильтрованные
    val adverts = if (selectedCategory == null) {
        getSampleAdverts()
    } else {
        getSampleAdverts().filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 56.dp)
    ) {
        // Заголовок и кнопки
        TopAppBar(
            title = {
                Text("District Товары", fontWeight = FontWeight.Bold)
            },
            actions = {
                // Кнопка сброса фильтра
                if (selectedCategory != null) {
                    IconButton(onClick = { selectedCategory = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Показать все")
                    }
                }

                // Кнопка фильтра
                IconButton(
                    onClick = { showFilter = !showFilter },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showFilter) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                }

                // Кнопка поиска
                IconButton(onClick = { /* Поиск */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                }
            }
        )

        // Фильтр категорий (показывается только при нажатии на иконку фильтра)
        if (showFilter) {
            FilterCategories(
                selectedCategory = selectedCategory,
                onCategorySelected = {
                    selectedCategory = it
                    showFilter = false // скрываем фильтр после выбора
                }
            )
        }

        // Показываем текущий фильтр
        if (selectedCategory != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Фильтр: $selectedCategory",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                TextButton(onClick = { selectedCategory = null }) {
                    Text("Сбросить")
                }
            }
        }

        // Список товаров
        AdvertsList(adverts = adverts)
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
                        if (category == "Все товары") {
                            onCategorySelected("")
                        } else {
                            onCategorySelected(category)
                        }
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

@Composable
fun AdvertsList(adverts: List<Advert>) {
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
                    Icons.Default.SearchOff,
                    contentDescription = "Нет товаров",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text("Товаров не найдено")
                Text(
                    text = "Попробуйте изменить фильтр",
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
                AdvertCard(advert = advert)
            }
        }
    }
}

@Composable
fun AdvertCard(advert: Advert) {
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
            // Заголовок и цена
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = advert.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
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

// Тестовые данные
fun getSampleAdverts(): List<Advert> {
    return listOf(
        Advert(
            id = 1,
            title = "iPhone 13 Pro",
            description = "Отличное состояние, батарея 98%, чехол в подарок",
            price = "65 000 ₽",
            category = "Электроника",
            author = "Алексей",
            phone = "+7 (999) 123-45-67",
            date = "17 янв"
        ),
        Advert(
            id = 2,
            title = "Диван угловой",
            description = "Новый, в упаковке, доставка возможна",
            price = "25 000 ₽",
            category = "Мебель",
            author = "Мария",
            phone = "+7 (999) 765-43-21",
            date = "16 янв"
        ),
        Advert(
            id = 3,
            title = "Кроссовки Nike",
            description = "Размер 42, носил 2 раза, как новые",
            price = "4 500 ₽",
            category = "Одежда",
            author = "Дмитрий",
            phone = "+7 (999) 111-22-33",
            date = "15 янв"
        ),
        Advert(
            id = 4,
            title = "Книга: Clean Code",
            description = "Роберт Мартин, идеальное состояние",
            price = "1 200 ₽",
            category = "Книги",
            author = "Сергей",
            phone = "+7 (999) 444-55-66",
            date = "14 янв"
        ),
        Advert(
            id = 5,
            title = "Велосипед горный",
            description = "21 скорость, тормоза дисковые, для взрослых",
            price = "15 000 ₽",
            category = "Другое",
            author = "Ольга",
            phone = "+7 (999) 777-88-99",
            date = "13 янв"
        ),
        Advert(
            id = 6,
            title = "Ноутбук ASUS",
            description = "Core i7, 16GB RAM, SSD 512GB, видеокарта RTX 3050",
            price = "85 000 ₽",
            category = "Электроника",
            author = "Иван",
            phone = "+7 (999) 555-44-33",
            date = "12 янв"
        ),
        Advert(
            id = 7,
            title = "Куртка зимняя",
            description = "Размер M, новая с биркой, теплая",
            price = "8 000 ₽",
            category = "Одежда",
            author = "Анна",
            phone = "+7 (999) 666-77-88",
            date = "11 янв"
        )
    )
}