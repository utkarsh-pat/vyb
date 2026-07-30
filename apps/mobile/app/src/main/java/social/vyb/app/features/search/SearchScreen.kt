package social.vyb.app.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybText
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybRemoteVideo

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    initialUsername: String? = null,
    onOpenPost: (String) -> Unit = {},
    onOpenVibe: (String) -> Unit = {},
    onOpenMarket: (MarketSearchItem) -> Unit = {},
    searchViewModel: SearchViewModel = viewModel()
) {
    val state by searchViewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(initialUsername) {
        initialUsername?.takeIf(String::isNotBlank)?.let(searchViewModel::openProfile)
    }
    val selectedProfile = state.selectedProfile
    if (selectedProfile != null) {
        PublicProfileContent(
            response = selectedProfile,
            mutating = selectedProfile.profile.username in state.mutatingUsers,
            onBack = searchViewModel::closeProfile,
            onToggleFollow = { searchViewModel.toggleFollow(selectedProfile.profile.copy(
                isFollowing = selectedProfile.isFollowing,
                stats = selectedProfile.stats
            )) }
        )
        return
    }

    VybResponsiveFrame(Modifier.fillMaxSize(), maxContentWidth = 1120.dp) { layout ->
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
        Column(Modifier.weight(1f).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VybText)
                }
                Column {
                    Text(
                        "Search campus",
                        color = VybText,
                        fontWeight = FontWeight.Black,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                    )
                    if (layout.wide) {
                        Text("People, posts, vibes and marketplace", color = VybMuted)
                    }
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = searchViewModel::updateQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = {
                    Text(
                        "Search people, posts, vibes or market",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = VybPanelLifted,
                    unfocusedContainerColor = VybPanelLifted,
                    focusedBorderColor = VybIndigo,
                    unfocusedBorderColor = VybBorder,
                    focusedTextColor = VybText,
                    unfocusedTextColor = VybText,
                    focusedPlaceholderColor = VybMuted,
                    unfocusedPlaceholderColor = VybMuted
                )
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.size(8.dp)) }
                items(SearchCategory.entries, key = SearchCategory::name) { category ->
                    val selected = state.selectedCategory == category
                    Surface(
                        onClick = { searchViewModel.selectCategory(category) },
                        modifier = Modifier.height(42.dp),
                        color = if (selected) VybIndigo else VybPanelLifted,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) VybIndigo else VybBorder
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${category.label} ${state.resultCount(category)}",
                                color = VybText,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.size(8.dp)) }
            }
            Text(
                if (state.query.isBlank()) {
                    if (state.selectedCategory == SearchCategory.People) {
                        "Suggested for you"
                    } else {
                        state.selectedCategory.label
                    }
                } else {
                    "${state.selectedCategory.label} matching '${state.query.trim()}'"
                },
                color = VybText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 8.dp)
            )
            val categoryError = state.categoryErrors[state.selectedCategory] ?: state.error
            val categoryIsEmpty = state.resultCount(state.selectedCategory) == 0
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VybIndigo)
                }
                categoryError != null -> VybEmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = "Could not load ${state.selectedCategory.label.lowercase()}",
                    body = categoryError,
                    actionLabel = "Try again",
                    onAction = searchViewModel::retry,
                    modifier = Modifier.padding(18.dp)
                )
                categoryIsEmpty -> VybEmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = if (state.query.isBlank()) {
                        if (state.selectedCategory == SearchCategory.People) {
                            "No suggestions yet"
                        } else {
                            "Search ${state.selectedCategory.label.lowercase()}"
                        }
                    } else {
                        "No ${state.selectedCategory.label.lowercase()} found"
                    },
                    body = if (state.query.isBlank()) {
                        if (state.selectedCategory == SearchCategory.People) {
                            "Campus suggestions will appear as your network grows."
                        } else {
                            "Enter a campus search above to explore this category."
                        }
                    } else {
                        "Try fewer words or a different campus keyword."
                    },
                    modifier = Modifier.padding(18.dp)
                )
                state.selectedCategory == SearchCategory.People -> LazyColumn {
                    items(state.visiblePeople, key = CampusPerson::userId) { person ->
                        PersonRow(
                            person = person,
                            mutating = person.username in state.mutatingUsers,
                            onOpen = { searchViewModel.openProfile(person.username) },
                            onToggleFollow = { searchViewModel.toggleFollow(person) }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
                state.selectedCategory == SearchCategory.Posts -> LazyColumn {
                    items(state.posts, key = SearchContentItem::id) {
                        SearchContentCard(it, label = "Post", onOpen = { onOpenPost(it.id) })
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
                state.selectedCategory == SearchCategory.Vibes -> LazyColumn {
                    items(state.vibes, key = SearchContentItem::id) {
                        SearchContentCard(it, label = "Vibe", onOpen = { onOpenVibe(it.id) })
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
                else -> LazyColumn {
                    items(
                        state.marketplace,
                        key = { "${it.kind.name}-${it.id}" }
                    ) {
                        MarketSearchCard(it, onOpen = { onOpenMarket(it) })
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        if (layout.wide) {
            SearchSummaryRail(
                state = state,
                onSelectCategory = searchViewModel::selectCategory,
                onOpenProfile = searchViewModel::openProfile,
                modifier = Modifier.fillMaxSize().weight(.42f).padding(top = 10.dp, end = 16.dp)
            )
        }
        }
    }
}

@Composable
private fun SearchSummaryRail(
    state: SearchUiState,
    onSelectCategory: (SearchCategory) -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = VybPanel.copy(alpha = .88f),
            border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Explore campus", color = VybText, fontWeight = FontWeight.Black)
                Text(
                    if (state.query.isBlank()) {
                        "Start with people your campus is discovering."
                    } else {
                        "Results for \"${state.query.trim()}\""
                    },
                    color = VybMuted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                SearchCategory.entries.forEach { category ->
                    Surface(
                        onClick = { onSelectCategory(category) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        color = if (state.selectedCategory == category) {
                            VybIndigo.copy(alpha = .16f)
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                category.label,
                                color = VybText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(state.resultCount(category).toString(), color = VybMuted)
                        }
                    }
                }
            }
        }
        if (state.suggestions.isNotEmpty()) {
            Text(
                "Trending profiles",
                color = VybText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
            )
            state.suggestions.take(4).forEach { person ->
                Surface(
                    onClick = { onOpenProfile(person.username) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileInitial(person.displayName, 40, person.avatarUrl)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(person.displayName, color = VybText, fontWeight = FontWeight.Bold)
                            Text("@${person.username}", color = VybMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchContentCard(
    item: SearchContentItem,
    label: String,
    onOpen: () -> Unit
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        color = VybPanel,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            item.mediaUrl?.takeIf(String::isNotBlank)?.let { url ->
                if (item.mediaKind == "video") {
                    VybRemoteVideo(
                        url = url,
                        contentDescription = "${item.authorName} $label video",
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)
                    )
                } else {
                    VybRemoteImage(
                        url = url,
                        contentDescription = "${item.authorName} $label image",
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f)
                    )
                }
            }
            Column(Modifier.padding(15.dp)) {
                Text(
                    "$label · @${item.authorUsername}",
                    color = VybIndigo,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                item.title.takeIf(String::isNotBlank)?.let {
                    Text(it, color = VybText, fontWeight = FontWeight.Bold)
                }
                item.body.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = VybText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    "${item.reactionCount} reactions · ${item.commentCount} comments",
                    color = VybMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MarketSearchCard(item: MarketSearchItem, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        color = VybPanel,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            item.mediaUrl?.takeIf(String::isNotBlank)?.let {
                VybRemoteImage(
                    url = it,
                    contentDescription = item.title,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(14.dp))
                )
            }
            Column(Modifier.weight(1f).padding(start = if (item.mediaUrl.isNullOrBlank()) 0.dp else 12.dp)) {
                Text(item.title, color = VybText, fontWeight = FontWeight.Bold)
                Text(
                    item.description,
                    color = VybMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${item.priceLabel} · ${item.category}",
                    color = VybIndigo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    listOf(item.location, item.ownerName).filter(String::isNotBlank)
                        .joinToString(" · "),
                    color = VybMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PersonRow(
    person: CampusPerson,
    mutating: Boolean,
    onOpen: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileInitial(person.displayName, 52, person.avatarUrl)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(person.displayName, color = VybText, fontWeight = FontWeight.Bold, maxLines = 1)
            Text("@${person.username}", color = VybMuted, maxLines = 1)
            Text(
                listOf(person.course, person.stream).filter(String::isNotBlank).joinToString(" · "),
                color = VybMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        FollowButton(person.isFollowing, mutating, onToggleFollow)
    }
}

@Composable
private fun PublicProfileContent(
    response: PublicProfileResponse,
    mutating: Boolean,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit
) {
    val person = response.profile
    VybResponsiveFrame(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VybText)
                    }
                    Text("@${person.username}", color = VybText, fontWeight = FontWeight.Black)
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ProfileInitial(person.displayName, 88, person.avatarUrl)
                    Spacer(Modifier.height(12.dp))
                    Text(person.displayName, color = VybText, fontWeight = FontWeight.Black)
                    Text(person.collegeName, color = VybMuted)
                    person.bio?.takeIf(String::isNotBlank)?.let {
                        Text(it, color = VybText, modifier = Modifier.padding(top = 12.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat(response.stats.posts, "Posts")
                        ProfileStat(response.stats.followers, "Followers")
                        ProfileStat(response.stats.following, "Following")
                    }
                    if (!response.isViewerProfile) {
                        FollowButton(response.isFollowing, mutating, onToggleFollow)
                    }
                }
                Text(
                    "Posts",
                    color = VybText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
            if (response.posts.isEmpty()) {
                item {
                    VybEmptyState(
                        icon = Icons.Default.PersonSearch,
                        title = "No posts yet",
                        body = "Public posts from this profile will appear here.",
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                items(response.posts, key = { it.id }) { post ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        color = VybPanel,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (post.title.isNotBlank()) {
                                Text(post.title, color = VybText, fontWeight = FontWeight.Bold)
                            }
                            Text(post.body, color = VybText, modifier = Modifier.padding(top = 4.dp))
                            post.mediaUrl?.takeIf(String::isNotBlank)?.let { url ->
                                if (post.kind == "video") {
                                    VybRemoteVideo(
                                        url = url,
                                        contentDescription = "${post.author.displayName} video",
                                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).padding(top = 10.dp)
                                    )
                                } else {
                                    VybRemoteImage(
                                        url = url,
                                        contentDescription = "${post.author.displayName} post image",
                                        modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).padding(top = 10.dp)
                                    )
                                }
                            }
                            Text(
                                "${post.reactions} likes · ${post.comments} comments",
                                color = VybMuted,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProfileInitial(name: String, size: Int, avatarUrl: String? = null) {
    Box(
        Modifier.size(size.dp).clip(CircleShape)
            .background(VybIndigo.copy(alpha = .25f)),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNullOrBlank()) {
            Text(name.take(1).uppercase(), color = VybText, fontWeight = FontWeight.Black)
        } else {
            VybRemoteImage(
                url = avatarUrl,
                contentDescription = "$name profile photo",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ProfileStat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = VybText, fontWeight = FontWeight.Black)
        Text(label, color = VybMuted)
    }
}

@Composable
private fun FollowButton(
    following: Boolean,
    mutating: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !mutating,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (following) VybPanelLifted else VybIndigo,
            contentColor = VybText
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (mutating) {
            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = VybText)
        } else {
            Text(if (following) "Following" else "Follow")
        }
    }
}
