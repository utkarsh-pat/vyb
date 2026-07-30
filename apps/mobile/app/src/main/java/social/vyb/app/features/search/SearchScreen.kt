package social.vyb.app.features.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    initialUsername: String? = null,
    onOpenPost: (String) -> Unit = {},
    onOpenVibe: (String) -> Unit = {},
    @Suppress("UNUSED_PARAMETER")
    onOpenMarket: (MarketSearchItem) -> Unit = {},
    searchViewModel: SearchViewModel = viewModel()
) {
    val state by searchViewModel.state.collectAsStateWithLifecycle()
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
            }
        )
        return
    }

    PwaSearchSurface(
        state = state,
        onBack = onBack,
        onQueryChange = searchViewModel::updateQuery,
        onOpenProfile = searchViewModel::openProfile,
        onToggleFollow = searchViewModel::toggleFollow,
        onOpenPost = onOpenPost,
        onOpenVibe = onOpenVibe
    )
}
