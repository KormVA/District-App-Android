package com.example.district.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.district.models.Advert
import kotlinx.coroutines.launch
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class FavoritesViewModel(private val context: Context) : ViewModel() {

    // Зашифрованное хранилище для избранного
    private val sharedPrefs by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "district_favorites",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Все товары
    private val _allAdverts = mutableStateListOf<Advert>()
    val allAdverts: List<Advert> get() = _allAdverts

    // Показывать только избранное?
    var showFavoritesOnly by mutableStateOf(false)
        private set

    // Загруженные избранные ID
    private val favoriteIds = mutableSetOf<Int>()

    // Инициализируем данные
    init {
        loadFavoritesFromStorage()  // сначала загружаем сохранённые
        loadSampleAdverts()         // потом загружаем объявления
    }

    // Загружаем избранное из хранилища
    private fun loadFavoritesFromStorage() {
        viewModelScope.launch {
            val savedIds = sharedPrefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
            favoriteIds.clear()
            savedIds.forEach {
                favoriteIds.add(it.toInt())
            }
        }
    }

    // Сохраняем избранное в хранилище
    private fun saveFavoritesToStorage() {
        viewModelScope.launch {
            val stringSet = favoriteIds.map { it.toString() }.toSet()
            sharedPrefs.edit()
                .putStringSet("favorite_ids", stringSet)
                .apply()
        }
    }

    // Загружаем примерные объявления
    private fun loadSampleAdverts() {
        val adverts = listOf(
            Advert(
                id = 1,
                title = "iPhone 13 Pro",
                description = "Отличное состояние, батарея 98%, чехол в подарок",
                price = "65 000 ₽",
                category = "Электроника",
                author = "Алексей",
                phone = "+7 (999) 123-45-67",
                date = "17 янв",
                isFavorite = favoriteIds.contains(1),  // ← восстанавливаем состояние!
                house = "ул. Ленина, 10"
            ),
            Advert(
                id = 2,
                title = "Диван угловой",
                description = "Новый, в упаковке, доставка возможна",
                price = "25 000 ₽",
                category = "Мебель",
                author = "Мария",
                phone = "+7 (999) 765-43-21",
                date = "16 янв",
                isFavorite = favoriteIds.contains(2),
                house = "ул. Ленина, 10"
            ),
            Advert(
                id = 3,
                title = "Кроссовки Nike",
                description = "Размер 42, носил 2 раза, как новые",
                price = "4 500 ₽",
                category = "Одежда",
                author = "Дмитрий",
                phone = "+7 (999) 111-22-33",
                date = "15 янв",
                isFavorite = favoriteIds.contains(3),
                house = "ул. Ленина, 12"
            ),
            Advert(
                id = 4,
                title = "Книга: Clean Code",
                description = "Роберт Мартин, идеальное состояние",
                price = "1 200 ₽",
                category = "Книги",
                author = "Сергей",
                phone = "+7 (999) 444-55-66",
                date = "14 янв",
                isFavorite = favoriteIds.contains(4),
                house = "ул. Ленина, 12"
            ),
            Advert(
                id = 5,
                title = "Велосипед горный",
                description = "21 скорость, тормоза дисковые, для взрослых",
                price = "15 000 ₽",
                category = "Другое",
                author = "Ольга",
                phone = "+7 (999) 777-88-99",
                date = "13 янв",
                isFavorite = favoriteIds.contains(5),
                house = "ул. Ленина, 10"
            )
        )

        _allAdverts.clear()
        _allAdverts.addAll(adverts)
    }

    // Переключить избранное + СОХРАНИТЬ
    fun toggleFavorite(advertId: Int) {
        val index = _allAdverts.indexOfFirst { it.id == advertId }
        if (index != -1) {
            val wasFavorite = _allAdverts[index].isFavorite
            _allAdverts[index] = _allAdverts[index].copy(
                isFavorite = !wasFavorite
            )

            // Сохраняем в хранилище
            if (wasFavorite) {
                favoriteIds.remove(advertId)
            } else {
                favoriteIds.add(advertId)
            }
            saveFavoritesToStorage()
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

    // Для тестов: очистить все избранное
    fun clearAllFavorites() {
        favoriteIds.clear()
        saveFavoritesToStorage()
        // Обновляем UI
        for (i in _allAdverts.indices) {
            _allAdverts[i] = _allAdverts[i].copy(isFavorite = false)
        }
    }
}