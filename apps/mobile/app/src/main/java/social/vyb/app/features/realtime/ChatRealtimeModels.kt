package social.vyb.app.features.realtime

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ChatSocketEnvelope(
    val type: String,
    val conversationId: String? = null,
    val payload: JsonObject? = null
)

sealed interface ChatRealtimeEvent {
    data object Connecting : ChatRealtimeEvent
    data object Connected : ChatRealtimeEvent
    data class Disconnected(val retrying: Boolean) : ChatRealtimeEvent
    data class MessageChanged(val messageId: String?) : ChatRealtimeEvent
    data class ReadChanged(val messageId: String?) : ChatRealtimeEvent
    data object SyncRequired : ChatRealtimeEvent
    data class PeerTyping(
        val userId: String?,
        val membershipId: String?,
        val isTyping: Boolean
    ) : ChatRealtimeEvent
}
