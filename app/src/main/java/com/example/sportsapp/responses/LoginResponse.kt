package com.example.sportsapp.responses

data class LoginResponse(
    val access: String,
    val refresh: String,
    val role: String
)