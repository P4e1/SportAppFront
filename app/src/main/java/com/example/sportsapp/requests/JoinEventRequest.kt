package com.example.sportsapp.requests

import com.google.gson.annotations.SerializedName

data class JoinEventRequest(
    @SerializedName("event_id")
    val eventId: Int
)