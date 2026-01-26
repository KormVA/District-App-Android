package com.example.district.models

// Модель объявления
data class Advert(
    val id: Int,
    val title: String,
    val description: String,
    val price: String,
    val category: String,
    val author: String,
    val phone: String,
    val date: String,
    val imageUrl: String = "", // для будущих фото
    var isFavorite: Boolean = false,
    val house: String, // ← ЗАПЯТАЯ ЗДЕСЬ ОБЯЗАТЕЛЬНА!
    val ownerLogin: String = "", // ← ДОБАВЛЕНО: логин владельца для проверки
    var canEdit: Boolean = false // ← ДОБАВЛЕНО: может ли текущий пользователь редактировать
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

// Расширяем модель пользователя
data class UserProfile(
    val login: String,
    val displayName: String, // Имя для показа
    val house: String, // Дом пользователя
    val phone: String = ""
)