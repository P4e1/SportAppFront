package com.example.sportsapp.requests

import com.example.sportsapp.models.ParticipantCategory

data class EventCreateRequest(
    val name: String,
    val description: String,
    val typeOfCompetition: String,
    val categories: List<ParticipantCategory>,
    val startDate: String,
    val startTime: String,
    val endDate: String,
    val endTime: String,
    val address: String,
    val limitOfParticipants: Int
)