package com.example.district.security

class SecureAuth {
    companion object {
        // ПРОСТАЯ ПРОВЕРКА - ДЛЯ НАЧАЛА
        fun checkPassword(login: String, password: String): Boolean {
            return login == "admin" && password == "admin123"
        }
    }
}