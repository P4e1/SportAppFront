package com.example.sportsapp.requests

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)