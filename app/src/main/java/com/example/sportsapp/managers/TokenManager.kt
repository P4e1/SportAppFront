package com.example.sportsapp.managers

import android.content.Context

class TokenManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    // Основной токен (access)
    fun saveToken(token: String) {
        prefs.edit().putString("jwt_token", token).apply()
    }

    fun getToken(): String? = prefs.getString("jwt_token", null)

    fun clearToken() {
        prefs.edit().remove("jwt_token").apply()
        prefs.edit().remove("refresh_token").apply() // Очистим и refresh при выходе
    }

    // Дополнительный метод для refresh-токена
    fun saveRefreshToken(refreshToken: String) {
        prefs.edit().putString("refresh_token", refreshToken).apply()
    }

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
}
