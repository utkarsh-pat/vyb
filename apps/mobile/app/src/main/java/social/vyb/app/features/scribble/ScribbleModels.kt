package social.vyb.app.features.scribble

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ScribbleSocketToken(
    val wsUrl: String,
    val expiresAt: Long,
    val tenantId: String,
    val viewer: ScribbleViewer,
)

@Serializable
data class ScribbleViewer(
    val userId: String = "",
    val membershipId: String = "",
    val username: String = "",
    val displayName: String = "",
)

@Serializable
data class ScribbleSettings(
    val drawTime: Int = 60,
    val rounds: Int = 3,
    val maxPlayers: Int = 8,
    val visibility: String = "private",
    val hintsEnabled: Boolean = true,
)

@Serializable
data class ScribbleDrawStep(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val color: String,
    val width: Float,
)

@Serializable
data class ScribblePlayer(
    val userId: String = "",
    val membershipId: String = "",
    val username: String = "",
    val displayName: String = "",
    val connected: Boolean = false,
    val score: Int = 0,
    val correctThisTurn: Boolean = false,
    val warnings: Int = 0,
    val isHost: Boolean = false,
    val isDrawer: Boolean = false,
)

@Serializable
data class ScribbleWordChoice(
    val id: String,
    val word: String,
    val difficulty: String = "easy",
    val multiplier: Double = 1.0,
)

@Serializable
data class ScribbleChatItem(
    val id: String,
    val kind: String = "system",
    val membershipId: String? = null,
    val displayName: String = "Scribble",
    val body: String = "",
    val createdAt: String = "",
)

@Serializable
data class ScribbleRoundScore(
    val membershipId: String = "",
    val displayName: String = "",
    val delta: Int = 0,
    val totalScore: Int = 0,
    val isDrawer: Boolean = false,
)

@Serializable
data class ScribbleRoundResult(
    val reason: String = "",
    val word: String? = null,
    val scores: List<ScribbleRoundScore> = emptyList(),
)

@Serializable
data class ScribbleSnapshot(
    val roomId: String,
    val displayName: String = "",
    val isSystemPublic: Boolean = false,
    val status: String = "LOBBY",
    val viewerMembershipId: String = "",
    val hostMembershipId: String = "",
    val currentDrawerMembershipId: String? = null,
    val round: Int = 0,
    val turn: Int = 0,
    val totalTurns: Int = 0,
    val timerEndsAt: String? = null,
    val settings: ScribbleSettings = ScribbleSettings(),
    val players: List<ScribblePlayer> = emptyList(),
    val currentWord: String? = null,
    val revealedWord: String? = null,
    val wordChoices: List<ScribbleWordChoice> = emptyList(),
    val hint: String? = null,
    val wordLengthHint: String? = null,
    val hintLetters: Int = 0,
    val drawing: List<ScribbleDrawStep> = emptyList(),
    val likeCount: Int = 0,
    val dislikeCount: Int = 0,
    val viewerLiked: Boolean = false,
    val viewerDisliked: Boolean = false,
    val chat: List<ScribbleChatItem> = emptyList(),
    val roundResult: ScribbleRoundResult? = null,
    val viewerCorrectThisTurn: Boolean = false,
    val invitePath: String = "",
)

@Serializable
data class ScribbleCatalogRoom(
    val roomId: String,
    val displayName: String = "",
    val isSystemPublic: Boolean = false,
    val hostName: String = "Host",
    val playerCount: Int = 0,
    val maxPlayers: Int = 8,
    val status: String = "LOBBY",
    val round: Int = 0,
    val drawTime: Int = 60,
    val rounds: Int = 3,
    val hintsEnabled: Boolean = true,
    val drawerName: String? = null,
)

@Serializable
data class ScribbleCatalog(val rooms: List<ScribbleCatalogRoom> = emptyList())

@Serializable
internal data class ScribbleSocketEnvelope(
    val type: String,
    val payload: JsonObject = JsonObject(emptyMap()),
)

internal sealed interface ScribblePendingAction {
    data class Create(val settings: ScribbleSettings) : ScribblePendingAction
    data class Join(val roomId: String) : ScribblePendingAction
}

internal sealed interface ScribbleRealtimeEvent {
    data class Connecting(val reconnecting: Boolean) : ScribbleRealtimeEvent
    data object Connected : ScribbleRealtimeEvent
    data class State(val snapshot: ScribbleSnapshot) : ScribbleRealtimeEvent
    data class Catalog(val rooms: List<ScribbleCatalogRoom>) : ScribbleRealtimeEvent
    data class Draw(val roomId: String?, val steps: List<ScribbleDrawStep>) : ScribbleRealtimeEvent
    data object CanvasCleared : ScribbleRealtimeEvent
    data class Notice(val message: String) : ScribbleRealtimeEvent
    data class Error(val message: String) : ScribbleRealtimeEvent
    data class Disconnected(val retrying: Boolean) : ScribbleRealtimeEvent
}

data class ScribbleUiState(
    val connection: String = "idle",
    val catalogLoading: Boolean = false,
    val rooms: List<ScribbleCatalogRoom> = emptyList(),
    val snapshot: ScribbleSnapshot? = null,
    val notice: String? = null,
    val error: String? = null,
)

internal fun normalizeScribbleRoomCode(value: String): String =
    value.trim().uppercase().filter(Char::isLetterOrDigit).take(12)

internal fun ScribbleSnapshot.withIncomingSteps(
    roomId: String?,
    steps: List<ScribbleDrawStep>,
): ScribbleSnapshot {
    if (roomId != null && roomId != this.roomId) return this
    if (steps.isEmpty()) return this
    return copy(drawing = (drawing + steps).takeLast(5_000))
}

internal fun mergeScribbleSnapshot(
    current: ScribbleSnapshot?,
    incoming: ScribbleSnapshot,
): ScribbleSnapshot =
    if (
        current != null &&
        current.roomId == incoming.roomId &&
        current.status == "PLAYING" &&
        incoming.status == "PLAYING" &&
        current.drawing.size > incoming.drawing.size
    ) {
        incoming.copy(drawing = current.drawing)
    } else {
        incoming
    }

internal val ScribbleSnapshot.viewerCanDraw: Boolean
    get() = status == "PLAYING" && viewerMembershipId == currentDrawerMembershipId

internal val ScribbleSnapshot.viewerIsHost: Boolean
    get() = viewerMembershipId == hostMembershipId

internal fun ScribbleSnapshot.visibleWord(): String = when {
    !currentWord.isNullOrBlank() -> currentWord
    !hint.isNullOrBlank() -> hint
    !wordLengthHint.isNullOrBlank() -> "Word lengths: $wordLengthHint"
    else -> "Waiting for word"
}
