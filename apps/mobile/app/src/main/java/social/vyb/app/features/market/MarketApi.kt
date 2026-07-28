package social.vyb.app.features.market

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

internal interface MarketApi {
    @GET("v1/market")
    suspend fun dashboard(
        @Header("Authorization") authorization: String,
    ): MarketDashboard

    @POST("v1/market")
    suspend fun create(
        @Header("Authorization") authorization: String,
        @Body body: CreateMarketPost,
    ): MarketMutationResponse

    @POST("v1/market/save")
    suspend fun toggleSave(
        @Header("Authorization") authorization: String,
        @Body body: SaveMarketListing,
    ): MarketMutationResponse

    @POST("v1/market/contact")
    suspend fun contact(
        @Header("Authorization") authorization: String,
        @Body body: ContactMarketPost,
    ): MarketMutationResponse

    @POST("v1/market/listings/{listingId}/sold")
    suspend fun markSold(
        @Header("Authorization") authorization: String,
        @Path("listingId") listingId: String,
    ): MarketMutationResponse
}
