package com.example.sportsapp.models

data class TeamInfo(
    val id: Int,
    val name: String,
    val currentMembers: Int,
    val maxMembers: Int,
    val isJoinable: Boolean
)