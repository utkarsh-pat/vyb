package social.vyb.app.features.funhub

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface FunApi {
    @GET("api/notifications")
    suspend fun notifications(
        @Header("Authorization") bearer: String,
        @Query("state") state: String = "all",
        @Query("limit") limit: Int = 30
    ): NotificationInbox

    @POST("api/notifications/read-all")
    suspend fun readAll(
        @Header("Authorization") bearer: String,
        @Body request: ReadAllRequest = ReadAllRequest()
    ): ReadAllResult

    @GET("api/games/connect/daily")
    suspend fun connectDaily(
        @Header("Authorization") bearer: String,
        @Query("leaderboard") leaderboard: String = "on"
    ): ConnectDaily

    @POST("api/games/connect/hint")
    suspend fun connectHint(
        @Header("Authorization") bearer: String,
        @Body request: ConnectMoveRequest
    ): ConnectHint

    @POST("api/games/connect/submit")
    suspend fun connectSubmit(
        @Header("Authorization") bearer: String,
        @Body request: ConnectSubmitRequest
    ): ConnectSubmitResult

    @GET("api/games/queens/daily")
    suspend fun queensDaily(
        @Header("Authorization") bearer: String,
        @Query("leaderboard") leaderboard: String = "on"
    ): QueensDaily

    @POST("api/games/queens/hint")
    suspend fun queensHint(
        @Header("Authorization") bearer: String,
        @Body request: QueensHintRequest
    ): QueensHint

    @POST("api/games/queens/submit")
    suspend fun queensSubmit(
        @Header("Authorization") bearer: String,
        @Body request: QueensSubmitRequest
    ): QueensSubmitResult
}
