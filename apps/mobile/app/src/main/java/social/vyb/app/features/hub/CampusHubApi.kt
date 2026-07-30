package social.vyb.app.features.hub

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query
import social.vyb.app.data.MeEnvelope

internal interface CampusHubApi {
    @GET("v1/me")
    suspend fun me(@Header("Authorization") authorization: String): MeEnvelope

    @GET("v1/events")
    suspend fun events(@Header("Authorization") authorization: String): EventsDashboardDto

    @GET("v1/events/{eventId}")
    suspend fun event(
        @Header("Authorization") authorization: String,
        @Path("eventId") eventId: String
    ): EventEnvelopeDto

    @POST("v1/events")
    suspend fun createEvent(
        @Header("Authorization") authorization: String,
        @Body request: UpsertHubEventRequest,
    ): EventMutationDto

    @PUT("v1/events/{eventId}")
    suspend fun updateEvent(
        @Header("Authorization") authorization: String,
        @Path("eventId") eventId: String,
        @Body request: UpsertHubEventRequest,
    ): EventMutationDto

    @PUT("v1/events/{eventId}/save")
    suspend fun toggleEventSave(
        @Header("Authorization") authorization: String,
        @Path("eventId") eventId: String
    ): EventMutationDto

    @POST("v1/events/{eventId}/register")
    suspend fun registerEvent(
        @Header("Authorization") authorization: String,
        @Path("eventId") eventId: String,
        @Body request: RegisterEventRequestDto
    ): EventRegistrationMutationDto

    @GET("v1/events/{eventId}/registrations")
    suspend fun registrations(
        @Header("Authorization") authorization: String,
        @Path("eventId") eventId: String,
    ): HubEventRegistrationListDto

    @PUT("v1/events/{eventId}/registrations/{registrationId}")
    suspend fun manageRegistration(
        @Header("Authorization") authorization: String,
        @Path("eventId") eventId: String,
        @Path("registrationId") registrationId: String,
        @Body request: ManageHubEventRegistrationRequest,
    ): ManageHubEventRegistrationDto

    @GET("v1/resources")
    suspend fun resources(
        @Header("Authorization") authorization: String,
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 50
    ): ResourcesResponseDto

    @GET("v1/communities/my")
    suspend fun communities(
        @Header("Authorization") authorization: String
    ): CommunitiesResponseDto

    @GET("v1/communities/{slug}")
    suspend fun community(
        @Header("Authorization") authorization: String,
        @Path("slug") slug: String
    ): CommunityDetailDto

    @GET("v1/communities/{slug}/members")
    suspend fun communityMembers(
        @Header("Authorization") authorization: String,
        @Path("slug") slug: String,
        @Query("limit") limit: Int = 50
    ): CommunityMembersDto
}
