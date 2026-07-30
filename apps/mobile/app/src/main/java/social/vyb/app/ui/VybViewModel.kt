package social.vyb.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import social.vyb.app.data.FirebaseAuthRepository
import social.vyb.app.data.FeedPost
import social.vyb.app.data.FeedMedia
import social.vyb.app.data.RemotePost
import social.vyb.app.data.VybApiRepository
import social.vyb.app.data.VybUiState
import social.vyb.app.data.VerificationEmailSentException
import social.vyb.app.data.UpsertProfileRequest
import java.time.Duration
import java.time.Instant

class VybViewModel : ViewModel() {
    private val authRepository = FirebaseAuthRepository()
    private val apiRepository = VybApiRepository()
    private val authListener = authRepository.addAuthStateListener(::applyUser)
    private var activeUserId: String? = null
    private var sessionLoadJob: Job? = null
    private var usernameCheckJob: Job? = null

    var state by mutableStateOf(VybUiState(isLoading = true))
        private set

    fun signInWithEmail(email: String, password: String) {
        runEmailOperation { callback ->
            authRepository.signInWithEmail(email, password, callback)
        }
    }

    fun createAccount(email: String, password: String) {
        runEmailOperation { callback ->
            authRepository.createAccount(email, password, callback)
        }
    }

    fun sendPasswordReset(email: String) {
        state = state.copy(isLoading = true, authError = null, authNotice = null)
        authRepository.sendPasswordReset(email) { result ->
            result
                .onSuccess { notice ->
                    state = state.copy(isLoading = false, authNotice = notice)
                }
                .onFailure(::applyError)
        }
    }

    fun signInWithGoogle(context: Context) {
        state = state.copy(isLoading = true, authError = null)
        viewModelScope.launch {
            authRepository.signInWithGoogle(context)
                .onSuccess(::applyUser)
                .onFailure(::applyError)
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            sessionLoadJob?.cancel()
            sessionLoadJob = null
            activeUserId = null
            authRepository.signOut(context)
            state = VybUiState()
        }
    }

    fun clearError() {
        state = state.copy(authError = null, authNotice = null)
    }

    fun refreshHomeFeed() {
        if (!state.isAuthenticated || state.profileCompleted != true || state.feedLoading) return
        state = state.copy(feedLoading = true, feedError = null)
        viewModelScope.launch {
            runCatching { apiRepository.loadHomeFeed() }
                .onSuccess { result ->
                    state = state.copy(
                        userId = result.me.user.id,
                        college = "Your campus",
                        feed = result.feed.items.map(::toFeedPost),
                        feedLoading = false,
                        feedError = null
                    )
                }
                .onFailure { error ->
                    state = state.copy(
                        feedLoading = false,
                        feedError = error.localizedMessage
                            ?: "Could not load your campus feed."
                    )
                }
        }
    }

    fun completeProfile(request: UpsertProfileRequest) {
        if (state.profileSaving) return
        state = state.copy(profileSaving = true, profileError = null)
        viewModelScope.launch {
            runCatching { apiRepository.completeProfile(request) }
                .onSuccess { profile ->
                    state = state.copy(
                        profileCompleted = profile.profileCompleted,
                        profileSaving = false,
                        profileError = null,
                        college = profile.collegeName,
                        displayName = profile.profile?.fullName ?: state.displayName
                    )
                    refreshHomeFeed()
                }
                .onFailure { error ->
                    state = state.copy(
                        profileSaving = false,
                        profileError = error.localizedMessage
                            ?: "We could not save your profile right now."
                    )
                }
        }
    }

    fun loadOnboardingCatalog() {
        if (state.profileCatalog.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { apiRepository.loadOnboardingCatalog() }
                .onSuccess { state = state.copy(profileCatalog = it) }
        }
    }

    fun checkUsername(username: String) {
        usernameCheckJob?.cancel()
        state = state.copy(usernameAvailability = null, usernameChecking = false)
        if (!username.matches(Regex("^[a-z0-9](?:[a-z0-9._]{1,22}[a-z0-9])?$"))) return
        usernameCheckJob = viewModelScope.launch {
            delay(350)
            state = state.copy(usernameChecking = true)
            runCatching { apiRepository.isUsernameAvailable(username) }
                .onSuccess { state = state.copy(usernameAvailability = it, usernameChecking = false) }
                .onFailure { state = state.copy(usernameAvailability = null, usernameChecking = false) }
        }
    }

    fun retryAppSession() {
        if (state.isAuthenticated && !state.isLoading) {
            state = state.copy(isLoading = true, profileError = null)
            loadAppSession(force = true)
        }
    }

    private fun runEmailOperation(
        operation: ((Result<FirebaseUser>) -> Unit) -> Unit
    ) {
        state = state.copy(isLoading = true, authError = null, authNotice = null)
        operation { result ->
            result.onSuccess(::applyUser).onFailure(::applyError)
        }
    }

