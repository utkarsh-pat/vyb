package social.vyb.app.features.hub

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import social.vyb.app.BuildConfig
import java.util.concurrent.TimeUnit

class CampusHubRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trim().let { if (it.endsWith("/")) it else "$it/" })
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
                    }
                )
                .build()
        )
        .addConverterFactory(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(CampusHubApi::class.java)

    suspend fun loadEvents(): List<HubEvent> = call {
        val dashboard = api.events(bearer())
        (dashboard.events + dashboard.hostedEvents).distinctBy(HubEvent::id)
    }

    suspend fun loadEvent(eventId: String): HubEvent = call {
        api.event(bearer(), eventId).item
    }

    suspend fun toggleEventSave(eventId: String): List<HubEvent> = call {
        val dashboard = api.toggleEventSave(bearer(), eventId).dashboard
        (dashboard.events + dashboard.hostedEvents).distinctBy(HubEvent::id)
    }

    suspend fun registerEvent(eventId: String): List<HubEvent> = call {
        val dashboard = api.registerEvent(
            bearer(),
            eventId,
            RegisterEventRequestDto(eventId = eventId)
        ).dashboard
        (dashboard.events + dashboard.hostedEvents).distinctBy(HubEvent::id)
    }

    suspend fun loadResources(): List<HubResource> = call {
        val token = bearer()
        val tenantId = api.me(token).membershipSummary.tenantId
        api.resources(token, tenantId).items.filter { it.status == "published" }
    }

    suspend fun loadCommunities(): List<HubCommunity> = call {
        api.communities(bearer()).communities
    }

    suspend fun loadCommunity(slug: String): Pair<CommunityDetailDto, List<HubCommunityMember>> = call {
        val token = bearer()
        val detail = api.community(token, slug)
        val members = api.communityMembers(token, slug).items
        detail to members
    }

    private suspend fun bearer(): String {
        val user = auth.currentUser ?: error("Your session expired. Please sign in again.")
        return "Bearer ${user.hubIdToken()}"
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val message = when (error.code()) {
            400 -> "This action needs more information."
            401 -> "Your session expired. Please sign in again."
            403 -> "Complete your campus profile to access this section."
            404 -> "This campus item is no longer available."
            429 -> "Too many requests. Please try again shortly."
            in 500..599 -> "Campus Hub is temporarily unavailable."
            else -> "Campus Hub could not connect (${error.code()})."
        }
        throw IllegalStateException(message, error)
    }
}

private suspend fun FirebaseUser.hubIdToken(): String =
    suspendCancellableCoroutine { continuation ->
        getIdToken(false)
            .addOnSuccessListener { result ->
                result.token?.let(continuation::resume)
                    ?: continuation.resumeWithException(
                        IllegalStateException("Firebase returned an empty ID token.")
                    )
            }
            .addOnFailureListener(continuation::resumeWithException)
    }
