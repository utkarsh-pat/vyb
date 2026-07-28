package social.vyb.app.features.funhub

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import social.vyb.app.BuildConfig
import java.util.concurrent.TimeUnit

class FunRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    private val api = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL.trim().let { if (it.endsWith("/")) it else "$it/" })
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                    else HttpLoggingInterceptor.Level.NONE
                })
                .build()
        )
        .addConverterFactory(
            Json { ignoreUnknownKeys = true; explicitNulls = false }
                .asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(FunApi::class.java)

    suspend fun loadHub(): Triple<NotificationInbox, ConnectDaily, QueensDaily> = safe {
        val token = bearer()
        coroutineScope {
            val inbox = async { api.notifications(token) }
            val connect = async { api.connectDaily(token) }
            val queens = async { api.queensDaily(token) }
            Triple(inbox.await(), connect.await(), queens.await())
        }
    }

    suspend fun readAll(): ReadAllResult = safe { api.readAll(bearer()) }
    suspend fun connectHint(sessionId: String, path: List<Coordinate>) =
        safe { api.connectHint(bearer(), ConnectMoveRequest(sessionId, path)) }
    suspend fun submitConnect(sessionId: String, path: List<Coordinate>) =
        safe { api.connectSubmit(bearer(), ConnectSubmitRequest(sessionId, path)) }
    suspend fun queensHint(sessionId: String, queens: List<Coordinate>, marks: List<Coordinate>) =
        safe { api.queensHint(bearer(), QueensHintRequest(sessionId, queens, marks)) }
    suspend fun submitQueens(sessionId: String, queens: List<Coordinate>) =
        safe { api.queensSubmit(bearer(), QueensSubmitRequest(sessionId, queens)) }

    private suspend fun bearer(): String {
        val user = auth.currentUser ?: error("Your session expired. Please sign in again.")
        return "Bearer ${user.idToken()}"
    }

    private suspend fun FirebaseUser.idToken(): String = suspendCancellableCoroutine { continuation ->
        getIdToken(false)
            .addOnSuccessListener { result ->
                result.token?.let(continuation::resume)
                    ?: continuation.resumeWithException(IllegalStateException("Firebase returned an empty ID token."))
            }
            .addOnFailureListener(continuation::resumeWithException)
    }

    private suspend fun <T> safe(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        throw IllegalStateException(
            when (error.code()) {
                401 -> "Your session expired. Please sign in again."
                403 -> "Your campus account cannot access this feature yet."
                404 -> "This daily challenge is unavailable."
                429 -> "Too many attempts. Please wait and try again."
                else -> "Vybnet services could not connect (${error.code()})."
            },
            error
        )
    }
}
