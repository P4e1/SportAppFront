package com.example.sportsapp.models

data class Participant (
    val id: Int,
    val fullname: String,
    val dateR: String,
    val height: Int,
    val weight: Int,
    val team: String,
    val teamParticipantStatus: String
)
