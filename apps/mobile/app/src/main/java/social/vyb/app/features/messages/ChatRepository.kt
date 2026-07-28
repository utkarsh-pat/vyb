package social.vyb.app.features.messages

import com.google.firebase.auth.FirebaseAuth
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import social.vyb.app.BuildConfig
import social.vyb.app.features.realtime.ChatRealtimeClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: ChatApi = Retrofit.Builder()
        .baseUrl(normalizeBaseUrl(BuildConfig.API_BASE_URL))
        .client(
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                        else HttpLoggingInterceptor.Level.NONE
                    }
                )
                .build()
        )
        .addConverterFactory(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }.asConverterFactory("application/json".toMediaType())
        )
        .build()
        .create(ChatApi::class.java)
    private val realtimeClient = ChatRealtimeClient(
        socketUrlProvider = { conversationId ->
            api.socketToken(bearer(), conversationId).wsUrl
        }
    )

    fun realtimeEvents(conversationId: String) =
        realtimeClient.observe(conversationId)

    fun sendTyping(conversationId: String, isTyping: Boolean): Boolean =
        realtimeClient.sendTyping(conversationId, isTyping)

    fun acknowledgeDelivered(conversationId: String, messageIds: List<String>): Boolean =
        realtimeClient.acknowledgeDelivered(conversationId, messageIds)

    suspend fun loadInbox(): List<ChatInboxItem> = apiCall {
        val response = api.inbox(bearer())
        val localIdentity = resolveLocalIdentity(response.viewer)
        response.items.map { preview ->
            val lastMessage = preview.lastMessage
            ChatInboxItem(
                id = preview.id,
                peerName = preview.peer.displayName,
                peerHandle = "@${preview.peer.username}",
                avatarUrl = preview.peer.avatarUrl,
                preview = when {
                    lastMessage == null -> "Start a conversation"
                    localIdentity == null || preview.peer.publicKey == null -> "Encrypted message"
                    else -> decryptOrPlaceholder(lastMessage, localIdentity, preview.peer.publicKey.publicKey)
                },
                timestamp = formatTimestamp(preview.lastActivityAt),
                unreadCount = preview.unreadCount,
                isOnline = preview.peer.isOnline
            )
        }
    }

    suspend fun loadConversation(conversationId: String): ConversationResult = apiCall {
        var response = api.conversation(bearer(), conversationId)
        val localIdentity = resolveOrProvisionIdentity(response.viewer)
        if (response.viewer.activeIdentity == null) {
            response = api.conversation(bearer(), conversationId)
        }
        val peerKey = response.conversation.peer.publicKey?.publicKey
            ?: error("This member has not set up secure chat yet.")
        val messages = response.conversation.messages.map { message ->
            val plaintext = runCatching { ChatCrypto.decrypt(message, localIdentity, peerKey) }.getOrNull()
            ChatMessageItem(
                id = message.id,
                body = plaintext ?: "Unable to decrypt this message on this device.",
                timestamp = formatTimestamp(message.createdAt),
                isMine = message.senderUserId == response.viewer.userId,
                isReadable = plaintext != null
            )
        }
        response.conversation.messages.lastOrNull()?.let { last ->
            runCatching {
                api.markRead(
                    bearer(),
                    conversationId,
                    MarkChatReadRequestDto(last.id)
                )
            }
        }
        ConversationResult(
            peerName = response.conversation.peer.displayName,
            peerHandle = "@${response.conversation.peer.username}",
            isOnline = response.conversation.peer.isOnline,
            messages = messages,
            viewerUserId = response.viewer.userId
        )
    }

    suspend fun sendText(conversationId: String, plaintext: String): ChatMessageItem = apiCall {
        val text = plaintext.trim()
        require(text.isNotEmpty()) { "Type a message first." }
        require(text.length <= 4_000) { "Message is too long." }

        val conversation = api.conversation(bearer(), conversationId)
        val identity = resolveOrProvisionIdentity(conversation.viewer)
        val peerKey = conversation.conversation.peer.publicKey?.publicKey
            ?: error("This member has not set up secure chat yet.")
        val encrypted = ChatCrypto.encrypt(text, identity, peerKey)
        val sent = api.sendMessage(
            bearer(),
            conversationId,
            SendChatMessageRequestDto(
                cipherText = encrypted.cipherText,
                cipherIv = encrypted.cipherIv,
                cipherAlgorithm = ChatCrypto.MESSAGE_ALGORITHM
            )
        ).item
        ChatMessageItem(
            id = sent.id,
            body = text,
            timestamp = formatTimestamp(sent.createdAt),
            isMine = true,
            isReadable = true
        )
    }

    suspend fun markRead(conversationId: String, messageId: String) {
        apiCall {
        api.markRead(bearer(), conversationId, MarkChatReadRequestDto(messageId))
        }
    }

    private suspend fun resolveOrProvisionIdentity(viewer: ChatViewerDto): ChatCrypto.LocalIdentity {
        val local = ChatCrypto.findLocalIdentity(viewer.userId)
        if (viewer.activeIdentity != null) {
            check(local?.publicKey == viewer.activeIdentity.publicKey) {
                "Secure chat is already linked to another device key. Restore or pair this device before sending."
            }
            return local
        }
        val created = ChatCrypto.getOrCreateLocalIdentity(viewer.userId)
        api.upsertIdentity(
            bearer(),
            UpsertChatIdentityRequestDto(publicKey = created.publicKey)
        )
        return created
    }

    private fun resolveLocalIdentity(viewer: ChatViewerDto): ChatCrypto.LocalIdentity? {
        val local = ChatCrypto.findLocalIdentity(viewer.userId) ?: return null
        return local.takeIf { it.publicKey == viewer.activeIdentity?.publicKey }
    }

    private suspend fun bearer(): String {
        val user = auth.currentUser ?: error("Your session expired. Please sign in again.")
        return "Bearer ${user.chatIdToken()}"
    }

    private fun decryptOrPlaceholder(
        message: ChatMessageDto,
        identity: ChatCrypto.LocalIdentity,
        peerKey: String
    ): String = runCatching {
        ChatCrypto.decrypt(message, identity, peerKey).replace(Regex("\\s+"), " ").take(90)
    }.getOrDefault("Encrypted message")

    private fun formatTimestamp(value: String): String = runCatching {
        val instant = Instant.parse(value)
        val local = instant.atZone(ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("h:mm a").format(local)
    }.getOrDefault(value)

    private suspend fun <T> apiCall(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val message = when (error.code()) {
            401 -> "Your session expired. Please sign in again."
            403 -> "Complete your profile before opening campus chat."
            404 -> "This conversation is no longer available."
            429 -> "Too many requests. Please wait and try again."
            else -> "Messages could not connect (${error.code()})."
        }
        throw IllegalStateException(message, error)
    }

    private fun normalizeBaseUrl(value: String): String =
        value.trim().let { if (it.endsWith("/")) it else "$it/" }
}

data class ConversationResult(
    val peerName: String,
    val peerHandle: String,
    val isOnline: Boolean,
    val messages: List<ChatMessageItem>,
    val viewerUserId: String
)
