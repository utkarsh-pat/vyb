package social.vyb.app.features.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybBackground
import social.vyb.app.ui.VybBackgroundDeep
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText
import social.vyb.app.features.social.SocialAvatar

@Composable
internal fun PwaSearchSurface(
    state: SearchUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectCategory: (SearchCategory) -> Unit,
    onOpenProfile: (String) -> Unit,
    onToggleFollow: (CampusPerson) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenVibe: (String) -> Unit,
    onOpenMarket: (MarketSearchItem) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VybBackgroundDeep, VybBackground, VybBackground)
                )
            )
    ) {
        SearchHeroBar(state.query, onQueryChange, onBack)
        when {
            state.loading && state.suggestions.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VybIndigo)
                }
            }
            state.query.isBlank() -> DiscoveryGrid(
                state = state,
                onOpenProfile = onOpenProfile,
                onOpenPost = onOpenPost,
                onOpenVibe = onOpenVibe
            )
            else -> Column(Modifier.fillMaxSize()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SearchCategory.entries) { category ->
                        Surface(
                            onClick = { onSelectCategory(category) },
                            color = if (state.selectedCategory == category) VybIndigo else VybPanelLifted,
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                "${category.label} ${state.resultCount(category)}",
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                color = VybText,
                                fontWeight = if (state.selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                SearchCategoryResults(
                    state = state,
                    onOpenProfile = onOpenProfile,
                    onToggleFollow = onToggleFollow,
                    onOpenPost = onOpenPost,
                    onOpenVibe = onOpenVibe,
                    onOpenMarket = onOpenMarket
                )
            }
        }
    }
}

@Composable
private fun SearchCategoryResults(
    state: SearchUiState,
    onOpenProfile: (String) -> Unit,
    onToggleFollow: (CampusPerson) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenVibe: (String) -> Unit,
    onOpenMarket: (MarketSearchItem) -> Unit
) {
    val empty = state.resultCount(state.selectedCategory) == 0
    if (empty) {
        VybEmptyState(
            icon = Icons.Default.PersonSearch,
            title = "No ${state.selectedCategory.label.lowercase()} found",
            body = state.categoryErrors[state.selectedCategory] ?: "Try a different campus search.",
            modifier = Modifier.padding(24.dp)
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        when (state.selectedCategory) {
            SearchCategory.People -> items(state.visiblePeople, key = CampusPerson::userId) { person ->
                PersonResultCard(person, person.username in state.mutatingUsers, { onOpenProfile(person.username) }) { onToggleFollow(person) }
            }
            SearchCategory.Posts -> items(state.posts, key = SearchContentItem::id) { item -> SearchContentResultCard(item) { onOpenPost(item.id) } }
            SearchCategory.Vibes -> items(state.vibes, key = SearchContentItem::id) { item -> SearchContentResultCard(item, vibe = true) { onOpenVibe(item.id) } }
            SearchCategory.Marketplace -> items(state.marketplace, key = MarketSearchItem::id) { item ->
                Surface(
                    onClick = { onOpenMarket(item) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    color = VybPanel,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, VybBorder)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(item.title, color = VybText, fontWeight = FontWeight.Bold)
                        Text(item.description, color = VybMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(item.priceLabel, color = VybIndigo, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchContentResultCard(item: SearchContentItem, vibe: Boolean = false, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        color = VybPanel,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, VybBorder)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            item.mediaUrl?.let { VybRemoteImage(it, item.title, Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)), ContentScale.Crop) }
            Column(Modifier.weight(1f).padding(start = if (item.mediaUrl == null) 0.dp else 12.dp)) {
                Text(item.title.ifBlank { item.body }, color = VybText, fontWeight = FontWeight.Bold, maxLines = 2)
                Text("@${item.authorUsername}${if (vibe) " · Vibe" else ""}", color = VybMuted)
            }
        }
    }
}

@Composable
private fun SearchHeroBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = onBack,
            modifier = Modifier.size(52.dp),
            color = VybPanelLifted.copy(alpha = .86f),
            border = BorderStroke(1.dp, VybBorder),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VybText)
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { focusManager.clearFocus() }
            ),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = {
                Text(
                    "Search student names or roll number",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = VybPanel.copy(alpha = .96f),
                unfocusedContainerColor = VybPanel.copy(alpha = .96f),
                focusedBorderColor = VybTeal,
                unfocusedBorderColor = VybBorder,
                focusedTextColor = VybText,
                unfocusedTextColor = VybText,
                focusedPlaceholderColor = VybMuted,
                unfocusedPlaceholderColor = VybMuted
            )
        )
    }
}

