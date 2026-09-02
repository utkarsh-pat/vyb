package social.vyb.app.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import social.vyb.app.features.social.SocialPost
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybText
import social.vyb.app.features.social.SocialAvatar

@Composable
internal fun PublicProfileContent(
    response: PublicProfileResponse,
    mutating: Boolean,
    openingChat: Boolean,
    chatError: String?,
    onBack: () -> Unit,
    onToggleFollow: () -> Unit,
    onBlock: () -> Unit,
    onMessage: () -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenVibe: (String) -> Unit
) {
    val person = response.profile
    var profileMenuOpen by remember(person.username) { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VybText)
                    }
                    Text("@${person.username}", color = VybText, fontWeight = FontWeight.Black)
                    Spacer(Modifier.weight(1f))
                    if (!response.isViewerProfile) {
                        Box {
                            IconButton(onClick = { profileMenuOpen = true }) {
                                Icon(Icons.Default.MoreVert, "Profile options", tint = VybText)
                            }
                            DropdownMenu(
                                expanded = profileMenuOpen,
                                onDismissRequest = { profileMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Block @${person.username}") },
                                    onClick = {
                                        profileMenuOpen = false
                                        onBlock()
                                    }
                                )
                            }
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SearchProfileAvatar(person.displayName, person.avatarUrl, 82)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(person.displayName, color = VybText, fontWeight = FontWeight.Black)
                        Text(person.collegeName, color = VybMuted, maxLines = 2)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SearchProfileStat(response.stats.posts, "posts")
                            SearchProfileStat(response.stats.followers, "followers")
                            SearchProfileStat(response.stats.following, "following")
                        }
                    }
                }
                person.bio?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = VybText, modifier = Modifier.padding(horizontal = 18.dp))
                }
                if (!response.isViewerProfile) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onToggleFollow,
                            enabled = !mutating,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (response.isFollowing) VybPanelLifted else VybIndigo,
                                contentColor = VybText
                            )
                        ) {
                            if (mutating) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = VybText
                                )
                            } else {
                                Text(if (response.isFollowing) "Following" else "Follow")
                            }
                        }
                        OutlinedButton(
                            onClick = onMessage,
                            enabled = !openingChat,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (openingChat) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = VybText
                                )
                            } else {
                                Text("Message")
                            }
                        }
                    }
                    chatError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                }
                Text(
                    "POSTS",
                    color = VybText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                )
            }
        }
        if (response.posts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                VybEmptyState(
                    icon = Icons.Default.PersonSearch,
                    title = "No posts yet",
                    body = "Public posts from this profile will appear here.",
                    modifier = Modifier.padding(18.dp)
                )
            }
        } else {
            items(response.posts, key = SocialPost::id) { post ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(VybPanelLifted)
                        .clickable {
                            if (post.kind == "video" || post.placement == "vibe") onOpenVibe(post.id)
                            else onOpenPost(post.id)
                        }
                ) {
                    val url = post.mediaUrl?.takeIf(String::isNotBlank)
                    when {
                        url != null && post.kind == "video" -> Box(
                            Modifier.fillMaxSize().background(VybPanelLifted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PlayArrow, "Open vibe", tint = Color.White)
                        }
                        url != null -> VybRemoteImage(
                            url,
                            "${post.author.displayName} post",
                            Modifier.fillMaxSize(),
                            ContentScale.Crop
                        )
                        else -> Text(
                            post.title.ifBlank { post.body },
                            color = VybText,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SearchProfileAvatar(name: String, url: String?, size: Int) {
    SocialAvatar(
        avatarUrl = url,
        displayName = name,
        size = size.dp,
        contentDescription = "$name profile photo"
    )
}

@Composable
private fun SearchProfileStat(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = VybText, fontWeight = FontWeight.Bold)
        Text(label, color = VybMuted)
    }
}
