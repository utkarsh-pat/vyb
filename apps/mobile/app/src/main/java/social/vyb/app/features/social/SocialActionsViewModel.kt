package social.vyb.app.features.social

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SocialActionsViewModel(
    private val repository: SocialActionsRepository = SocialActionsRepository()
) : ViewModel() {
    var state by mutableStateOf(SocialActionsUiState())
        private set
    private val pendingSaveOverrides = mutableMapOf<String, SaveResult>()

    fun schedulePost(
        publishAtMillis: Long,
        text: String,
        isAnonymous: Boolean = false,
        allowAnonymousComments: Boolean = true,
        visibility: String = PostReach.Public.wireValue,
        communityId: String? = null,
        onCreated: (SocialPost) -> Unit = {}
    ) {
        val waitMillis = publishAtMillis - System.currentTimeMillis()
        if (waitMillis <= 0L || text.isBlank()) return
        state = state.copy(operationNotice = "Post scheduled")
        viewModelScope.launch {
            delay(waitMillis)
            createPost(
                text = text,
                isAnonymous = isAnonymous,
                allowAnonymousComments = allowAnonymousComments,
                visibility = visibility,
                communityId = communityId,
                onCreated = onCreated
            )
        }
    }

    fun seedPost(
        postId: String,
        reactionCount: Int,
        viewerReactionType: String? = null,
        savedCount: Int = 0,
        isSaved: Boolean = false
    ) {
        val backendState = PostEngagementState(
            reactionCount = reactionCount,
            viewerReactionType = viewerReactionType,
            savedCount = savedCount,
            isSaved = isSaved
        )
        val pendingSave = pendingSaveOverrides[postId]
        val saveResolution = reconcilePendingSave(backendState, pendingSave)
        if (saveResolution.confirmedByBackend) pendingSaveOverrides.remove(postId)
        val authoritative = saveResolution.engagement
        val reconciled = state.engagements[postId]
            ?.mergeAuthoritative(authoritative)
            ?: authoritative
        if (state.engagements[postId] == reconciled) return
        state = state.copy(
            engagements = state.engagements + (postId to reconciled)
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
                    pendingSaveOverrides[postId] = result
                    updateEngagement(
                        postId,
                        latest.copy(
                            savedCount = result.savedCount,
                            isSaved = result.isSaved,
                            saveLoading = false
                        )
                    )
                    SavedPostsSync.publish(result.postId, result.isSaved)
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

    fun loadContentInsights(postId: String, range: String = "7d") {
        runPostMutation(postId, "Could not load creator insights.") {
            val metrics = repository.contentInsights(postId, range).metrics
            "${range.uppercase()} · ${metrics.reach} reached · ${metrics.views} views · " +
                "${metrics.impressions} impressions · ${metrics.watchSeconds}s watch time"
        }
    }

    fun hideRecommendation(postId: String, onHidden: () -> Unit = {}) {
        runPostMutation(postId, "Could not update recommendations.") {
            repository.hideRecommendation(postId)
            onHidden()
            "We’ll show fewer posts like this."
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
        mediaUrl: String? = null,
        mediaType: String? = null,
        onAdded: () -> Unit = {}
    ) {
        val current = state.commentThreads[postId] ?: CommentThreadState()
        if (current.submitting) return
        updateThread(postId, current.copy(submitting = true, error = null))
        viewModelScope.launch {
            runCatching { repository.addComment(postId, text, parentCommentId, isAnonymous, mediaUrl, mediaType) }
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

    fun toggleCommentReaction(postId: String, commentId: String) {
        if (commentId in state.busyCommentIds) return
        state = state.copy(busyCommentIds = state.busyCommentIds + commentId)
        viewModelScope.launch {
            runCatching { repository.toggleCommentReaction(commentId) }
                .onSuccess { result ->
                    val current = state.commentThreads[postId] ?: CommentThreadState()
                    updateThread(
                        postId,
                        current.copy(items = current.items.applyCommentReaction(result))
                    )
                    state = state.copy(busyCommentIds = state.busyCommentIds - commentId)
                }
                .onFailure {
                    state = state.copy(
                        busyCommentIds = state.busyCommentIds - commentId,
                        operationError = it.userMessage("Could not update comment reaction.")
                    )
                }
        }
    }

    fun loadReactionMembers(postId: String, force: Boolean = false) {
        val current = state.reactionMembers[postId] ?: ReactionMembersState()
        if (current.loading || (current.loaded && !force)) return
        updateReactionMembers(postId, current.copy(loading = true, error = null))
        viewModelScope.launch {
            runCatching { repository.listReactionMembers(postId) }
                .onSuccess {
                    updateReactionMembers(
                        postId,
                        ReactionMembersState(items = it, loaded = true)
                    )
                }
                .onFailure {
                    updateReactionMembers(
                        postId,
                        current.copy(
                            loading = false,
                            error = it.userMessage("Could not load reactions.")
                        )
                    )
                }
        }
    }

    fun repost(
        postId: String,
        quote: String? = null,
        placement: String = "feed",
        onCreated: (SocialPost) -> Unit = {}
    ) = runPostMutation(postId, "Could not repost.") {
        val post = repository.repost(postId, quote, placement)
        seedPost(post.id, post.reactions, post.viewerReactionType, post.savedCount, post.isSaved)
        onCreated(post)
        "Reposted to ${if (placement == "vibe") "Vibes" else "your feed"}."
    }

    fun updatePost(
        postId: String,
        title: String,
        body: String,
        onUpdated: (SocialPost) -> Unit = {}
    ) = runPostMutation(postId, "Could not update post.") {
        onUpdated(repository.updatePost(postId, title, body))
        "Post updated."
    }

    fun deletePost(postId: String, onDeleted: () -> Unit = {}) =
        runPostMutation(postId, "Could not delete post.") {
            repository.deletePost(postId)
            state = state.copy(
                engagements = state.engagements - postId,
                commentThreads = state.commentThreads - postId,
                reactionMembers = state.reactionMembers - postId
            )
            onDeleted()
            "Post deleted."
        }

    fun updateComment(postId: String, commentId: String, body: String) {
        if (commentId in state.busyCommentIds) return
        state = state.copy(busyCommentIds = state.busyCommentIds + commentId)
        viewModelScope.launch {
            runCatching { repository.updateComment(commentId, body) }
                .onSuccess { updated ->
                    val current = state.commentThreads[postId] ?: CommentThreadState()
                    updateThread(
                        postId,
                        current.copy(items = current.items.replaceComment(updated))
                    )
                    state = state.copy(
                        busyCommentIds = state.busyCommentIds - commentId,
                        operationNotice = "Comment updated."
                    )
                }
                .onFailure {
                    state = state.copy(
                        busyCommentIds = state.busyCommentIds - commentId,
                        operationError = it.userMessage("Could not update comment.")
                    )
                }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        if (commentId in state.busyCommentIds) return
        state = state.copy(busyCommentIds = state.busyCommentIds + commentId)
        viewModelScope.launch {
            runCatching { repository.deleteComment(commentId) }
                .onSuccess {
                    val current = state.commentThreads[postId] ?: CommentThreadState()
                    updateThread(
                        postId,
                        current.copy(items = current.items.removeCommentBranch(commentId))
                    )
                    state = state.copy(
                        busyCommentIds = state.busyCommentIds - commentId,
                        operationNotice = "Comment deleted."
                    )
                }
                .onFailure {
                    state = state.copy(
                        busyCommentIds = state.busyCommentIds - commentId,
                        operationError = it.userMessage("Could not delete comment.")
                    )
                }
        }
    }

    fun report(targetType: String, targetId: String, reason: String) {
        val operationId = "$targetType:$targetId"
        if (operationId in state.busyPostIds) return
        state = state.copy(busyPostIds = state.busyPostIds + operationId)
        viewModelScope.launch {
            runCatching { repository.report(targetType, targetId, reason) }
                .onSuccess {
                    state = state.copy(
                        busyPostIds = state.busyPostIds - operationId,
                        operationNotice = "Report submitted for review."
                    )
                }
                .onFailure {
                    state = state.copy(
                        busyPostIds = state.busyPostIds - operationId,
                        operationError = it.userMessage("Could not submit report.")
                    )
                }
        }
    }

    fun clearOperationError() {
        state = state.copy(operationError = null)
    }

    fun clearOperationNotice() {
        state = state.copy(operationNotice = null)
    }

    private fun updateEngagement(postId: String, value: PostEngagementState) {
        state = state.copy(engagements = state.engagements + (postId to value))
    }

    private fun updateThread(postId: String, value: CommentThreadState) {
        state = state.copy(commentThreads = state.commentThreads + (postId to value))
    }

    private fun updateReactionMembers(postId: String, value: ReactionMembersState) {
        state = state.copy(reactionMembers = state.reactionMembers + (postId to value))
    }

    private fun runPostMutation(
        postId: String,
        fallback: String,
        operation: suspend () -> String
    ) {
        if (postId in state.busyPostIds) return
        state = state.copy(busyPostIds = state.busyPostIds + postId)
        viewModelScope.launch {
            runCatching { operation() }
                .onSuccess {
                    state = state.copy(
                        busyPostIds = state.busyPostIds - postId,
                        operationNotice = it
                    )
                }
                .onFailure {
                    state = state.copy(
                        busyPostIds = state.busyPostIds - postId,
                        operationError = it.userMessage(fallback)
                    )
                }
        }
    }

    private fun Throwable.userMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() } ?: fallback
}

internal fun PostEngagementState.mergeAuthoritative(
    authoritative: PostEngagementState
): PostEngagementState = copy(
    reactionCount = if (reactionLoading) reactionCount else authoritative.reactionCount,
    viewerReactionType = if (reactionLoading) viewerReactionType
        else authoritative.viewerReactionType,
    savedCount = if (saveLoading) savedCount else authoritative.savedCount,
    isSaved = if (saveLoading) isSaved else authoritative.isSaved
)

internal data class SaveReconciliation(
    val engagement: PostEngagementState,
    val confirmedByBackend: Boolean,
)

internal fun reconcilePendingSave(
    backend: PostEngagementState,
    pending: SaveResult?,
): SaveReconciliation {
    if (pending == null) return SaveReconciliation(backend, confirmedByBackend = false)
    if (pending.isSaved == backend.isSaved) {
        return SaveReconciliation(backend, confirmedByBackend = true)
    }
    // Navigation can recompose from the previous feed snapshot before a
    // refreshed feed observes the save mutation. Do not let that stale
    // snapshot undo the successful mutation response.
    return SaveReconciliation(
        engagement = backend.copy(
            savedCount = pending.savedCount,
            isSaved = pending.isSaved,
        ),
        confirmedByBackend = false,
    )
}

internal fun List<SocialComment>.replaceComment(updated: SocialComment): List<SocialComment> =
    map { if (it.id == updated.id) updated else it }

internal fun List<SocialComment>.applyCommentReaction(
    result: CommentReactionResult
): List<SocialComment> = map { comment ->
    if (comment.id == result.commentId) {
        comment.copy(
            reactions = result.aggregateCount.coerceAtLeast(0),
            viewerHasLiked = result.active
        )
    } else {
        comment
    }
}

internal fun List<SocialComment>.removeCommentBranch(commentId: String): List<SocialComment> {
    val removedIds = mutableSetOf(commentId)
    var changed: Boolean
    do {
        changed = false
        forEach { comment ->
            if (comment.parentCommentId in removedIds && removedIds.add(comment.id)) {
                changed = true
            }
        }
    } while (changed)
    return filterNot { it.id in removedIds }
}
