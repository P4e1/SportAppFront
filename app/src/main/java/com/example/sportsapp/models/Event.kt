package com.example.sportsapp.models

data class Event (
    val id: Int,
    val name: String,
    val description: String,
    val typeOfCompetition: String,
    val categories: List<ParticipantCategory>,
    val startDate: String,
    val startTime: String,
    val endDate: String,
    val endTime: String,
    val address: String,
    val aLotOfParticipant: Int,
    val limitOfParticipants: Int,
    val status: String
)
