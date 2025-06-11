package com.example.sportsapp.responses

data class ResetPasswordResponse(
    val message: String? = null,
    val success: Boolean = false,
    val error: String? = null
)