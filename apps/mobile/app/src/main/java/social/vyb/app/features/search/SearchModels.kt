package social.vyb.app.features.search

import kotlinx.serialization.Serializable
import social.vyb.app.features.social.SocialPost

@Serializable
data class ProfileStats(
    val posts: Int = 0,
    val followers: Int = 0,
    val following: Int = 0
)

@Serializable
data class CampusPerson(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val collegeName: String = "",
    val course: String = "",
    val stream: String = "",
    val bio: String? = null,
    val isFollowing: Boolean = false,
    val stats: ProfileStats = ProfileStats()
)

@Serializable
internal data class UserSearchResponse(
    val query: String = "",
    val items: List<CampusPerson> = emptyList()
)

@Serializable
data class PublicProfileResponse(
    val profile: CampusPerson,
    val stats: ProfileStats = ProfileStats(),
    val isFollowing: Boolean = false,
    val isViewerProfile: Boolean = false,
    val posts: List<SocialPost> = emptyList()
)

@Serializable
data class FollowResponse(
    val username: String,
    val isFollowing: Boolean,
    val stats: ProfileStats = ProfileStats()
)

@Serializable
data class BlockResponse(
    val username: String,
    val isBlocked: Boolean
)

@Serializable
data class BlockedPerson(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null
)

@Serializable
internal data class BlockedPeopleResponse(val items: List<BlockedPerson> = emptyList())

internal fun ProfileStats.withFollowCounts(followStats: ProfileStats): ProfileStats = copy(
    followers = followStats.followers,
    following = followStats.following
)

enum class SearchCategory(val label: String) {
    People("People"),
    Posts("Posts"),
    Vibes("Vibes"),
    Marketplace("Market")
}

data class SearchContentItem(
    val id: String,
    val title: String,
    val body: String,
    val authorName: String,
    val authorUsername: String,
    val mediaUrl: String? = null,
    val mediaKind: String = "image",
    val location: String? = null,
    val reactionCount: Int = 0,
    val commentCount: Int = 0,
    val createdAt: String = ""
)

enum class MarketSearchKind {
    Listing,
    Request
}

data class MarketSearchItem(
    val id: String,
    val kind: MarketSearchKind,
    val title: String,
    val description: String,
    val category: String,
    val priceLabel: String,
    val location: String,
    val ownerName: String,
    val mediaUrl: String? = null,
    val createdAt: String = ""
)

data class UniversalSearchResult(
    val people: List<CampusPerson> = emptyList(),
    val posts: List<SearchContentItem> = emptyList(),
    val vibes: List<SearchContentItem> = emptyList(),
    val marketplace: List<MarketSearchItem> = emptyList(),
    val categoryErrors: Map<SearchCategory, String> = emptyMap()
)
