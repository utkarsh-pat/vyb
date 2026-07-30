package social.vyb.app.features.funhub

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken

class FunRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    private val api: FunApi = VybNetwork.create()

    suspend fun loadHub(): Pair<ConnectDaily, QueensDaily> = safe {
        val token = bearer()
        coroutineScope {
            val connect = async { api.connectDaily(token) }
            val queens = async { api.queensDaily(token) }
            connect.await() to queens.await()
        }
    }

    suspend fun connectHint(sessionId: String, path: List<Coordinate>) =
        safe { api.connectHint(bearer(), ConnectMoveRequest(sessionId, path)) }
    suspend fun submitConnect(sessionId: String, path: List<Coordinate>) =
        safe { api.connectSubmit(bearer(), ConnectSubmitRequest(sessionId, path)) }
    suspend fun queensHint(sessionId: String, queens: List<Coordinate>, marks: List<Coordinate>) =
        safe { api.queensHint(bearer(), QueensHintRequest(sessionId, queens, marks)) }
    suspend fun submitQueens(sessionId: String, queens: List<Coordinate>) =
        safe { api.queensSubmit(bearer(), QueensSubmitRequest(sessionId, queens)) }

    private suspend fun bearer(): String = auth.requireBearerToken()

    private suspend fun <T> safe(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        throw IllegalStateException(
            when (error.code()) {
                401 -> "Your session expired. Please sign in again."
                403 -> "Your campus account cannot access this feature yet."
                404 -> "This daily challenge is unavailable."
                429 -> "Too many attempts. Please wait and try again."
                else -> "Vyb services could not connect (${error.code()})."
            },
            error
        )
    }
}
