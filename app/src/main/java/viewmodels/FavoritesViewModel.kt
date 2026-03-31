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

    private val gson = Gson()

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

    private val _allAdverts = mutableStateListOf<Advert>()
    val allAdverts: List<Advert> get() = _allAdverts

    var showFavoritesOnly by mutableStateOf(false)
        private set

    private val favoriteIds = mutableSetOf<Int>()

    init {
        loadFavoritesFromStorage()
        loadAdvertsFromStorage()
    }

    private fun loadAdvertsFromStorage() {
        viewModelScope.launch {
            val json = sharedPrefs.getString("saved_adverts", null)

            if (json != null && json.isNotBlank()) {
                try {
                    val type = object : TypeToken<List<Advert>>() {}.type
                    val savedAdverts = gson.fromJson<List<Advert>>(json, type)
                    _allAdverts.clear()
                    _allAdverts.addAll(savedAdverts ?: emptyList())
                    updateFavoritesStatus()
                } catch (e: Exception) {
                    loadSampleAdverts()
                }
            } else {
                loadSampleAdverts()
            }
        }
    }

    private fun saveAdvertsToStorage() {
        viewModelScope.launch {
            try {
                val json = gson.toJson(_allAdverts)
                sharedPrefs.edit()
                    .putString("saved_adverts", json)
                    .apply()
            } catch (e: Exception) {
                // Ошибка
            }
        }
    }

    private fun loadFavoritesFromStorage() {
        viewModelScope.launch {
            val savedIds = sharedPrefs.getStringSet("favorite_ids", emptySet()) ?: emptySet()
            favoriteIds.clear()
            savedIds.forEach {
                try {
                    favoriteIds.add(it.toInt())
                } catch (e: NumberFormatException) {
                    // Пропускаем
                }
            }
        }
    }

    private fun saveFavoritesToStorage() {
        viewModelScope.launch {
            val stringSet = favoriteIds.map { it.toString() }.toSet()
            sharedPrefs.edit()
                .putStringSet("favorite_ids", stringSet)
                .apply()
        }
    }

    private fun updateFavoritesStatus() {
        for (i in _allAdverts.indices) {
            val advert = _allAdverts[i]
            val isFavorite = favoriteIds.contains(advert.id)
            if (advert.isFavorite != isFavorite) {
                _allAdverts[i] = advert.copy(isFavorite = isFavorite)
            }
        }
    }

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
                address = "Уфа, Бородинская 19",
                ownerLogin = "alex",
                canEdit = false,
                telegram = null,
                phoneVisible = true,
                telegramVisible = false
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
                address = "Уфа, Бородинская 19",
                ownerLogin = "maria",
                canEdit = false,
                telegram = null,
                phoneVisible = true,
                telegramVisible = false
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
                address = "Уфа, Бородинская 19",
                ownerLogin = "dmitry",
                canEdit = false,
                telegram = null,
                phoneVisible = true,
                telegramVisible = false
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
                address = "Уфа, Бородинская 19",
                ownerLogin = "sergey",
                canEdit = false,
                telegram = null,
                phoneVisible = true,
                telegramVisible = false
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
                address = "Уфа, Бородинская 19",
                ownerLogin = "olga",
                canEdit = false,
                telegram = null,
                phoneVisible = true,
                telegramVisible = false
            )
        )

        _allAdverts.clear()
        _allAdverts.addAll(adverts)
    }

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

    fun getFilteredAdverts(category: String? = null): List<Advert> {
        return _allAdverts.filter { advert ->
            (category == null || category == "Все товары" || advert.category == category) &&
                    (!showFavoritesOnly || advert.isFavorite)
        }
    }

    fun addNewAdvert(advert: Advert) {
        val newId = (_allAdverts.maxOfOrNull { it.id } ?: 0) + 1
        val newAdvert = advert.copy(id = newId)
        _allAdverts.add(0, newAdvert)
        saveAdvertsToStorage()
    }

    fun updateAdvert(updatedAdvert: Advert) {
        val index = _allAdverts.indexOfFirst { it.id == updatedAdvert.id }
        if (index != -1) {
            val wasFavorite = _allAdverts[index].isFavorite
            _allAdverts[index] = updatedAdvert.copy(isFavorite = wasFavorite)
            saveAdvertsToStorage()
        }
    }

    fun removeAdvert(advertId: Int) {
        val removed = _allAdverts.removeAll { it.id == advertId }
        if (favoriteIds.contains(advertId)) {
            favoriteIds.remove(advertId)
            saveFavoritesToStorage()
        }
        if (removed) {
            saveAdvertsToStorage()
        }
    }

    fun getAdvertsCount(): Int = _allAdverts.size
    fun getFirstAdvertTitle(): String = _allAdverts.firstOrNull()?.title ?: "Нет объявлений"

    fun findAdvertById(advertId: Int): Advert? {
        return _allAdverts.find { it.id == advertId }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _allAdverts.clear()
            favoriteIds.clear()
            sharedPrefs.edit()
                .remove("saved_adverts")
                .remove("favorite_ids")
                .apply()
            loadSampleAdverts()
        }
    }
}