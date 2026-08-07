package social.vyb.app.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

@Serializable
data class SessionBootstrapRequest(
    val idToken: String,
    val displayName: String? = null
)

@Serializable
data class SessionBootstrapEnvelope(
    val profileCompleted: Boolean = false,
    val nextPath: String? = null
)

@Serializable
data class ProfileRecord(
    val userId: String,
    val tenantId: String,
    val primaryEmail: String,
    val collegeName: String,
    val avatarUrl: String? = null,
    val username: String,
    val firstName: String,
    val lastName: String? = null,
    val fullName: String,
    val course: String,
    val stream: String,
    val year: Int,
    val section: String,
    val isHosteller: Boolean,
    val hostelName: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val socialLinks: Map<String, String>? = null,
    val profileCompleted: Boolean,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class ProfileEnvelope(
    val profileCompleted: Boolean,
    val allowedEmailDomain: String,
    val collegeName: String,
    val profile: ProfileRecord? = null
)

@Serializable
data class UpsertProfileRequest(
    val username: String,
    val firstName: String,
    val lastName: String? = null,
    val course: String,
    val stream: String,
    val year: Int,
    val section: String,
    val isHosteller: Boolean,
    val hostelName: String? = null,
    val phoneNumber: String? = null,
    val bio: String? = null,
    val socialLinks: Map<String, String>? = null,
    val avatarUrl: String? = null
)

@Serializable
data class CourseCatalogItem(
    val id: String,
    val code: String,
    val title: String,
    val branch: String? = null
)

@Serializable
data class CourseCatalogEnvelope(val items: List<CourseCatalogItem> = emptyList())

@Serializable
data class UsernameAvailabilityEnvelope(
    val username: String,
    val available: Boolean,
    val isCurrent: Boolean = false
)

@Serializable
data class MeEnvelope(
    val user: RemoteUser,
    val membershipSummary: RemoteMembership
)

@Serializable
data class RemoteUser(
    val id: String,
    val primaryEmail: String,
    val displayName: String,
    val status: String
)

@Serializable
data class RemoteMembership(
    val id: String,
    val tenantId: String,
    val role: String,
    val verificationStatus: String
)

@Serializable
data class FeedEnvelope(
    val tenantId: String,
    val communityId: String? = null,
    val items: List<RemotePost> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class RemotePost(
    val id: String,
    val placement: String = "feed",
    val title: String = "",
    val body: String = "",
    val kind: String = "text",
    val mediaUrl: String? = null,
    val media: List<RemoteMediaAsset> = emptyList(),
    val location: String? = null,
    val reactions: Int = 0,
    val comments: Int = 0,
    val savedCount: Int = 0,
    val isSaved: Boolean = false,
    val isAnonymous: Boolean = false,
    val visibility: String = "public",
    val viewerCanManage: Boolean = false,
    val viewerReactionType: String? = null,
    val createdAt: String,
    val author: RemoteAuthor
)

@Serializable
data class RemoteMediaAsset(
    val url: String,
    val kind: String,
    val mimeType: String? = null,
    val processingStatus: String? = null
)

@Serializable
data class RemoteAuthor(
    val userId: String? = null,
    val username: String = "member",
    val displayName: String = "Vyb member",
    val avatarUrl: String? = null,
    val isAnonymous: Boolean = false
)

interface ApiService {
    @POST("v1/auth/session/bootstrap")
    suspend fun bootstrapSession(
        @Body request: SessionBootstrapRequest
    ): Response<SessionBootstrapEnvelope>

    @GET("v1/me")
    suspend fun me(@Header("Authorization") bearerToken: String): MeEnvelope

    @GET("v1/profile")
    suspend fun profile(@Header("Authorization") bearerToken: String): ProfileEnvelope

    @PUT("v1/profile")
    suspend fun upsertProfile(
        @Header("Authorization") bearerToken: String,
        @Body request: UpsertProfileRequest
    ): ProfileEnvelope

    @GET("v1/courses")
    suspend fun courses(
        @Header("Authorization") bearerToken: String,
        @Query("limit") limit: Int = 50
    ): CourseCatalogEnvelope

    @GET("v1/profile/username-availability")
    suspend fun usernameAvailability(
        @Header("Authorization") bearerToken: String,
        @Query("username") username: String
    ): UsernameAvailabilityEnvelope

    @GET("v1/feed")
    suspend fun feed(
        @Header("Authorization") bearerToken: String,
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 24,
        @Query("cursor") cursor: String? = null
    ): FeedEnvelope
}
