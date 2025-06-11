package com.example.sportsapp.managers

import android.content.Context

class RoleManager(context: Context) {

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveRole(role: String) {
        prefs.edit().putString("user_role", role).apply()
    }

    fun getRole(): String? = prefs.getString("user_role", null)

    fun clearRole() {
        prefs.edit().remove("user_role").apply()
    }
}
