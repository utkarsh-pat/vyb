package social.vyb.app.features.messages

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import social.vyb.app.data.FeedEnvelope
import social.vyb.app.data.MeEnvelope
import social.vyb.app.features.hub.CommunitiesResponseDto
import social.vyb.app.features.hub.CommunityDetailDto

internal interface ChatApi {
    @GET("v1/me")
    suspend fun viewer(@Header("Authorization") authorization: String): MeEnvelope

    @GET("v1/communities/my")
    suspend fun communities(
        @Header("Authorization") authorization: String
    ): CommunitiesResponseDto

    @GET("v1/communities/{slug}")
    suspend fun community(
        @Header("Authorization") authorization: String,
        @Path("slug") slug: String
    ): CommunityDetailDto

    @GET("v1/feed")
    suspend fun communityMessages(
        @Header("Authorization") authorization: String,
        @Query("tenantId") tenantId: String,
        @Query("communityId") communityId: String,
        @Query("limit") limit: Int = 50
    ): FeedEnvelope

    @POST("v1/posts")
    suspend fun sendCommunityMessage(
        @Header("Authorization") authorization: String,
        @Body body: SendCommunityMessageRequestDto
    ): CommunityMessageEnvelopeDto

    @GET("v1/chats/socket-token")
    suspend fun socketToken(
        @Header("Authorization") authorization: String,
        @Query("conversationId") conversationId: String
    ): ChatSocketTokenDto

    @GET("v1/chats")
    suspend fun inbox(@Header("Authorization") authorization: String): ChatInboxDto

    @GET("v1/chats/{conversationId}")
    suspend fun conversation(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String
    ): ChatConversationEnvelopeDto

    @POST("v1/chats/{conversationId}/messages")
    suspend fun sendMessage(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Body body: SendChatMessageRequestDto
    ): SendChatMessageResponseDto

    @PUT("v1/chats/{conversationId}/read")
    suspend fun markRead(
        @Header("Authorization") authorization: String,
        @Path("conversationId") conversationId: String,
        @Body body: MarkChatReadRequestDto
    ): MarkChatReadResponseDto

    @PUT("v1/chats/keys")
    suspend fun upsertIdentity(
        @Header("Authorization") authorization: String,
        @Body body: UpsertChatIdentityRequestDto
    ): UpsertChatIdentityResponseDto
}
