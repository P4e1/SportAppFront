package com.example.sportsapp.api

import com.example.sportsapp.models.Event
import com.example.sportsapp.models.AppNotification
import com.example.sportsapp.models.Participant
import com.example.sportsapp.requests.ChangePasswordRequest
import com.example.sportsapp.requests.CreateTeamRequest
import com.example.sportsapp.requests.EventCreateRequest
import com.example.sportsapp.requests.EventUpdateRequest
import com.example.sportsapp.requests.JoinEventRequest
import com.example.sportsapp.requests.JoinTeamRequest
import com.example.sportsapp.requests.LeaveEventRequest
import com.example.sportsapp.responses.EventCreateResponse
import com.example.sportsapp.requests.LoginRequest
import com.example.sportsapp.responses.LoginResponse
import com.example.sportsapp.requests.OrganizerProfileRequest
import com.example.sportsapp.requests.ParticipantProfileRequest
import com.example.sportsapp.responses.ProfileResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.sportsapp.requests.RegisterRequest
import com.example.sportsapp.responses.RegisterResponse
import com.example.sportsapp.requests.ResetPasswordRequest
import com.example.sportsapp.responses.CheckUserJoinStatusResponse
import com.example.sportsapp.responses.EventTeamsResponse
import com.example.sportsapp.responses.JoinEventResponse
import com.example.sportsapp.responses.ResetPasswordResponse
import com.example.sportsapp.responses.TeamActionResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {
    @POST("api/register/")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("api/token/")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @POST("api/password/reset/")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<ResetPasswordResponse>

    @POST("api/participantset/")
    fun createParticipantProfile(
        @Header("Authorization") authHeader: String,
        @Body request: ParticipantProfileRequest
    ): Call<ProfileResponse>

    @POST("api/organizerset/")
    fun createOrganizerProfile(
        @Header("Authorization") authHeader: String,
        @Body request: OrganizerProfileRequest
    ): Call<ProfileResponse>

    @POST("api/event/")
    fun createEvent(
        @Header("Authorization") authHeader: String,
        @Body event: EventCreateRequest
    ): Call<EventCreateResponse>

    @GET("api/event/")
    fun getMyEvents(
        @Header("Authorization") token: String
    ): Call<List<Event>>

    @GET("api/event/all/")
    fun getAllEvents(
        @Header("Authorization") token: String
    ): Call<List<Event>>

    @POST("api/event/join/")
    fun joinEvent(
        @Body request: JoinEventRequest,
        @Header("Authorization") token: String
    ): Call<JoinEventResponse>

    @POST("api/event/leave/")
    fun leaveEvent(
        @Body request: LeaveEventRequest,
        @Header("Authorization") token: String
    ): Call<JoinEventResponse>

    @GET("api/event/{eventId}/join-status/")
    fun checkUserJoinStatus(
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Call<CheckUserJoinStatusResponse>

    // Новые методы для команд
    @POST("api/event/teams/join/")
    fun joinTeam(
        @Body request: JoinTeamRequest,
        @Header("Authorization") token: String
    ): Call<TeamActionResponse>

    @POST("api/event/teams/create/")
    fun createTeam(
        @Body request: CreateTeamRequest,
        @Header("Authorization") token: String
    ): Call<TeamActionResponse>

    @GET("api/event/{eventId}/teams/")
    fun getEventTeams(
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Call<EventTeamsResponse>

    @PATCH("api/event/{id}/")
    fun updateEvent(
        @Header("Authorization") token: String,
        @Path("id") eventId: Int,
        @Body event: EventUpdateRequest
    ): Call<Void>

    // Получить список участников события
    @GET("api/event/{event_id}/participants/")
    fun getEventParticipants(
        @Header("Authorization") token: String,
        @Path("event_id") eventId: Int
    ): Call<List<Participant>>

    // Подтвердить участника события
    @PUT("api/event/{event_id}/participants/{participant_id}/confirm/")
    fun confirmParticipant(
        @Header("Authorization") token: String,
        @Path("event_id") eventId: Int,
        @Path("participant_id") participantId: Int
    ): Call<ResponseBody>

    // Отклонить участника события
    @PUT("api/event/{event_id}/participants/{participant_id}/reject/")
    fun rejectParticipant(
        @Header("Authorization") token: String,
        @Path("event_id") eventId: Int,
        @Path("participant_id") participantId: Int
    ): Call<ResponseBody>

    @GET("api/notification/")
    fun getAppNotifications(
        @Header("Authorization") token: String
    ): Call<List<AppNotification>>

    @GET("api/profile/")
    fun getProfile(
        @Header("Authorization") token: String
    ): Call<List<ProfileResponse>>

    @POST("api/logout/")
    fun logout(@Body body: Map<String, String>): Call<Void>

    @POST("api/change-password/")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body passwordData: Map<String, String>
    ): Call<Map<String, String>>

    @POST("api/event/{eventId}/complete/")
    fun completeEvent(
        @Header("Authorization") authHeader: String,
        @Path("eventId") eventId: Int,
        @Body results: List<Map<String, String>>
    ): Call<Void>
}