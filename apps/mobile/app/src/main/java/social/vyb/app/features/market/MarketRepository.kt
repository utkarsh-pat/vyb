package social.vyb.app.features.market

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Retrofit
import social.vyb.app.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MarketRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trim().let { if (it.endsWith("/")) it else "$it/" })
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(MarketApi::class.java)

    suspend fun dashboard(): MarketDashboard = call { api.dashboard(bearer()) }

    suspend fun create(draft: MarketPostDraft): MarketDashboard = call {
        val isSale = draft.tab == "sale"
        api.create(
            bearer(),
            CreateMarketPost(
                tab = draft.tab,
                title = draft.title.trim(),
                category = draft.category.trim(),
                description = draft.description.trim(),
                campusSpot = draft.campusSpot.trim().ifBlank { null },
                condition = draft.condition.trim().ifBlank { null },
                priceAmount = draft.amount.takeIf { isSale },
                budgetAmount = draft.amount.takeUnless { isSale },
                budgetLabel = draft.amount.takeUnless { isSale }?.let { "Up to ₹$it" },
            ),
        ).dashboard
    }

    suspend fun toggleSave(listingId: String): MarketDashboard = call {
        api.toggleSave(bearer(), SaveMarketListing(listingId)).dashboard
    }

    suspend fun contact(target: MarketDetail, message: String): MarketDashboard = call {
        api.contact(
            bearer(),
            ContactMarketPost(
                targetId = target.id,
                targetType = if (target is MarketDetail.Listing) "listing" else "request",
                message = message.trim(),
            ),
        ).dashboard
    }

    suspend fun markSold(listingId: String): MarketDashboard = call {
        api.markSold(bearer(), listingId).dashboard
    }

    private suspend fun bearer(): String {
        val user = auth.currentUser ?: throw MarketException("Please sign in to use Campus Market.")
        return "Bearer ${user.idToken()}"
    }

    private suspend fun FirebaseUser.idToken(): String =
        suspendCancellableCoroutine { continuation ->
            getIdToken(false)
                .addOnSuccessListener { result ->
                    val token = result.token
                    if (token.isNullOrBlank()) {
                        continuation.resumeWithException(MarketException("Could not verify your session."))
                    } else {
                        continuation.resume(token)
                    }
                }
                .addOnFailureListener(continuation::resumeWithException)
        }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (error: MarketException) {
        throw error
    } catch (error: HttpException) {
        val backendMessage = runCatching {
            val body = error.response()?.errorBody()?.string().orEmpty()
            Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
        }.getOrNull()
        throw MarketException(backendMessage ?: "Market request failed (${error.code()}).", error)
    } catch (error: Exception) {
        throw MarketException(error.message ?: "Campus Market is unavailable.", error)
    }
}

class MarketException(message: String, cause: Throwable? = null) : Exception(message, cause)
