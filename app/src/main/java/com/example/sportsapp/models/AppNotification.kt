package com.example.sportsapp.models

import java.io.Serializable

data class AppNotification(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val isRead: Boolean = false,
    val type: String = "info"
) : Serializable // ✅ добавлено
