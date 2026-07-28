package social.vyb.app.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class SessionBootstrapRequest(
    val idToken: String,
    val displayName: String? = null
)

@Serializable
data class SessionBootstrapEnvelope(
    val nextPath: String? = null
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
    val title: String = "",
    val body: String = "",
    val kind: String = "text",
    val mediaUrl: String? = null,
    val location: String? = null,
    val reactions: Int = 0,
    val comments: Int = 0,
    val createdAt: String,
    val author: RemoteAuthor
)

@Serializable
data class RemoteAuthor(
    val userId: String? = null,
    val username: String = "member",
    val displayName: String = "Vybnet member"
)

interface ApiService {
    @POST("v1/auth/session/bootstrap")
    suspend fun bootstrapSession(
        @Body request: SessionBootstrapRequest
    ): Response<SessionBootstrapEnvelope>

    @GET("v1/me")
    suspend fun me(@Header("Authorization") bearerToken: String): MeEnvelope

    @GET("v1/feed")
    suspend fun feed(
        @Header("Authorization") bearerToken: String,
        @Query("tenantId") tenantId: String,
        @Query("limit") limit: Int = 24
    ): FeedEnvelope
}
