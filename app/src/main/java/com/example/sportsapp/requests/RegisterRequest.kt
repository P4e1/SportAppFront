package com.example.sportsapp.requests

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String
)