    private fun applyUser(user: FirebaseUser?) {
        if (user == null) {
            sessionLoadJob?.cancel()
            sessionLoadJob = null
            activeUserId = null
            state = VybUiState(isLoading = false)
            return
        }
        val wasDifferentUser = activeUserId != user.uid
        if (wasDifferentUser) {
            sessionLoadJob?.cancel()
            sessionLoadJob = null
        }
        activeUserId = user.uid
        state = state.copy(
            isAuthenticated = true,
            isLoading = wasDifferentUser ||
                state.profileCompleted == null ||
                sessionLoadJob?.isActive == true,
            authError = null,
            profileCompleted = if (wasDifferentUser) null else state.profileCompleted,
            displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Vyb member",
            email = user.email.orEmpty(),
            userId = user.uid,
            photoUrl = user.photoUrl?.toString()
        )
        if (wasDifferentUser || state.profileCompleted == null) loadAppSession()
    }

    private fun loadAppSession(force: Boolean = false) {
        val requestedUserId = activeUserId ?: return
        val runningLoad = sessionLoadJob
        if (runningLoad?.isActive == true) {
            if (!force) return
            runningLoad.cancel()
        }
        sessionLoadJob = viewModelScope.launch {
            runCatching { apiRepository.loadAppSession() }
                .onSuccess { result ->
                    if (activeUserId != requestedUserId) return@onSuccess
                    val home = result.home
                    state = state.copy(
                        isLoading = false,
                        userId = home?.me?.user?.id ?: state.userId,
                        profileCompleted = result.profile.profileCompleted,
                        profileError = null,
                        college = result.profile.collegeName,
                        displayName = result.profile.profile?.fullName ?: state.displayName,
                        feed = home?.feed?.items?.map(::toFeedPost) ?: emptyList(),
                        feedLoading = false,
                        feedError = null
                    )
                }
                .onFailure { error ->
                    if (activeUserId != requestedUserId) return@onFailure
                    state = state.copy(
                        isLoading = false,
                        profileCompleted = null,
                        profileError = error.localizedMessage
                            ?: "Your campus profile could not be loaded."
                    )
                }
        }
    }

    private fun applyError(error: Throwable) {
        state = if (error is VerificationEmailSentException) {
            state.copy(
                isLoading = false,
                authError = null,
                authNotice = error.message
            )
        } else {
            state.copy(
                isLoading = false,
                authError = error.localizedMessage ?: "Authentication failed.",
                authNotice = null
            )
        }
    }

    override fun onCleared() {
        usernameCheckJob?.cancel()
        authRepository.removeAuthStateListener(authListener)
        super.onCleared()
    }
}

internal fun toFeedPost(post: RemotePost) = FeedPost(
    id = post.id,
    authorUserId = post.author.userId,
    author = if (post.isAnonymous || post.author.isAnonymous) "Anonymous" else post.author.displayName,
    handle = if (post.isAnonymous || post.author.isAnonymous) "@anonymous" else "@${post.author.username}",
    avatarUrl = post.author.avatarUrl,
    time = formatSocialAge(post.createdAt),
    title = post.title,
    body = post.body.ifBlank { post.title },
    kind = post.kind,
    media = (post.media.ifEmpty {
        post.mediaUrl?.let {
            listOf(
                social.vyb.app.data.RemoteMediaAsset(
                    url = it,
                    kind = if (post.kind == "video") "video" else "image"
                )
            )
        }.orEmpty()
    }).map { FeedMedia(it.url, it.kind, it.mimeType) },
    location = post.location,
    visibility = post.visibility,
    isAnonymous = post.isAnonymous || post.author.isAnonymous,
    likes = post.reactions,
    comments = post.comments,
    savedCount = post.savedCount,
    isSaved = post.isSaved,
    viewerReactionType = post.viewerReactionType,
    viewerCanManage = post.viewerCanManage,
    category = post.location?.takeIf { it.isNotBlank() } ?: post.kind.replaceFirstChar {
        it.uppercase()
    }
)

internal fun formatSocialAge(value: String, now: Instant = Instant.now()): String {
    val instant = runCatching { Instant.parse(value) }.getOrNull() ?: return ""
    val elapsed = Duration.between(instant, now).coerceAtLeast(Duration.ZERO)
    return when {
        elapsed.toMinutes() < 1 -> "now"
        elapsed.toHours() < 1 -> "${elapsed.toMinutes()}m"
        elapsed.toDays() < 1 -> "${elapsed.toHours()}h"
        elapsed.toDays() < 7 -> "${elapsed.toDays()}d"
        elapsed.toDays() < 365 -> "${elapsed.toDays() / 7}w"
        else -> "${elapsed.toDays() / 365}y"
    }
}
