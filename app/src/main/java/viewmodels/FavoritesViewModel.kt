package com.example.district.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.district.models.Advert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class FavoritesViewModel(private val context: Context) : ViewModel() {

    // 📦 Библиотека для работы с JSON (чтобы сохранять объекты)
    private val gson = Gson()

    // 🔐 Зашифрованное хранилище
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

    // 📋 Все объявления
    private val _allAdverts = mutableStateListOf<Advert>()
    val allAdverts: List<Advert> get() = _allAdverts

    // ❤️ Показывать только избранное?
    var showFavoritesOnly by mutableStateOf(false)
        private set

    // 💾 Избранные ID
    private val favoriteIds = mutableSetOf<Int>()

    // 🚀 ПРИ ЗАПУСКЕ: загружаем всё
    init {
        loadFavoritesFromStorage()
        loadAdvertsFromStorage()  // ⬅️ ВАЖНО: теперь грузим из хранилища!
    }

    // ========== 💾 СОХРАНЕНИЕ ОБЪЯВЛЕНИЙ ==========

    // 📥 ЗАГРУЖАЕМ объявления из памяти телефона
    private fun loadAdvertsFromStorage() {
        viewModelScope.launch {
            // 1. Пытаемся достать сохранённые объявления
            val json = sharedPrefs.getString("saved_adverts", null)

            if (json != null && json.isNotBlank()) {
                // 2. УРА! Есть сохранённые объявления
                println("📥 Загружаем сохранённые объявления...")

                try {
                    // 3. Превращаем текст JSON обратно в список объявлений
                    val type = object : TypeToken<List<Advert>>() {}.type
                    val savedAdverts = gson.fromJson<List<Advert>>(json, type)

                    // 4. Очищаем старые и добавляем сохранённые
                    _allAdverts.clear()
                    _allAdverts.addAll(savedAdverts ?: emptyList())

                    println("✅ Загружено ${savedAdverts?.size ?: 0} объявлений")

                    // 5. Обновляем состояния избранного
                    updateFavoritesStatus()

                } catch (e: Exception) {
                    // 6. Если ошибка - грузим примерные объявления
                    println("❌ Ошибка загрузки: ${e.message}")
                    loadSampleAdverts()
                }
            } else {
                // 7. Если нет сохранённых - грузим примерные
                println("📭 Нет сохранённых объявлений, грузим примерные")
                loadSampleAdverts()
            }
        }
    }

    // 💾 СОХРАНЯЕМ объявления в память телефона
    private fun saveAdvertsToStorage() {
        viewModelScope.launch {
            try {
                // 1. Превращаем список объявлений в текст JSON
                val json = gson.toJson(_allAdverts)

                // 2. Сохраняем в зашифрованное хранилище
                sharedPrefs.edit()
                    .putString("saved_adverts", json)
                    .apply()

                println("💾 Сохранено ${_allAdverts.size} объявлений")
            } catch (e: Exception) {
                println("❌ Ошибка сохранения: ${e.message}")
            }
        }
    }

    // ========== ❤️ ИЗБРАННОЕ ==========

    // 📥 Загружаем избранное
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
            println("❤️ Загружено ${favoriteIds.size} избранных")
        }
    }

    // 💾 Сохраняем избранное
    private fun saveFavoritesToStorage() {
        viewModelScope.launch {
            val stringSet = favoriteIds.map { it.toString() }.toSet()
            sharedPrefs.edit()
                .putStringSet("favorite_ids", stringSet)
                .apply()
        }
    }

    // 🔄 Обновляем статус избранного у всех объявлений
    private fun updateFavoritesStatus() {
        for (i in _allAdverts.indices) {
            val advert = _allAdverts[i]
            val isFavorite = favoriteIds.contains(advert.id)
            if (advert.isFavorite != isFavorite) {
                _allAdverts[i] = advert.copy(isFavorite = isFavorite)
            }
        }
    }

    // ========== 📝 ПРИМЕРНЫЕ ДАННЫЕ ==========

    // 🧪 Загружаем примерные объявления (только если нет сохранённых)
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
                ownerLogin = "alex",
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
                ownerLogin = "maria",
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
                ownerLogin = "dmitry",
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
                ownerLogin = "sergey",
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
                ownerLogin = "olga",
                canEdit = false
            )
        )

        _allAdverts.clear()
        _allAdverts.addAll(adverts)
        println("🧪 Загружено ${adverts.size} примерных объявлений")
    }

    // ========== 🎯 ОСНОВНЫЕ ФУНКЦИИ ==========

    // ❤️ Переключить избранное
    fun toggleFavorite(advertId: Int) {
        val index = _allAdverts.indexOfFirst { it.id == advertId }
        if (index != -1) {
            val wasFavorite = _allAdverts[index].isFavorite
            _allAdverts[index] = _allAdverts[index].copy(
                isFavorite = !wasFavorite
            )

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

    // 📋 Фильтрация
    fun getFilteredAdverts(category: String? = null): List<Advert> {
        return _allAdverts.filter { advert ->
            (category == null || category == "Все товары" || advert.category == category) &&
                    (!showFavoritesOnly || advert.isFavorite)
        }
    }

    // ========== ✨ СОЗДАНИЕ/РЕДАКТИРОВАНИЕ/УДАЛЕНИЕ ==========

    // ➕ ДОБАВИТЬ новое объявление
    fun addNewAdvert(advert: Advert) {
        println("🟢 Добавляем новое объявление: ${advert.title}")

        // Генерируем новый уникальный ID
        val newId = (_allAdverts.maxOfOrNull { it.id } ?: 0) + 1
        val newAdvert = advert.copy(id = newId)

        // Добавляем в начало списка
        _allAdverts.add(0, newAdvert)

        // 💾 НОВОЕ: Сохраняем изменения!
        saveAdvertsToStorage()

        println("✅ Добавлено! ID: $newId, всего: ${_allAdverts.size}")
    }

    // ✏️ ОБНОВИТЬ объявление
    fun updateAdvert(updatedAdvert: Advert) {
        val index = _allAdverts.indexOfFirst { it.id == updatedAdvert.id }
        if (index != -1) {
            println("🟡 Обновляем объявление ID ${updatedAdvert.id}")

            // Сохраняем состояние избранного
            val wasFavorite = _allAdverts[index].isFavorite
            _allAdverts[index] = updatedAdvert.copy(isFavorite = wasFavorite)

            // 💾 НОВОЕ: Сохраняем изменения!
            saveAdvertsToStorage()

            println("✅ Обновлено!")
        } else {
            println("🔴 Объявление с ID ${updatedAdvert.id} не найдено!")
        }
    }

    // ❌ УДАЛИТЬ объявление
    fun removeAdvert(advertId: Int) {
        println("🔴 Удаляем объявление ID $advertId")

        val removed = _allAdverts.removeAll { it.id == advertId }

        // Удаляем из избранного если нужно
        if (favoriteIds.contains(advertId)) {
            favoriteIds.remove(advertId)
            saveFavoritesToStorage()
        }

        // 💾 НОВОЕ: Сохраняем изменения!
        if (removed) {
            saveAdvertsToStorage()
        }

        println("${if (removed) "✅ Удалено!" else "❌ Не найдено"}")
    }

    // ========== 🛠️ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ==========

    fun getAdvertsCount(): Int = _allAdverts.size
    fun getFirstAdvertTitle(): String = _allAdverts.firstOrNull()?.title ?: "Нет объявлений"

    fun findAdvertById(advertId: Int): Advert? {
        return _allAdverts.find { it.id == advertId }
    }

    // 🧹 Для тестов: очистить ВСЕ данные
    fun clearAllData() {
        viewModelScope.launch {
            _allAdverts.clear()
            favoriteIds.clear()

            sharedPrefs.edit()
                .remove("saved_adverts")
                .remove("favorite_ids")
                .apply()

            println("🧹 Все данные очищены")
            loadSampleAdverts()
        }
    }
}