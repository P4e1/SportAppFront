package com.example.sportsapp.responses

data class RegisterResponse(
    val id: Int,
    val username: String,
    val email: String,
    val fullname: String,
    val phone: String
)