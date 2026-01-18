package com.example.district.viewmodels

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.district.models.Advert

class FavoritesViewModel : ViewModel() {
    // Храним все товары здесь
    private val _allAdverts = mutableStateListOf<Advert>()
    val allAdverts: List<Advert> get() = _allAdverts

    var showFavoritesOnly by mutableStateOf(false)
        private set

    // Инициализируем данные
    init {
        _allAdverts.addAll(getSampleAdverts())
    }

    fun toggleFavorite(advertId: Int) {
        val index = _allAdverts.indexOfFirst { it.id == advertId }
        if (index != -1) {
            _allAdverts[index] = _allAdverts[index].copy(
                isFavorite = !_allAdverts[index].isFavorite
            )
        }
    }

    fun isFavorite(advertId: Int): Boolean {
        return _allAdverts.find { it.id == advertId }?.isFavorite ?: false
    }

    fun toggleShowFavorites() {
        showFavoritesOnly = !showFavoritesOnly
    }

    // Получить товары в зависимости от фильтра
    fun getFilteredAdverts(category: String? = null): List<Advert> {
        return _allAdverts.filter { advert ->
            (category == null || category == "Все товары" || advert.category == category) &&
                    (!showFavoritesOnly || advert.isFavorite)
        }
    }
}

// Вынеси функцию сюда
private fun getSampleAdverts(): List<Advert> {
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
        )
    )
}