package social.vyb.app.features.stories

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface StoriesVibesApi {
    @GET("v1/me")
    suspend fun me(
        @Header("Authorization") bearerToken: String
    ): MobileMeEnvelope

    @GET("v1/stories")
    suspend fun stories(
        @Header("Authorization") bearerToken: String,
        @Query("tenantId") tenantId: String
    ): StoriesEnvelope

    @PUT("v1/stories/{storyId}/seen")
    suspend fun markStorySeen(
        @Header("Authorization") bearerToken: String,
        @Path("storyId") storyId: String,
        @Body body: Map<String, String> = emptyMap()
    ): StorySeenEnvelope

    @PUT("v1/stories/{storyId}/reactions")
    suspend fun toggleStoryLike(
        @Header("Authorization") bearerToken: String,
        @Path("storyId") storyId: String,
        @Body body: Map<String, String> = emptyMap()
    ): StoryReactionEnvelope

    @GET("v1/vibes")
    suspend fun vibes(
        @Header("Authorization") bearerToken: String,
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 12,
        @Query("cursor") cursor: String? = null
    ): VibesEnvelope

    @PUT("v1/posts/{postId}/reactions")
    suspend fun toggleVibeLike(
        @Header("Authorization") bearerToken: String,
        @Path("postId") postId: String,
        @Body body: ReactionRequest = ReactionRequest()
    ): VibeReactionEnvelope
}
