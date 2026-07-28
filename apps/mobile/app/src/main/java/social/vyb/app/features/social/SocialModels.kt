package social.vyb.app.features.social

import kotlinx.serialization.Serializable

@Serializable
data class SocialAuthor(
    val userId: String? = null,
    val username: String = "member",
    val displayName: String = "Vybnet member",
    val avatarUrl: String? = null,
    val isAnonymous: Boolean = false
)

@Serializable
data class SocialPost(
    val id: String,
    val tenantId: String = "",
    val communityId: String? = null,
    val membershipId: String? = null,
    val placement: String = "feed",
    val kind: String = "text",
    val title: String = "",
    val body: String = "",
    val mediaUrl: String? = null,
    val location: String? = null,
    val reactions: Int = 0,
    val comments: Int = 0,
    val savedCount: Int = 0,
    val isSaved: Boolean = false,
    val viewerReactionType: String? = null,
    val createdAt: String = "",
    val author: SocialAuthor = SocialAuthor()
)

@Serializable
data class SocialComment(
    val id: String,
    val postId: String,
    val membershipId: String? = null,
    val authorUserId: String? = null,
    val parentCommentId: String? = null,
    val body: String = "",
    val mediaUrl: String? = null,
    val mediaType: String? = null,
    val isAnonymous: Boolean = false,
    val createdAt: String = "",
    val reactions: Int = 0,
    val viewerHasLiked: Boolean = false,
    val viewerCanManage: Boolean = false,
    val author: SocialAuthor? = null
)

@Serializable
internal data class ViewerEnvelope(
    val membershipSummary: ViewerMembership
)

@Serializable
internal data class ViewerMembership(
    val id: String,
    val tenantId: String,
    val verificationStatus: String
)

@Serializable
internal data class CreatePostBody(
    val tenantId: String,
    val membershipId: String,
    val kind: String = "text",
    val placement: String = "feed",
    val title: String = "Campus update",
    val body: String,
    val communityId: String? = null,
    val isAnonymous: Boolean = false,
    val allowAnonymousComments: Boolean = true,
    val visibility: String = PostReach.Public.wireValue
)

data class PostCommunityOption(
    val id: String,
    val name: String
)

enum class PostReach(
    val wireValue: String,
    val label: String,
    val description: String
) {
    Public(
        wireValue = "public",
        label = "Public",
        description = "Anyone on Vybnet can see this"
    ),
    FollowersOnly(
        wireValue = "followers",
        label = "Followers only",
        description = "Only people who follow you can see it"
    ),
    CommunityOnly(
        wireValue = "community",
        label = "Community only",
        description = "Only members of one selected community can see it"
    );

    companion object {
        fun fromWireValue(value: String?): PostReach =
            entries.firstOrNull { it.wireValue == value } ?: Public
    }
}

@Serializable
internal data class CreatePostEnvelope(val item: SocialPost)

@Serializable
internal data class ReactionBody(val reactionType: String = "like")

@Serializable
data class ReactionResult(
    val postId: String,
    val reactionType: String? = null,
    val aggregateCount: Int = 0,
    val active: Boolean = false,
    val viewerReactionType: String? = null
)

@Serializable
data class SaveResult(
    val postId: String,
    val savedCount: Int = 0,
    val isSaved: Boolean = false
)

@Serializable
internal data class CommentListEnvelope(
    val postId: String,
    val items: List<SocialComment> = emptyList()
)

@Serializable
internal data class CreateCommentBody(
    val membershipId: String,
    val body: String,
    val parentCommentId: String? = null,
    val isAnonymous: Boolean = false
)

@Serializable
internal data class CreateCommentEnvelope(val item: SocialComment)

data class PostEngagementState(
    val reactionCount: Int = 0,
    val viewerReactionType: String? = null,
    val savedCount: Int = 0,
    val isSaved: Boolean = false,
    val reactionLoading: Boolean = false,
    val saveLoading: Boolean = false
)

data class CommentThreadState(
    val items: List<SocialComment> = emptyList(),
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null
)

data class SocialActionsUiState(
    val creatingPost: Boolean = false,
    val createPostError: String? = null,
    val operationError: String? = null,
    val engagements: Map<String, PostEngagementState> = emptyMap(),
    val commentThreads: Map<String, CommentThreadState> = emptyMap()
)
