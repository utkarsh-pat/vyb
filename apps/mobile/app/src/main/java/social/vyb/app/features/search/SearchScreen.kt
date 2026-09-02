package social.vyb.app.features.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import social.vyb.app.features.messages.ChatRepository

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    initialUsername: String? = null,
    onOpenPost: (String) -> Unit = {},
    onOpenVibe: (String) -> Unit = {},
    onOpenMarket: (MarketSearchItem) -> Unit = {},
    onOpenConversation: (String) -> Unit = {},
    searchViewModel: SearchViewModel = viewModel()
) {
    val state by searchViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chatRepository = remember(context) { ChatRepository(context) }
    val scope = rememberCoroutineScope()
    var openingChat by remember { mutableStateOf(false) }
    var chatError by remember { mutableStateOf<String?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }
    LaunchedEffect(initialUsername) {
        initialUsername?.takeIf(String::isNotBlank)?.let(searchViewModel::openProfile)
    }

    state.selectedProfile?.let { selectedProfile ->
        PublicProfileContent(
            response = selectedProfile,
            mutating = selectedProfile.profile.username in state.mutatingUsers,
            onBack = searchViewModel::closeProfile,
            onToggleFollow = {
                searchViewModel.toggleFollow(
                    selectedProfile.profile.copy(
                        isFollowing = selectedProfile.isFollowing,
                        stats = selectedProfile.stats
                    )
                )
            },
            onBlock = { confirmBlock = true },
            openingChat = openingChat,
            chatError = chatError,
            onMessage = {
                if (!openingChat) {
                    openingChat = true
                    chatError = null
                    scope.launch {
                        runCatching {
                            chatRepository.openDirectConversation(selectedProfile.profile.username)
                        }.onSuccess(onOpenConversation)
                            .onFailure { chatError = it.message ?: "Could not open this conversation." }
                        openingChat = false
                    }
                }
            },
            onOpenPost = onOpenPost,
            onOpenVibe = onOpenVibe
        )
        if (confirmBlock) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { confirmBlock = false },
                title = { androidx.compose.material3.Text("Block @${selectedProfile.profile.username}?") },
                text = {
                    androidx.compose.material3.Text(
                        "You will unfollow each other. Their posts, profile and direct chat will be hidden until you unblock them in Privacy settings."
                    )
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        confirmBlock = false
                        searchViewModel.blockSelectedProfile()
                    }) { androidx.compose.material3.Text("Block") }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { confirmBlock = false }) {
                        androidx.compose.material3.Text("Cancel")
                    }
                }
            )
        }
        return
    }

    PwaSearchSurface(
        state = state,
        onBack = onBack,
        onQueryChange = searchViewModel::updateQuery,
        onSelectCategory = searchViewModel::selectCategory,
        onOpenProfile = searchViewModel::openProfile,
        onToggleFollow = searchViewModel::toggleFollow,
        onOpenPost = onOpenPost,
        onOpenVibe = onOpenVibe,
        onOpenMarket = onOpenMarket
    )
}
