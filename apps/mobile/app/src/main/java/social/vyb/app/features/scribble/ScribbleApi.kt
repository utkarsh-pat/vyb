package social.vyb.app.features.scribble

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

internal interface ScribbleApi {
    @GET("v1/games/scribble/socket-token")
    suspend fun socketToken(
        @Header("Authorization") authorization: String,
    ): ScribbleSocketToken

    @GET("v1/games/scribble/public-rooms")
    suspend fun publicRooms(
        @Header("Authorization") authorization: String,
        @Query("tenantId") tenantId: String,
    ): ScribbleCatalog
}
