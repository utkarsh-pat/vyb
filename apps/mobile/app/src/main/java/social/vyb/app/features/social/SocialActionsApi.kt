package social.vyb.app.features.social

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
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

    @GET("v1/posts/{postId}")
    suspend fun post(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String
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

    @PUT("v1/comments/{commentId}/reactions")
    suspend fun toggleCommentReaction(
        @Header("Authorization") bearer: String,
        @Path("commentId") commentId: String,
        @Body body: ReactionBody = ReactionBody()
    ): CommentReactionResult

    @GET("v1/posts/{postId}/likes")
    suspend fun reactionMembers(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Query("limit") limit: Int = 50
    ): ReactionMembersEnvelope

    @POST("v1/posts/{postId}/repost")
    suspend fun repost(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Body body: RepostBody
    ): CreatePostEnvelope

    @PATCH("v1/posts/{postId}")
    suspend fun updatePost(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String,
        @Body body: UpdatePostBody
    ): UpdatePostEnvelope

    @DELETE("v1/posts/{postId}")
    suspend fun deletePost(
        @Header("Authorization") bearer: String,
        @Path("postId") postId: String
    )

    @PATCH("v1/comments/{commentId}")
    suspend fun updateComment(
        @Header("Authorization") bearer: String,
        @Path("commentId") commentId: String,
        @Body body: UpdateCommentBody
    ): UpdateCommentEnvelope

    @DELETE("v1/comments/{commentId}")
    suspend fun deleteComment(
        @Header("Authorization") bearer: String,
        @Path("commentId") commentId: String
    )

    @POST("v1/reports")
    suspend fun report(
        @Header("Authorization") bearer: String,
        @Body body: ReportBody
    ): ReportEnvelope
}
