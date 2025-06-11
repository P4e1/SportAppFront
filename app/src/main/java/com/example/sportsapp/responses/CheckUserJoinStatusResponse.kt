package com.example.sportsapp.responses

import com.google.gson.annotations.SerializedName

data class CheckUserJoinStatusResponse(
    @SerializedName("is_joined")
    val isJoined: Boolean,

    @SerializedName("success")
    val success: Boolean = true,

    @SerializedName("status")
    val status: String? = null
)