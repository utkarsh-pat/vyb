package social.vyb.app.features.profile

import android.content.ContentResolver
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import social.vyb.app.data.ProfileEnvelope
import social.vyb.app.data.UpsertProfileRequest
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken
import social.vyb.app.features.media.ContentUriRequestBody
import social.vyb.app.features.media.measureContentBytes
import social.vyb.app.features.search.FollowResponse
import social.vyb.app.features.search.PublicProfileResponse

internal interface ProfileApi {
    @GET("v1/profile")
    suspend fun ownProfile(@Header("Authorization") bearer: String): ProfileEnvelope

    @PUT("v1/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body body: UpsertProfileRequest
    ): ProfileEnvelope

    @POST("v1/social-media/upload")
    suspend fun uploadMedia(
        @Header("Authorization") bearer: String,
        @Query("intent") intent: String,
        @Query("fileName") fileName: String,
        @Body body: RequestBody
    ): ProfileMediaUploadEnvelope

    @GET("v1/users/{username}")
    suspend fun publicProfile(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): PublicProfileResponse

    @GET("v1/posts/saved")
    suspend fun savedPosts(
        @Header("Authorization") bearer: String,
        @Query("limit") limit: Int = 50
    ): ProfileSavedPostsEnvelope

    @GET("v1/users/{username}/{scope}")
    suspend fun connections(
        @Header("Authorization") bearer: String,
        @Path("username") username: String,
        @Path("scope") scope: String,
        @Query("limit") limit: Int = 50
    ): ProfileConnectionsResponse

    @PUT("v1/users/{username}/follow")
    suspend fun follow(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): FollowResponse

    @DELETE("v1/users/{username}/follow")
    suspend fun unfollow(
        @Header("Authorization") bearer: String,
        @Path("username") username: String
    ): FollowResponse

    @GET("v1/chats/privacy-settings")
    suspend fun privacy(@Header("Authorization") bearer: String): ChatPrivacyEnvelope

    @PUT("v1/chats/privacy-settings")
    suspend fun updatePrivacy(
        @Header("Authorization") bearer: String,
        @Body body: UpdateChatPrivacyRequest
    ): ChatPrivacyEnvelope

    @GET("v1/chats/devices")
    suspend fun devices(@Header("Authorization") bearer: String): TrustedDevicesEnvelope

    @DELETE("v1/chats/devices/{deviceId}")
    suspend fun revokeDevice(
        @Header("Authorization") bearer: String,
        @Path("deviceId") deviceId: String
    ): RevokeTrustedDeviceResponse
}

class ProfileRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: ProfileApi = VybNetwork.create()

    suspend fun load(): OwnProfileBundle = coroutineScope {
        val bearer = auth.requireBearerToken()
        val own = api.ownProfile(bearer)
        val username = requireNotNull(own.profile?.username) {
            "Complete your campus profile before opening the dashboard."
        }
        val public = async { api.publicProfile(bearer, username) }
        val privacy = async {
            runCatching { api.privacy(bearer).settings }
                .getOrDefault(ChatPrivacySettings())
        }
        val devices = async {
            runCatching { api.devices(bearer).items.filter { it.revokedAt == null } }
                .getOrDefault(emptyList())
        }
        OwnProfileBundle(
            privateProfile = own,
            publicProfile = public.await(),
            privacy = privacy.await(),
            devices = devices.await()
        )
    }

    suspend fun updateProfile(request: UpsertProfileRequest): ProfileEnvelope =
        api.updateProfile(auth.requireBearerToken(), request)

    suspend fun uploadAvatar(resolver: ContentResolver, uri: Uri): String {
        val mimeType = resolver.getType(uri)?.lowercase()
            ?: error("The selected image type could not be detected.")
        require(mimeType in setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")) {
            "Choose a JPG, PNG, WebP, HEIC, or HEIF image."
        }
        val maxBytes = 4L * 1024L * 1024L
        val sizeBytes = resolver.measureContentBytes(uri, maxBytes)
        return api.uploadMedia(
            auth.requireBearerToken(),
            intent = "avatar",
            fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "profile-photo",
            body = ContentUriRequestBody(
                resolver = resolver,
                uri = uri,
                mediaType = mimeType.toMediaType(),
                expectedBytes = sizeBytes
            )
        ).asset.url
    }

    suspend fun connections(username: String, scope: String): List<ProfileConnection> {
        require(scope == "followers" || scope == "following")
        return api.connections(auth.requireBearerToken(), username, scope).items
    }

    suspend fun loadSavedPosts() = api.savedPosts(auth.requireBearerToken()).items

    suspend fun setFollowing(username: String, following: Boolean): FollowResponse {
        val bearer = auth.requireBearerToken()
        return if (following) api.follow(bearer, username) else api.unfollow(bearer, username)
    }

    suspend fun updatePrivacy(settings: ChatPrivacySettings): ChatPrivacySettings =
        api.updatePrivacy(
            auth.requireBearerToken(),
            UpdateChatPrivacyRequest(
                lastSeenOnline = settings.lastSeenOnline,
                readReceipts = settings.readReceipts,
                typingIndicator = settings.typingIndicator
            )
        ).settings

    suspend fun revokeDevice(deviceId: String): List<TrustedDevice> =
        api.revokeDevice(auth.requireBearerToken(), deviceId).items.filter { it.revokedAt == null }

    suspend fun sendPasswordReset(): String {
        val user = auth.currentUser ?: error("Your session expired.")
        val email = user.email ?: error("No account email is available.")
        suspendCancellableCoroutine<Unit> { continuation ->
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        return "Password reset email sent to $email."
    }
}
