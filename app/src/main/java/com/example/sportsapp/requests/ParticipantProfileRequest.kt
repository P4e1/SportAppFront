package com.example.sportsapp.requests

data class ParticipantProfileRequest(
    val dateR: String,        // Формат: YYYY-MM-DD
    val height: Int,
    val weight: Int,
    val fullname: String,
    val phone: String,
    val role: String
)