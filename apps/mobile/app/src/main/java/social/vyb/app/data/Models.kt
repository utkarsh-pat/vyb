package social.vyb.app.data

data class FeedPost(
    val id: String,
    val authorUserId: String?,
    val author: String,
    val handle: String,
    val avatarUrl: String?,
    val time: String,
    val title: String,
    val body: String,
    val kind: String,
    val media: List<FeedMedia>,
    val location: String?,
    val visibility: String,
    val isAnonymous: Boolean,
    val likes: Int,
    val comments: Int,
    val savedCount: Int,
    val isSaved: Boolean,
    val viewerReactionType: String?,
    val viewerCanManage: Boolean,
    val category: String
)

data class FeedMedia(
    val url: String,
    val kind: String,
    val mimeType: String? = null
)

data class VybUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val authError: String? = null,
    val authNotice: String? = null,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val userId: String? = null,
    val profileCompleted: Boolean? = null,
    val profileSaving: Boolean = false,
    val profileError: String? = null,
    val profileCatalog: List<CourseCatalogItem> = emptyList(),
    val usernameAvailability: Boolean? = null,
    val usernameChecking: Boolean = false,
    val college: String = "Your campus",
    val feed: List<FeedPost> = emptyList(),
    val feedLoading: Boolean = false,
    val feedError: String? = null
)
