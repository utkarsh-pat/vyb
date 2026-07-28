package social.vyb.app.features.messages

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface ChatApi {
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
