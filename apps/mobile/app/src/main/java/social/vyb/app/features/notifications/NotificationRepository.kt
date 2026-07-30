package social.vyb.app.features.notifications

import com.google.firebase.auth.FirebaseAuth
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken

internal interface NotificationApi {
    @GET("v1/notifications")
    suspend fun list(
        @Header("Authorization") bearer: String,
        @Query("state") state: String,
        @Query("limit") limit: Int = 50
    ): NotificationInbox

    @PUT("v1/notifications/{notificationId}/read")
    suspend fun markRead(
        @Header("Authorization") bearer: String,
        @Path("notificationId") notificationId: String
    ): MarkNotificationReadResponse

    @PUT("v1/notifications/read-all")
    suspend fun markAllRead(
        @Header("Authorization") bearer: String,
        @Body body: MarkAllNotificationsReadRequest = MarkAllNotificationsReadRequest()
    ): MarkAllNotificationsReadResponse
}

class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: NotificationApi = VybNetwork.create()

    internal suspend fun list(state: String): NotificationInbox {
        require(state in setOf("all", "unread", "read"))
        return api.list(auth.requireBearerToken(), state)
    }

    suspend fun markRead(id: String): NotificationItem =
        api.markRead(auth.requireBearerToken(), id).item

    internal suspend fun markAllRead(): MarkAllNotificationsReadResponse =
        api.markAllRead(auth.requireBearerToken())
}
