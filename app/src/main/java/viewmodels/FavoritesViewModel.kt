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

    // Все товары - ОБНОВЛЯЕМОЕ СОСТОЯНИЕ
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
                try {
                    favoriteIds.add(it.toInt())
                } catch (e: NumberFormatException) {
                    // Пропускаем некорректные ID
                }
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

    // Загружаем примерные объявления - ОБНОВЛЕНО с ownerLogin
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
                isFavorite = favoriteIds.contains(1),
                house = "ул. Ленина, 10",
                ownerLogin = "alex", // ← ДОБАВЛЕНО
                canEdit = false
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
                house = "ул. Ленина, 10",
                ownerLogin = "maria", // ← ДОБАВЛЕНО
                canEdit = false
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
                house = "ул. Ленина, 12",
                ownerLogin = "dmitry", // ← ДОБАВЛЕНО
                canEdit = false
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
                house = "ул. Ленина, 12",
                ownerLogin = "sergey", // ← ДОБАВЛЕНО
                canEdit = false
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
                house = "ул. Ленина, 10",
                ownerLogin = "olga", // ← ДОБАВЛЕНО
                canEdit = false
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

    // ДОБАВЛЯЕМ НОВОЕ ОБЪЯВЛЕНИЕ - ИСПРАВЛЕННЫЙ МЕТОД
    fun addNewAdvert(advert: Advert) {
        // ДЕЛАЕМ КОПИЮ и добавляем в начало
        val newAdvert = advert.copy()

        // Логирование для отладки
        println("🟢 FavoritesViewModel.addNewAdvert() вызван!")
        println("   Новое объявление: ${newAdvert.title}")
        println("   Дом: '${newAdvert.house}'")
        println("   До добавления: ${_allAdverts.size} объявлений")

        // Добавляем в НАЧАЛО списка
        _allAdverts.add(0, newAdvert)

        println("   После добавления: ${_allAdverts.size} объявлений")
        println("   Теперь первое: ${_allAdverts.firstOrNull()?.title}")
        println("   Все ID: ${_allAdverts.map { it.id }}")
    }

    // НОВАЯ ФУНКЦИЯ: Обновить объявление
    fun updateAdvert(updatedAdvert: Advert) {
        val index = _allAdverts.indexOfFirst { it.id == updatedAdvert.id }
        if (index != -1) {
            println("🟡 FavoritesViewModel.updateAdvert() - обновляем ID ${updatedAdvert.id}")
            println("   Старый заголовок: ${_allAdverts[index].title}")
            println("   Новый заголовок: ${updatedAdvert.title}")

            // Сохраняем состояние избранного из старого объявления
            val wasFavorite = _allAdverts[index].isFavorite
            _allAdverts[index] = updatedAdvert.copy(isFavorite = wasFavorite)

            println("   Успешно обновлено!")
        } else {
            println("🔴 FavoritesViewModel.updateAdvert() - объявление с ID ${updatedAdvert.id} не найдено!")
        }
    }

    // НОВАЯ ФУНКЦИЯ: Удалить объявление
    fun removeAdvert(advertId: Int) {
        println("🔴 FavoritesViewModel.removeAdvert() - удаляем ID $advertId")
        println("   До удаления: ${_allAdverts.size} объявлений")

        val removed = _allAdverts.removeAll { it.id == advertId }

        // Также удаляем из избранного если нужно
        if (favoriteIds.contains(advertId)) {
            favoriteIds.remove(advertId)
            saveFavoritesToStorage()
        }

        println("   После удаления: ${_allAdverts.size} объявлений")
        println("   Удаление ${if (removed) "успешно" else "не удалось"}")
    }

    // ПРОСТОЙ МЕТОД ДЛЯ ПРОВЕРКИ
    fun getAdvertsCount(): Int = _allAdverts.size

    fun getFirstAdvertTitle(): String = _allAdverts.firstOrNull()?.title ?: "Нет объявлений"

    // НОВАЯ ФУНКЦИЯ: Найти объявление по ID
    fun findAdvertById(advertId: Int): Advert? {
        return _allAdverts.find { it.id == advertId }
    }
}