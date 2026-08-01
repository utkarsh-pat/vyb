package social.vyb.app.features.social

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SavedPostChange(
    val revision: Long = 0,
    val postId: String? = null,
    val isSaved: Boolean = false,
)

/**
 * Process-local invalidation for screens backed by the saved-posts endpoint.
 *
 * Feed and Profile use navigation-scoped ViewModels, so changing a bookmark in
 * one destination otherwise leaves the other destination's cached shelf stale.
 */
object SavedPostsSync {
    private val _changes = MutableStateFlow(SavedPostChange())
    val changes: StateFlow<SavedPostChange> = _changes.asStateFlow()

    fun publish(postId: String, isSaved: Boolean) {
        _changes.update { current ->
            SavedPostChange(
                revision = current.revision + 1,
                postId = postId,
                isSaved = isSaved,
            )
        }
    }
}
