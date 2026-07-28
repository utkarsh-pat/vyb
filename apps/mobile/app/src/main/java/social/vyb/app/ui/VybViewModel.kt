package social.vyb.app.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import social.vyb.app.data.FirebaseAuthRepository
import social.vyb.app.data.FeedPost
import social.vyb.app.data.RemotePost
import social.vyb.app.data.VybApiRepository
import social.vyb.app.data.VybRepository
import social.vyb.app.data.VybUiState
import social.vyb.app.data.VerificationEmailSentException

class VybViewModel : ViewModel() {
    val repository = VybRepository()
    private val authRepository = FirebaseAuthRepository()
    private val apiRepository = VybApiRepository()
    private val authListener = authRepository.addAuthStateListener(::applyUser)

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
            authRepository.signOut(context)
            state = VybUiState()
        }
    }

    fun clearError() {
        state = state.copy(authError = null, authNotice = null)
    }

    fun refreshHomeFeed() {
        if (!state.isAuthenticated || state.feedLoading) return
        state = state.copy(feedLoading = true, feedError = null)
        viewModelScope.launch {
            runCatching { apiRepository.loadHomeFeed() }
                .onSuccess { result ->
                    state = state.copy(
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
            state = VybUiState(isLoading = false)
            return
        }
        val wasDifferentUser = state.email != user.email.orEmpty()
        state = state.copy(
            isAuthenticated = true,
            isLoading = false,
            authError = null,
            displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Vybnet member",
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString()
        )
        if (wasDifferentUser || state.feed.isEmpty()) refreshHomeFeed()
    }

    private fun toFeedPost(post: RemotePost) = FeedPost(
        id = post.id,
        author = post.author.displayName,
        handle = "@${post.author.username}",
        time = post.createdAt.take(10),
        body = post.body.ifBlank { post.title },
        likes = post.reactions,
        comments = post.comments,
        category = post.location?.takeIf { it.isNotBlank() } ?: post.kind.replaceFirstChar {
            it.uppercase()
        }
    )

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
        authRepository.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
