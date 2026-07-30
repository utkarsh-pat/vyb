package social.vyb.app.features.messages

import kotlinx.serialization.Serializable
import social.vyb.app.data.RemotePost

@Serializable
internal data class ChatSocketTokenDto(
    val wsUrl: String,
    val expiresAt: Long
)

@Serializable
internal data class ChatIdentityDto(
    val id: String,
    val userId: String,
    val membershipId: String,
    val publicKey: String,
    val algorithm: String,
    val keyVersion: Int,
    val updatedAt: String
)

@Serializable
internal data class ChatViewerDto(
    val userId: String,
    val membershipId: String,
    val activeIdentity: ChatIdentityDto? = null
)

@Serializable
internal data class ChatPeerDto(
    val userId: String,
    val membershipId: String,
    val username: String,
    val displayName: String,
    val course: String? = null,
    val stream: String? = null,
    val avatarUrl: String? = null,
    val publicKey: ChatIdentityDto? = null,
    val isOnline: Boolean = false,
    val lastActiveAt: String? = null
)

@Serializable
internal data class ChatMessageDto(
    val id: String,
    val conversationId: String,
    val senderUserId: String,
    val senderMembershipId: String,
    val senderIdentityId: String,
    val messageKind: String,
    val cipherText: String,
    val cipherIv: String,
    val cipherAlgorithm: String,
    val replyToMessageId: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val expiresAt: String? = null,
    val isStarred: Boolean = false,
    val isSaved: Boolean = false
)

@Serializable
internal data class ChatPreviewDto(
    val id: String,
    val tenantId: String,
    val kind: String,
    val peer: ChatPeerDto,
    val lastMessage: ChatMessageDto? = null,
    val lastActivityAt: String,
    val unreadCount: Int = 0
)

@Serializable
internal data class ChatInboxDto(
    val viewer: ChatViewerDto,
    val items: List<ChatPreviewDto> = emptyList()
)

@Serializable
internal data class ChatConversationDto(
    val id: String,
    val tenantId: String,
    val kind: String,
    val peer: ChatPeerDto,
    val messages: List<ChatMessageDto> = emptyList(),
    val lastReadMessageId: String? = null,
    val lastReadAt: String? = null,
    val peerLastReadMessageId: String? = null,
    val peerLastReadAt: String? = null
)

@Serializable
internal data class ChatConversationEnvelopeDto(
    val viewer: ChatViewerDto,
    val conversation: ChatConversationDto
)

@Serializable
internal data class SendChatMessageRequestDto(
    val messageKind: String = "text",
    val cipherText: String,
    val cipherIv: String,
    val cipherAlgorithm: String,
    val durationKey: String = "30d"
)

@Serializable
internal data class SendChatMessageResponseDto(
    val item: ChatMessageDto,
    val conversationPreview: ChatPreviewDto
)

@Serializable
internal data class MarkChatReadRequestDto(
    val messageId: String,
    val exposeReceipt: Boolean = true
)

@Serializable
internal data class MarkChatReadResponseDto(
    val conversationId: String,
    val messageId: String,
    val readAt: String,
    val receiptExposed: Boolean
)

@Serializable
internal data class UpsertChatIdentityRequestDto(
    val publicKey: String,
    val algorithm: String = ChatCrypto.IDENTITY_ALGORITHM,
    val keyVersion: Int = 1
)

@Serializable
internal data class UpsertChatIdentityResponseDto(val identity: ChatIdentityDto)

@Serializable
internal data class ChatCipherEnvelope(
    val version: Int = 1,
    val cipherText: String,
    val iv: String,
    val algorithm: String = ChatCrypto.MESSAGE_ALGORITHM,
    val senderPublicKey: String,
    val recipientPublicKey: String
)

data class ChatInboxItem(
    val id: String,
    val peerName: String,
    val peerHandle: String,
    val avatarUrl: String?,
    val preview: String,
    val timestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean
)

data class ChatMessageItem(
    val id: String,
    val body: String,
    val timestamp: String,
    val isMine: Boolean,
    val isReadable: Boolean
)

@Serializable
internal data class SendCommunityMessageRequestDto(
    val tenantId: String,
    val membershipId: String,
    val communityId: String,
    val placement: String = "feed",
    val kind: String = "text",
    val title: String = "Community message",
    val body: String,
    val visibility: String = "community",
    val isAnonymous: Boolean = false,
    val allowAnonymousComments: Boolean = true
)

@Serializable
internal data class CommunityMessageEnvelopeDto(val item: RemotePost)

data class CommunityInboxItem(
    val id: String,
    val slug: String,
    val name: String,
    val type: String,
    val memberCount: Int,
    val membershipRole: String?,
    val isOfficial: Boolean
)

data class CommunityConversationContext(
    val id: String,
    val slug: String,
    val name: String,
    val type: String,
    val memberCount: Int,
    val tenantId: String,
    val membershipId: String,
    val viewerUserId: String
)

data class CommunityMessageItem(
    val id: String,
    val body: String,
    val authorName: String,
    val authorHandle: String,
    val timestamp: String,
    val createdAt: String,
    val isMine: Boolean,
    val isAnonymous: Boolean,
    val reactionCount: Int,
    val replyCount: Int
)

data class CommunityConversationResult(
    val context: CommunityConversationContext,
    val messages: List<CommunityMessageItem>
)
