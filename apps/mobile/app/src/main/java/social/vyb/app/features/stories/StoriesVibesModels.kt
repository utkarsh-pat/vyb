package social.vyb.app.features.stories

import kotlinx.serialization.Serializable

@Serializable
data class StoriesEnvelope(
    val items: List<StoryItem> = emptyList()
)

@Serializable
data class StoryItem(
    val id: String,
    val tenantId: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val mediaType: String,
    val mediaUrl: String,
    val compositionJson: String? = null,
    val caption: String = "",
    val createdAt: String,
    val expiresAt: String,
    val isOwn: Boolean = false,
    val reactions: Int = 0,
    val viewerHasLiked: Boolean = false,
    val viewerHasSeen: Boolean = false
)

@Serializable
data class VibesEnvelope(
    val tenantId: String,
    val communityId: String? = null,
    val items: List<VibeItem> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class VibeItem(
    val id: String,
    val tenantId: String,
    val communityId: String? = null,
    val userId: String? = null,
    val membershipId: String? = null,
    val placement: String = "vibe",
    val kind: String = "video",
    val mediaUrl: String? = null,
    val media: List<VibeMedia> = emptyList(),
    val location: String? = null,
    val title: String = "",
    val body: String = "",
    val reactions: Int = 0,
    val comments: Int = 0,
    val savedCount: Int = 0,
    val isSaved: Boolean = false,
    val viewerCanManage: Boolean = false,
    val viewerReactionType: String? = null,
    val createdAt: String,
    val author: VibeAuthor
) {
    val playableMediaUrl: String?
        get() = media.firstOrNull { it.kind == "video" }?.url
            ?: mediaUrl
            ?: media.firstOrNull()?.url
}

@Serializable
data class VibeMedia(
    val url: String,
    val kind: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val storagePath: String? = null
)

@Serializable
data class VibeAuthor(
    val userId: String? = null,
    val username: String = "member",
    val displayName: String = "Vyb member",
    val avatarUrl: String? = null,
    val isAnonymous: Boolean = false
)

@Serializable
internal data class MobileMeEnvelope(
    val membershipSummary: MobileMembership
)

@Serializable
internal data class MobileMembership(
    val tenantId: String,
    val verificationStatus: String
)

@Serializable
internal data class ReactionRequest(
    val reactionType: String = "like"
)

@Serializable
internal data class StoryReactionEnvelope(
    val storyId: String,
    val aggregateCount: Int,
    val active: Boolean
)

@Serializable
internal data class StorySeenEnvelope(
    val storyId: String,
    val viewed: Boolean
)

@Serializable
internal data class VibeReactionEnvelope(
    val postId: String,
    val aggregateCount: Int,
    val active: Boolean,
    val viewerReactionType: String? = null
)

data class StoriesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val items: List<StoryItem> = emptyList(),
    val selectedIndex: Int? = null,
    val busyStoryIds: Set<String> = emptySet(),
    val error: String? = null
) {
    val selectedStory: StoryItem?
        get() = selectedIndex?.let(items::getOrNull)
}

data class VibesUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val items: List<VibeItem> = emptyList(),
    val nextCursor: String? = null,
    val busyVibeIds: Set<String> = emptySet(),
    val error: String? = null
)