private data class DiscoveryItem(val content: SearchContentItem, val vibe: Boolean)

@Composable
private fun DiscoveryGrid(
    state: SearchUiState,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenVibe: (String) -> Unit
) {
    val discovery = buildList {
        val max = maxOf(state.posts.size, state.vibes.size)
        repeat(max) { index ->
            state.posts.getOrNull(index)?.let { add(DiscoveryItem(it, false)) }
            state.vibes.getOrNull(index)?.let { add(DiscoveryItem(it, true)) }
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            TrendingPeople(state.suggestions, onOpenProfile)
        }
        if (discovery.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                VybEmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = "Campus discovery is warming up",
                    body = state.error ?: "New posts and vibes will appear here.",
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        } else {
            items(discovery, key = { "${it.vibe}-${it.content.id}" }) { item ->
                DiscoveryCard(
                    item = item.content,
                    vibe = item.vibe,
                    onOpen = {
                        if (item.vibe) onOpenVibe(item.content.id)
                        else onOpenPost(item.content.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun TrendingPeople(people: List<CampusPerson>, onOpen: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(people.take(8), key = CampusPerson::userId) { person ->
            Column(
                Modifier.clickable { onOpen(person.username) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(78.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(VybIndigo, VybTeal)))
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(VybPanelLifted),
                    contentAlignment = Alignment.Center
                ) {
                    SocialAvatar(
                        avatarUrl = person.avatarUrl,
                        displayName = person.displayName,
                        size = 68.dp,
                        contentDescription = "${person.displayName} profile photo"
                    )
                }
                Text(
                    person.displayName,
                    color = VybText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text("@${person.username}", color = VybMuted, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun DiscoveryCard(item: SearchContentItem, vibe: Boolean, onOpen: () -> Unit) {
    val hasMedia = !item.mediaUrl.isNullOrBlank()
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        color = VybPanel.copy(alpha = .94f),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column {
            if (hasMedia) {
                Box(Modifier.fillMaxWidth().aspectRatio(if (vibe) .76f else 1f)) {
                    if (item.mediaKind == "video") {
                        Box(
                            Modifier.fillMaxSize().background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                "Open vibe",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        VybRemoteImage(
                            item.mediaUrl,
                            "${item.authorName} post",
                            Modifier.fillMaxSize(),
                            ContentScale.Crop
                        )
                    }
                    if (vibe) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                            color = Color.Black.copy(alpha = .62f),
                            shape = CircleShape
                        ) {
                            Row(Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    null,
                                    tint = Color(0xFFFFE26A),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text("Vibe", color = Color(0xFFFFE26A), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(
                    item.authorName,
                    color = VybText,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Text("@${item.authorUsername}", color = VybMuted, fontSize = 11.sp, maxLines = 1)
                item.title.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = VybText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
                item.body.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        color = VybText,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Text(
                    "${item.reactionCount} likes   ${item.commentCount} comments",
                    color = VybMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PersonResultCard(
    person: CampusPerson,
    busy: Boolean,
    onOpen: () -> Unit,
    onToggleFollow: () -> Unit
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
        color = VybPanel.copy(alpha = .94f),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SocialAvatar(
                avatarUrl = person.avatarUrl,
                displayName = person.displayName,
                size = 52.dp
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(person.displayName, color = VybText, fontWeight = FontWeight.Bold)
                Text("@${person.username}", color = VybMuted)
                Text(
                    listOf(person.course, person.stream).filter(String::isNotBlank)
                        .joinToString(" · "),
                    color = VybMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            Surface(
                onClick = onToggleFollow,
                enabled = !busy,
                color = if (person.isFollowing) VybPanelLifted else VybIndigo,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (person.isFollowing) "Following" else "Follow",
                    color = VybText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
