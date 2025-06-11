package com.example.sportsapp.models

data class ParticipantCategory(
    val type: String, // "Возрастная", "Весовая", "Мой вариант"
    val name: String,
    val minValue: Int? = null,
    val maxValue: Int? = null
)