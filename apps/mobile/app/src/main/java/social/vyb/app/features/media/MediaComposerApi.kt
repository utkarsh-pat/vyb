package social.vyb.app.features.media

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.RequestBody

internal interface MediaComposerApi {
    @GET("v1/me")
    suspend fun viewer(@Header("Authorization") authorization: String): MediaViewerEnvelope

    @POST("v1/social-media/upload")
    suspend fun uploadStream(
        @Header("Authorization") authorization: String,
        @Query("intent") intent: String,
        @Query("fileName") fileName: String,
        @Body body: RequestBody,
    ): UploadSocialMediaEnvelope

    @POST("v1/social-media/upload")
    suspend fun uploadLegacy(
        @Header("Authorization") authorization: String,
        @Body body: LegacySocialMediaUploadRequest
    ): UploadSocialMediaEnvelope

    @POST("v1/posts")
    suspend fun createPost(
        @Header("Authorization") authorization: String,
        @Body request: CreateMediaPostRequest
    ): CreatedItemEnvelope

    @POST("v1/stories")
    suspend fun createStory(
        @Header("Authorization") authorization: String,
        @Body request: CreateMediaStoryRequest
    ): CreatedItemEnvelope
}
