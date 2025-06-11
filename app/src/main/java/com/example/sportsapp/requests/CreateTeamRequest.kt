package com.example.sportsapp.requests

import com.google.gson.annotations.SerializedName

data class CreateTeamRequest(
    @SerializedName("event_id")
    val eventId: Int,
    @SerializedName("team_name")
    val teamName: String
)