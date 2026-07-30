package social.vyb.app.features.funhub

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface FunApi {
    @GET("v1/games/connect/daily")
    suspend fun connectDaily(
        @Header("Authorization") bearer: String,
        @Query("leaderboard") leaderboard: String = "on"
    ): ConnectDaily

    @POST("v1/games/connect/hint")
    suspend fun connectHint(
        @Header("Authorization") bearer: String,
        @Body request: ConnectMoveRequest
    ): ConnectHint

    @POST("v1/games/connect/submit")
    suspend fun connectSubmit(
        @Header("Authorization") bearer: String,
        @Body request: ConnectSubmitRequest
    ): ConnectSubmitResult

    @GET("v1/games/queens/daily")
    suspend fun queensDaily(
        @Header("Authorization") bearer: String,
        @Query("leaderboard") leaderboard: String = "on"
    ): QueensDaily

    @POST("v1/games/queens/hint")
    suspend fun queensHint(
        @Header("Authorization") bearer: String,
        @Body request: QueensHintRequest
    ): QueensHint

    @POST("v1/games/queens/submit")
    suspend fun queensSubmit(
        @Header("Authorization") bearer: String,
        @Body request: QueensSubmitRequest
    ): QueensSubmitResult
}
