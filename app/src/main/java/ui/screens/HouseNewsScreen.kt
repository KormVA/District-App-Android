package com.example.district.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.district.models.HouseNews

@Composable
fun HouseNewsScreen() {
    val sampleNews = remember {
        listOf(
            HouseNews(
                id = 1,
                title = "Отключение воды",
                content = "25 января с 10:00 до 16:00",
                date = "24 янв",
                isUrgent = true,
                author = "УК"
            ),
            HouseNews(
                id = 2,
                title = "Собрание жильцов",
                content = "Обсуждение благоустройства двора",
                date = "23 янв",
                isUrgent = false,
                author = "Старшая по дому"
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleNews) { newsItem ->
                NewsCard(news = newsItem)
            }
        }

        Text(
            text = "Экран новостей дома\n(в разработке)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun NewsCard(news: HouseNews) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (news.isUrgent) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = news.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = news.content,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${news.date} • ${news.author}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}