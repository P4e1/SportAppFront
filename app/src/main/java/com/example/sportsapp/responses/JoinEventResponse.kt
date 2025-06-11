package com.example.sportsapp.responses

import com.google.gson.annotations.SerializedName

data class JoinEventResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("is_joined")
    val isJoined: Boolean = false,

    @SerializedName("current_participants")
    val currentParticipants: Int? = null
)