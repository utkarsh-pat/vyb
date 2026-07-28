package social.vyb.app.features.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SocialActionsViewModel(
    private val repository: SocialActionsRepository = SocialActionsRepository()
) : ViewModel() {
    var state by mutableStateOf(SocialActionsUiState())
        private set

    fun seedPost(
        postId: String,
        reactionCount: Int,
        viewerReactionType: String? = null,
        savedCount: Int = 0,
        isSaved: Boolean = false
    ) {
        if (state.engagements.containsKey(postId)) return
        state = state.copy(
            engagements = state.engagements + (
                postId to PostEngagementState(
                    reactionCount = reactionCount,
                    viewerReactionType = viewerReactionType,
                    savedCount = savedCount,
                    isSaved = isSaved
                )
            )
        )
    }

    fun createPost(
        text: String,
        isAnonymous: Boolean = false,
        allowAnonymousComments: Boolean = true,
        visibility: String = PostReach.Public.wireValue,
        communityId: String? = null,
        onCreated: (SocialPost) -> Unit = {}
    ) {
        if (state.creatingPost) return
        state = state.copy(creatingPost = true, createPostError = null)
        viewModelScope.launch {
            runCatching {
                repository.createTextPost(
                    text = text,
                    isAnonymous = isAnonymous,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = visibility,
                    communityId = communityId
                )
            }
                .onSuccess { post ->
                    state = state.copy(creatingPost = false, createPostError = null)
                    seedPost(
                        post.id,
                        post.reactions,
                        post.viewerReactionType,
                        post.savedCount,
                        post.isSaved
                    )
                    onCreated(post)
                }
                .onFailure { error ->
                    state = state.copy(
                        creatingPost = false,
                        createPostError = error.userMessage("Could not publish your post.")
                    )
                }
        }
    }

    fun toggleReaction(postId: String, reactionType: String = "like") {
        val current = state.engagements[postId] ?: PostEngagementState()
        if (current.reactionLoading) return
        updateEngagement(postId, current.copy(reactionLoading = true))
        viewModelScope.launch {
            runCatching { repository.toggleReaction(postId, reactionType) }
                .onSuccess { result ->
                    val latest = state.engagements[postId] ?: current
                    updateEngagement(
                        postId,
                        latest.copy(
                            reactionCount = result.aggregateCount,
                            viewerReactionType = result.viewerReactionType,
                            reactionLoading = false
                        )
                    )
                }
                .onFailure { error ->
                    val latest = state.engagements[postId] ?: current
                    updateEngagement(postId, latest.copy(reactionLoading = false))
                    state = state.copy(
                        operationError = error.userMessage("Could not update reaction.")
                    )
                }
        }
    }

    fun toggleSave(postId: String) {
        val current = state.engagements[postId] ?: PostEngagementState()
        if (current.saveLoading) return
        updateEngagement(postId, current.copy(saveLoading = true))
        viewModelScope.launch {
            runCatching { repository.toggleSave(postId) }
                .onSuccess { result ->
                    val latest = state.engagements[postId] ?: current
                    updateEngagement(
                        postId,
                        latest.copy(
                            savedCount = result.savedCount,
                            isSaved = result.isSaved,
                            saveLoading = false
                        )
                    )
                }
                .onFailure { error ->
                    val latest = state.engagements[postId] ?: current
                    updateEngagement(postId, latest.copy(saveLoading = false))
                    state = state.copy(
                        operationError = error.userMessage("Could not update saved posts.")
                    )
                }
        }
    }

    fun loadComments(postId: String, force: Boolean = false) {
        val current = state.commentThreads[postId] ?: CommentThreadState()
        if (current.loading || (current.loaded && !force)) return
        updateThread(postId, current.copy(loading = true, error = null))
        viewModelScope.launch {
            runCatching { repository.listComments(postId) }
                .onSuccess { comments ->
                    updateThread(
                        postId,
                        current.copy(items = comments, loading = false, loaded = true, error = null)
                    )
                }
                .onFailure { error ->
                    updateThread(
                        postId,
                        current.copy(
                            loading = false,
                            loaded = false,
                            error = error.userMessage("Could not load comments.")
                        )
                    )
                }
        }
    }

    fun addComment(
        postId: String,
        text: String,
        parentCommentId: String? = null,
        isAnonymous: Boolean = false,
        onAdded: () -> Unit = {}
    ) {
        val current = state.commentThreads[postId] ?: CommentThreadState()
        if (current.submitting) return
        updateThread(postId, current.copy(submitting = true, error = null))
        viewModelScope.launch {
            runCatching { repository.addComment(postId, text, parentCommentId, isAnonymous) }
                .onSuccess { comment ->
                    val latest = state.commentThreads[postId] ?: current
                    updateThread(
                        postId,
                        latest.copy(
                            items = latest.items + comment,
                            submitting = false,
                            loaded = true,
                            error = null
                        )
                    )
                    onAdded()
                }
                .onFailure { error ->
                    val latest = state.commentThreads[postId] ?: current
                    updateThread(
                        postId,
                        latest.copy(
                            submitting = false,
                            error = error.userMessage("Could not publish comment.")
                        )
                    )
                }
        }
    }

    fun clearOperationError() {
        state = state.copy(operationError = null)
    }

    private fun updateEngagement(postId: String, value: PostEngagementState) {
        state = state.copy(engagements = state.engagements + (postId to value))
    }

    private fun updateThread(postId: String, value: CommentThreadState) {
        state = state.copy(commentThreads = state.commentThreads + (postId to value))
    }

    private fun Throwable.userMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() } ?: fallback
}
