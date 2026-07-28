package social.vyb.app.features.social

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

internal interface SocialActionsApi {
    @GET("v1/me")
    suspend fun viewer(@Header("Authorization") bearer: String): ViewerEnvelope

    @POST("v1/posts")
    suspend fun createPost(
        @Header("Authorization") bearer: String,
        @Body body: CreatePostBody
    ): CreatePostEnvelope

    @PUT("v1/posts/{postId}/reactions")
    suspend fun toggleReaction(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Body body: ReactionBody
    ): ReactionResult

    @PUT("v1/posts/{postId}/save")
    suspend fun toggleSave(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Body body: Map<String, String> = emptyMap()
    ): SaveResult

    @GET("v1/posts/{postId}/comments")
    suspend fun comments(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Query("limit") limit: Int = 50
    ): CommentListEnvelope

    @POST("v1/posts/{postId}/comments")
    suspend fun addComment(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Body body: CreateCommentBody
    ): CreateCommentEnvelope
}
