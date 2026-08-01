package social.vyb.app.features.messages

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.HttpException
import social.vyb.app.data.RemotePost
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken
import social.vyb.app.features.realtime.ChatRealtimeClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChatRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: ChatApi = VybNetwork.create()
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

    suspend fun loadCommunityInbox(): List<CommunityInboxItem> = apiCall {
        api.communities(bearer()).communities
            .filter { it.isMember && it.membershipStatus != "left" && it.slug.isNotBlank() }
            .sortedWith(
                compareByDescending<social.vyb.app.features.hub.HubCommunity> { it.pinned }
                    .thenByDescending { it.isOfficial }
                    .thenBy { it.name.lowercase() }
            )
            .map {
                CommunityInboxItem(
                    id = it.id,
                    slug = it.slug,
                    name = it.name,
                    type = it.type,
                    memberCount = it.memberCount,
                    membershipRole = it.membershipRole,
                    isOfficial = it.isOfficial
                )
            }
    }

    suspend fun loadCommunityConversation(slug: String): CommunityConversationResult = apiCall {
        val normalizedSlug = slug.trim()
        require(normalizedSlug.isNotEmpty()) { "Choose a community first." }
        val authorization = bearer()
        val viewer = api.viewer(authorization)
        check(viewer.membershipSummary.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }
        val detail = api.community(authorization, normalizedSlug)
        check(
            detail.viewer.isMember ||
                detail.viewer.membershipStatus == "member" ||
                detail.community.isMember
        ) {
            "Join this community before opening its conversation."
        }
        val context = CommunityConversationContext(
            id = detail.community.id,
            slug = detail.community.slug,
            name = detail.community.name,
            type = detail.community.type,
            memberCount = detail.community.memberCount,
            tenantId = viewer.membershipSummary.tenantId,
            membershipId = viewer.membershipSummary.id,
            viewerUserId = viewer.user.id
        )
        val messages = api.communityMessages(
            authorization = authorization,
            tenantId = context.tenantId,
            communityId = context.id
        ).items
            .map { it.toCommunityMessage(context.viewerUserId) }
            .sortedBy(CommunityMessageItem::createdAt)
        CommunityConversationResult(context = context, messages = messages)
    }

    suspend fun sendCommunityText(
        context: CommunityConversationContext,
        plaintext: String
    ): CommunityMessageItem = apiCall {
        val text = plaintext.trim()
        require(text.isNotEmpty()) { "Type a message first." }
        require(text.length <= 4_000) { "Message is too long." }
        api.sendCommunityMessage(
            authorization = bearer(),
            body = SendCommunityMessageRequestDto(
                tenantId = context.tenantId,
                membershipId = context.membershipId,
                communityId = context.id,
                body = text
            )
        ).item.toCommunityMessage(context.viewerUserId)
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

    suspend fun sendVibeCard(
        conversationId: String,
        postId: String,
        title: String,
        body: String,
        mediaUrl: String?,
        authorUsername: String,
        authorDisplayName: String
    ): ChatMessageItem = apiCall {
        val payload = buildJsonObject {
            put("version", 1)
            put("type", "vibe_card")
            put("postId", postId)
            put("title", title.take(80))
            put("body", body.take(140))
            mediaUrl?.takeIf(String::isNotBlank)?.let {
                put("mediaUrl", it)
                put("thumbnailUrl", it)
            }
            put("authorUsername", authorUsername)
            put("authorDisplayName", authorDisplayName)
        }.toString()
        val conversation = api.conversation(bearer(), conversationId)
        val identity = resolveOrProvisionIdentity(conversation.viewer)
        val peerKey = conversation.conversation.peer.publicKey?.publicKey
            ?: error("This member has not set up secure chat yet.")
        val encrypted = ChatCrypto.encrypt(payload, identity, peerKey)
        val sent = api.sendMessage(
            bearer(),
            conversationId,
            SendChatMessageRequestDto(
                messageKind = "vibe_card",
                cipherText = encrypted.cipherText,
                cipherIv = encrypted.cipherIv,
                cipherAlgorithm = ChatCrypto.MESSAGE_ALGORITHM
            )
        ).item
        ChatMessageItem(
            id = sent.id,
            body = payload,
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
        val local = ChatCrypto.findLocalIdentity(context, viewer.userId)
        if (viewer.activeIdentity != null) {
            check(local?.publicKey == viewer.activeIdentity.publicKey) {
                "Secure chat is already linked to another device key. Restore or pair this device before sending."
            }
            return local
        }
        val created = ChatCrypto.getOrCreateLocalIdentity(context, viewer.userId)
        api.upsertIdentity(
            bearer(),
            UpsertChatIdentityRequestDto(publicKey = created.publicKey)
        )
        return created
    }

    private fun resolveLocalIdentity(viewer: ChatViewerDto): ChatCrypto.LocalIdentity? {
        val local = ChatCrypto.findLocalIdentity(context, viewer.userId) ?: return null
        return local.takeIf { it.publicKey == viewer.activeIdentity?.publicKey }
    }

    private suspend fun bearer(): String = auth.requireBearerToken()

    private fun decryptOrPlaceholder(
        message: ChatMessageDto,
        identity: ChatCrypto.LocalIdentity,
        peerKey: String
    ): String = runCatching {
        ChatCrypto.decrypt(message, identity, peerKey).replace(Regex("\\s+"), " ").take(90)
    }.getOrDefault("Encrypted message")

    private suspend fun <T> apiCall(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val message = when (error.code()) {
            401 -> "Your session expired. Please sign in again."
            403 -> "Complete your profile before opening campus chat."
            404 -> "This conversation or community is no longer available."
            429 -> "Too many requests. Please wait and try again."
            else -> "Messages could not connect (${error.code()})."
        }
        throw IllegalStateException(message, error)
    }

}

data class ConversationResult(
    val peerName: String,
    val peerHandle: String,
    val isOnline: Boolean,
    val messages: List<ChatMessageItem>,
    val viewerUserId: String
)

internal fun RemotePost.toCommunityMessage(viewerUserId: String): CommunityMessageItem {
    val anonymous = isAnonymous || author.isAnonymous
    return CommunityMessageItem(
        id = id,
        body = body.ifBlank { title },
        authorName = if (anonymous) "Anonymous" else author.displayName,
        authorHandle = if (anonymous) "@anonymous" else "@${author.username}",
        timestamp = formatTimestamp(createdAt),
        createdAt = createdAt,
        isMine = !anonymous && author.userId == viewerUserId,
        isAnonymous = anonymous,
        reactionCount = reactions,
        replyCount = comments
    )
}

internal fun mergeCommunityMessages(
    current: List<CommunityMessageItem>,
    incoming: List<CommunityMessageItem>
): List<CommunityMessageItem> =
    (current + incoming)
        .distinctBy(CommunityMessageItem::id)
        .sortedBy(CommunityMessageItem::createdAt)

private fun formatTimestamp(value: String): String = runCatching {
    val instant = Instant.parse(value)
    val local = instant.atZone(ZoneId.systemDefault())
    DateTimeFormatter.ofPattern("h:mm a").format(local)
}.getOrDefault(value)
