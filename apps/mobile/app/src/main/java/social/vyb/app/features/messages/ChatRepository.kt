package social.vyb.app.features.messages

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import retrofit2.HttpException
import social.vyb.app.data.RemotePost
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken
import social.vyb.app.features.realtime.ChatRealtimeClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class ChatRepository(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    fun currentViewerName(): String = auth.currentUser?.displayName
        ?.trim()?.takeIf(String::isNotBlank)
        ?: auth.currentUser?.email?.substringBefore('@')
        ?: "A participant"
    private val chatPreferences by lazy {
        context.getSharedPreferences("vyb_chat_preferences", Context.MODE_PRIVATE)
    }

    fun loadDefaultDuration(conversationId: String): String {
        val userId = auth.currentUser?.uid ?: "anonymous"
        return chatPreferences.getString("ttl:$userId:$conversationId", "30d")
            ?.takeIf { it in SUPPORTED_TTL_KEYS }
            ?: "30d"
    }

    fun saveDefaultDuration(conversationId: String, durationKey: String) {
        require(durationKey in SUPPORTED_TTL_KEYS) { "Unsupported chat expiry timer." }
        val userId = auth.currentUser?.uid ?: "anonymous"
        chatPreferences.edit().putString("ttl:$userId:$conversationId", durationKey).apply()
    }

    private data class ConversationSession(
        val viewerUserId: String,
        val identity: ChatCrypto.LocalIdentity,
        val peerPublicKey: String,
        val peerName: String,
        val peerHandle: String,
        val peerAvatarUrl: String?,
        val isOnline: Boolean
    )

    private val api: ChatApi = VybNetwork.create()
    private val json = Json { ignoreUnknownKeys = true }
    private val conversationSessions = ConcurrentHashMap<String, ConversationSession>()
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

    internal suspend fun heartbeatPresence(): ChatPresenceHeartbeatResponseDto = apiCall {
        api.heartbeatPresence(bearer())
    }

    suspend fun loadInbox(): List<ChatInboxItem> = apiCall {
        val response = api.inbox(bearer())
        val localIdentity = resolveLocalIdentity(response.viewer)
        response.items.distinctBy(ChatPreviewDto::id).map { preview ->
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

    suspend fun openDirectConversation(username: String): String = apiCall {
        val normalizedUsername = username.trim().removePrefix("@")
        require(normalizedUsername.isNotEmpty()) { "Choose a member first." }
        api.createDirectConversation(
            authorization = bearer(),
            body = CreateDirectChatRequestDto(recipientUsername = normalizedUsername)
        ).conversation.id
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
        val session = ConversationSession(
            viewerUserId = response.viewer.userId,
            identity = localIdentity,
            peerPublicKey = peerKey,
            peerName = response.conversation.peer.displayName,
            peerHandle = "@${response.conversation.peer.username}",
            peerAvatarUrl = response.conversation.peer.avatarUrl,
            isOnline = response.conversation.peer.isOnline
        )
        conversationSessions[conversationId] = session
        val peerReadIndex = response.conversation.peerLastReadMessageId?.let { readId ->
            response.conversation.messages.indexOfFirst { it.id == readId }
        } ?: -1
        val messages = response.conversation.messages.mapIndexed { index, message ->
            val plaintext = runCatching { ChatCrypto.decrypt(message, localIdentity, peerKey) }.getOrNull()
            ChatMessageItem(
                id = message.id,
                body = plaintext ?: "Unable to decrypt this message on this device.",
                timestamp = formatTimestamp(message.createdAt),
                isMine = message.senderUserId == response.viewer.userId,
                isReadable = plaintext != null,
                deliveryState = if (message.senderUserId == response.viewer.userId && index <= peerReadIndex) {
                    ChatDeliveryState.Read
                } else {
                    ChatDeliveryState.Sent
                },
                expiresAt = message.expiresAt,
                messageKind = message.messageKind,
                attachment = message.attachment
            )
        }
        response.conversation.messages.lastOrNull()?.takeIf { last ->
            last.senderUserId != response.viewer.userId &&
                last.id != response.conversation.lastReadMessageId
        }?.let { last ->
            runCatching {
                api.markRead(
                    bearer(),
                    conversationId,
                    MarkChatReadRequestDto(last.id)
                )
            }
        }
        ConversationResult(
            peerName = session.peerName,
            peerHandle = session.peerHandle,
            peerAvatarUrl = session.peerAvatarUrl,
            isOnline = session.isOnline,
            messages = messages,
            viewerUserId = response.viewer.userId
        )
    }

    suspend fun sendText(
        conversationId: String,
        plaintext: String,
        durationKey: String = "30d"
    ): ChatMessageItem = apiCall {
        val text = plaintext.trim()
        require(text.isNotEmpty()) { "Type a message first." }
        require(text.length <= 4_000) { "Message is too long." }

        val session = conversationSessions[conversationId] ?: loadConversationSession(conversationId)
        val encrypted = ChatCrypto.encrypt(text, session.identity, session.peerPublicKey)
        val sent = api.sendMessage(
            bearer(),
            conversationId,
            SendChatMessageRequestDto(
                cipherText = encrypted.cipherText,
                cipherIv = encrypted.cipherIv,
                cipherAlgorithm = ChatCrypto.MESSAGE_ALGORITHM,
                durationKey = durationKey
            )
        ).item
        ChatMessageItem(
            id = sent.id,
            body = text,
            timestamp = formatTimestamp(sent.createdAt),
            isMine = true,
            isReadable = true,
            deliveryState = ChatDeliveryState.Sent,
            expiresAt = sent.expiresAt,
            messageKind = sent.messageKind,
            attachment = sent.attachment
        )
    }

    suspend fun receiveRealtimeMessage(
        conversationId: String,
        item: JsonObject?
    ): ChatMessageItem? = apiCall {
        val message = item?.let { json.decodeFromJsonElement<ChatMessageDto>(it) } ?: return@apiCall null
        val session = conversationSessions[conversationId] ?: return@apiCall null
        val plaintext = runCatching {
            ChatCrypto.decrypt(message, session.identity, session.peerPublicKey)
        }.getOrNull()
        val isMine = message.senderUserId == session.viewerUserId
        if (!isMine) {
            runCatching { Instant.parse(message.createdAt).toEpochMilli() }
                .onSuccess { ChatTelemetry.realtimeArrival(it, message.id) }
        }
        ChatMessageItem(
            id = message.id,
            body = plaintext ?: "Unable to decrypt this message on this device.",
            timestamp = formatTimestamp(message.createdAt),
            isMine = isMine,
            isReadable = plaintext != null,
            deliveryState = ChatDeliveryState.Sent,
            expiresAt = message.expiresAt,
            messageKind = message.messageKind,
            attachment = message.attachment
        )
    }

    suspend fun sendMedia(
        conversationId: String,
        source: Uri,
        fileName: String,
        mimeType: String,
        width: Int? = null,
        height: Int? = null,
        durationMs: Int? = null,
        viewOnce: Boolean = false,
        caption: String = "",
        durationKey: String = "30d"
    ): ChatMessageItem = apiCall {
        require(
            mimeType.startsWith("image/") || mimeType.startsWith("video/") || mimeType.startsWith("audio/")
        ) { "Choose an image, video, or audio file." }
        val originalBytes = context.contentResolver.openInputStream(source)?.use { it.readBytes() }
            ?: error("The selected media could not be opened.")
        val session = conversationSessions[conversationId] ?: loadConversationSession(conversationId)
        val encryptedAttachment = ChatCrypto.encryptAttachment(
            originalBytes,
            session.identity,
            session.peerPublicKey
        )
        val uploaded = api.uploadAttachment(
            bearer(),
            UploadChatAttachmentRequestDto(
                fileName = fileName,
                mimeType = mimeType,
                base64Data = Base64.encodeToString(encryptedAttachment.bytes, Base64.NO_WRAP),
                width = width,
                height = height,
                durationMs = durationMs,
                viewOnce = viewOnce && !mimeType.startsWith("audio/"),
                cipherAlgorithm = ChatCrypto.ATTACHMENT_ALGORITHM,
                cipherIv = encryptedAttachment.cipherIv,
                senderPublicKey = encryptedAttachment.senderPublicKey,
                recipientPublicKey = encryptedAttachment.recipientPublicKey
            )
        ).attachment
        val plaintext = caption.trim().ifBlank {
            when {
                mimeType.startsWith("audio/") -> "Voice note"
                mimeType.startsWith("video/") -> "Video"
                else -> "Photo"
            }
        }
        val encryptedText = ChatCrypto.encrypt(plaintext, session.identity, session.peerPublicKey)
        val sent = api.sendMessage(
            bearer(),
            conversationId,
            SendChatMessageRequestDto(
                messageKind = "image",
                cipherText = encryptedText.cipherText,
                cipherIv = encryptedText.cipherIv,
                cipherAlgorithm = ChatCrypto.MESSAGE_ALGORITHM,
                attachment = uploaded,
                durationKey = durationKey
            )
        ).item
        val cached = cacheDecryptedAttachment(sent.id, mimeType, originalBytes)
        ChatMessageItem(
            id = sent.id,
            body = plaintext,
            timestamp = formatTimestamp(sent.createdAt),
            isMine = true,
            isReadable = true,
            deliveryState = ChatDeliveryState.Sent,
            expiresAt = sent.expiresAt,
            messageKind = sent.messageKind,
            attachment = sent.attachment,
            localMediaUri = cached.toURI().toString()
        )
    }

    suspend fun loadAttachment(
        conversationId: String,
        message: ChatMessageItem,
        consumeViewOnce: Boolean = false
    ): String = apiCall {
        val attachment = requireNotNull(message.attachment) { "This message has no media." }
        val cached = cachedAttachment(message.id, attachment.mimeType)
        val localUri = if (cached != null) {
            cached.toURI().toString()
        } else {
            val session = conversationSessions[conversationId] ?: loadConversationSession(conversationId)
            val encryptedBytes = api.downloadAttachment(bearer(), message.id).use { it.bytes() }
            val plaintext = ChatCrypto.decryptAttachment(
                encryptedBytes,
                attachment,
                session.identity,
                session.peerPublicKey
            )
            cacheDecryptedAttachment(message.id, attachment.mimeType, plaintext).toURI().toString()
        }
        if (consumeViewOnce && attachment.viewOnce && !message.isMine) {
            api.updateMessageLifecycle(
                bearer(),
                message.id,
                UpdateChatMessageLifecycleRequestDto(consumeViewOnce = true)
            )
        }
        localUri
    }

    private fun cachedAttachment(messageId: String, mimeType: String): File? {
        val file = File(File(context.cacheDir, "chat-media"), "$messageId.${extensionFor(mimeType)}")
        return file.takeIf(File::isFile)
    }

    private fun cacheDecryptedAttachment(messageId: String, mimeType: String, bytes: ByteArray): File {
        val directory = File(context.cacheDir, "chat-media").apply { mkdirs() }
        return File(directory, "$messageId.${extensionFor(mimeType)}").apply { writeBytes(bytes) }
    }

    private fun extensionFor(mimeType: String): String = when {
        mimeType == "image/png" -> "png"
        mimeType == "image/webp" -> "webp"
        mimeType.startsWith("image/") -> "jpg"
        mimeType == "video/webm" -> "webm"
        mimeType.startsWith("video/") -> "mp4"
        mimeType == "audio/webm" -> "webm"
        mimeType == "audio/ogg" -> "ogg"
        else -> "m4a"
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
            isReadable = true,
            messageKind = sent.messageKind,
            attachment = sent.attachment
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

    private suspend fun loadConversationSession(conversationId: String): ConversationSession {
        var response = api.conversation(bearer(), conversationId)
        val identity = resolveOrProvisionIdentity(response.viewer)
        if (response.viewer.activeIdentity == null) {
            response = api.conversation(bearer(), conversationId)
        }
        val peerKey = response.conversation.peer.publicKey?.publicKey
            ?: error("This member has not set up secure chat yet.")
        return ConversationSession(
            viewerUserId = response.viewer.userId,
            identity = identity,
            peerPublicKey = peerKey,
            peerName = response.conversation.peer.displayName,
            peerHandle = "@${response.conversation.peer.username}",
            peerAvatarUrl = response.conversation.peer.avatarUrl,
            isOnline = response.conversation.peer.isOnline
        ).also { conversationSessions[conversationId] = it }
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

    private companion object {
        val SUPPORTED_TTL_KEYS = setOf("instant", "1h", "24h", "7d", "30d", "90d")
    }

}

data class ConversationResult(
    val peerName: String,
    val peerHandle: String,
    val peerAvatarUrl: String?,
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
