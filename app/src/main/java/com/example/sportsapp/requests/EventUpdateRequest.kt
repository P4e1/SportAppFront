package com.example.sportsapp.requests

import com.example.sportsapp.models.ParticipantCategory

data class EventUpdateRequest(
    val id: Int,
    val name: String,
    val description: String,
    val typeOfCompetition: String,
    val startDate: String,
    val startTime: String,
    val endDate: String,
    val endTime: String,
    val address: String,
    val aLotOfParticipant: Int,
    val limitOfParticipants: Int
)

