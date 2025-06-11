package com.example.sportsapp.requests

data class OrganizerProfileRequest(
    val orgName: String,
    val orgEmail: String,
    val orgPhone: String,
    val orgAddress: String,
    val fullname: String,
    val phone: String,
    val role: String
)