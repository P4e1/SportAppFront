package com.example.sportsapp.responses

data class ProfileResponse(
    val id: Int,
    val username: String,
    val email: String,
    val fullname: String,
    val phone: String,
    val role: String,
    val organizer: OrganizerInfo?,
    val participant: ParticipantInfo?
)

data class OrganizerInfo(
    val id: Int,
    val orgName: String,
    val orgEmail: String,
    val orgPhone: String,
    val orgAddress: String
)

data class ParticipantInfo(
    val institution: String,
    val dateR: String,
    val height: Int,
    val weight: Int,
    val numberOfCompetitions: Int,
    val rating: Int
)
