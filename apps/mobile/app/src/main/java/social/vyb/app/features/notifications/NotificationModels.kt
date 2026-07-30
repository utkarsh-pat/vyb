package social.vyb.app.features.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationCopy(
    val title: String,
    val body: String,
    @SerialName("cta_label") val ctaLabel: String = "",
    val href: String = ""
)

@Serializable
data class NotificationReadState(
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("seen_at") val seenAt: String? = null,
    @SerialName("archived_at") val archivedAt: String? = null
)

@Serializable
data class NotificationItem(
    val id: String,
    @SerialName("event_key") val eventKey: String = "",
    val copy: NotificationCopy,
    val state: NotificationReadState = NotificationReadState(),
    val category: String = "updates",
    @SerialName("created_at") val createdAt: String
)

@Serializable
internal data class NotificationInbox(
    val tenantId: String,
    val items: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val nextCursor: String? = null
)

@Serializable
internal data class MarkNotificationReadResponse(val item: NotificationItem)

@Serializable
internal data class MarkAllNotificationsReadRequest(val category: String? = null)

@Serializable
internal data class MarkAllNotificationsReadResponse(
    val updatedCount: Int,
    val readAt: String
)
