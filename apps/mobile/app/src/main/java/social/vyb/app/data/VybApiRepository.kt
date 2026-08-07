package social.vyb.app.data

import com.google.firebase.auth.FirebaseAuth
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireIdToken

class VybApiRepository {
    private val api: ApiService = VybNetwork.create(readTimeoutSeconds = 15)

    suspend fun loadHomeFeed(cursor: String? = null): HomeFeedResult {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Your session expired. Please sign in again.")
        val token = bootstrapBackendSession(user)
        val bearer = "Bearer $token"
        val me = api.me(bearer)
        check(me.membershipSummary.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }
        val feed = api.feed(bearer, me.membershipSummary.tenantId, cursor = cursor)
        return HomeFeedResult(me, feed)
    }

    suspend fun loadAppSession(): AppSessionResult {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Your session expired. Please sign in again.")
        val bearer = "Bearer ${bootstrapBackendSession(user)}"
        val profile = api.profile(bearer)
        if (!profile.profileCompleted) return AppSessionResult(profile = profile)

        val me = api.me(bearer)
        check(me.membershipSummary.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }
        return AppSessionResult(
            profile = profile,
            home = HomeFeedResult(
                me = me,
                feed = api.feed(bearer, me.membershipSummary.tenantId)
            )
        )
    }

    suspend fun completeProfile(request: UpsertProfileRequest): ProfileEnvelope {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Your session expired. Please sign in again.")
        val bearer = "Bearer ${bootstrapBackendSession(user)}"
        return api.upsertProfile(bearer, request)
    }

    suspend fun loadOnboardingCatalog(): List<CourseCatalogItem> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Your session expired. Please sign in again.")
        val bearer = "Bearer ${bootstrapBackendSession(user)}"
        return api.courses(bearer).items
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
            ?: error("Your session expired. Please sign in again.")
        val bearer = "Bearer ${bootstrapBackendSession(user)}"
        return api.usernameAvailability(bearer, username).available
    }

    private suspend fun bootstrapBackendSession(
        user: com.google.firebase.auth.FirebaseUser
    ): String {
        var token = user.requireIdToken(forceRefresh = false)
        var response = api.bootstrapSession(
            SessionBootstrapRequest(
                idToken = token,
                displayName = user.displayName
            )
        )

        if (response.code() == 409) {
            token = user.requireIdToken(forceRefresh = true)
            response = api.bootstrapSession(
                SessionBootstrapRequest(
                    idToken = token,
                    displayName = user.displayName
                )
            )
        }

        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        return token
    }

}

data class HomeFeedResult(
    val me: MeEnvelope,
    val feed: FeedEnvelope
)

data class AppSessionResult(
    val profile: ProfileEnvelope,
    val home: HomeFeedResult? = null
)
