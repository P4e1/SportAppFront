package com.example.sportsapp.responses

import com.google.gson.annotations.SerializedName

data class TeamActionResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("team_id")
    val teamId: Int? = null,
    @SerializedName("team_name")
    val teamName: String? = null
)