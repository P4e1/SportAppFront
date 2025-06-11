package com.example.sportsapp.responses

import com.example.sportsapp.models.TeamInfo

data class EventTeamsResponse(
    val success: Boolean,
    val teams: List<TeamInfo>
)