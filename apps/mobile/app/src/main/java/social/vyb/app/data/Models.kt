package social.vyb.app.data

data class Story(val name: String, val accent: Long, val isYou: Boolean = false)
data class FeedPost(
    val id: String,
    val author: String,
    val handle: String,
    val time: String,
    val body: String,
    val likes: Int,
    val comments: Int,
    val category: String
)
data class Chat(val name: String, val preview: String, val time: String, val unread: Int = 0)
data class Listing(val title: String, val price: String, val seller: String, val tag: String)

data class VybUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val authError: String? = null,
    val authNotice: String? = null,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val college: String = "Your campus",
    val selectedPost: Int? = null,
    val feed: List<FeedPost> = emptyList(),
    val feedLoading: Boolean = false,
    val feedError: String? = null
)
