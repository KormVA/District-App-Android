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
    var isFavorite: Boolean = false, // ← НОВОЕ ПОЛЕ
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