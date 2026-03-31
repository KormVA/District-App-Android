package com.example.district.models

// Модель объявления
data class Advert(
    val id: Int,
    val title: String,
    val description: String,
    val price: String,
    val category: String,
    val author: String,
    val phone: String? = null,           // ← изменено на nullable
    val telegram: String? = null,        // ← добавлено
    val date: String,
    val imageUrl: String = "",
    var isFavorite: Boolean = false,
    val ownerLogin: String = "",
    var canEdit: Boolean = false,
    val phoneVisible: Boolean = false,    // ← добавлено
    val address: String? = null,
    val telegramVisible: Boolean = false  // ← добавлено
)

// Категории товаров
enum class Category(val title: String) {
    ELECTRONICS("Электроника"),
    CLOTHES("Одежда"),
    BOOKS("Книги"),
    FURNITURE("Мебель"),
    AUTO("Авто"),
    OTHER("Другое")
}
