package social.vyb.app.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.R
import social.vyb.app.data.ProfileRecord
import social.vyb.app.features.social.SocialPost
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybText

@Composable
internal fun PwaProfileSurface(
    state: ProfileUiState,
    onTab: (String) -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onCreatePost: () -> Unit,
    onConnections: (String) -> Unit,
    onOpenLink: (String) -> Unit
) {
    val profile = requireNotNull(state.privateProfile)
    val publicProfile = requireNotNull(state.publicProfile)
    val links = listOf("github", "instagram", "email", "twitter", "x")
        .mapNotNull { network ->
            safeSocialUrl(network, profile.socialLinks?.get(network))
                ?.let { ProfileLink(network, it) }
        }
        .distinctBy { if (it.network == "twitter") "x" else it.network }
    val content = when (state.activeTab) {
        "vibes" -> publicProfile.posts.filter { it.kind == "video" || it.placement == "vibe" }
        "saved" -> emptyList()
        else -> publicProfile.posts.filterNot { it.kind == "video" || it.placement == "vibe" }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF071A35), Color(0xFF071426), Color(0xFF0A1325))
                )
            ),
        contentPadding = PaddingValues(bottom = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                PwaProfileHeader(onSettings, onCreatePost)
                ProfileSummary(
                    profile = profile,
                    posts = publicProfile.stats.posts,
                    followers = publicProfile.stats.followers,
                    following = publicProfile.stats.following,
                    links = links,
                    onEdit = onEdit,
                    onConnections = onConnections,
                    onOpenLink = onOpenLink
                )
                ProfileUnderlineTabs(state.activeTab, onTab)
            }
        }
        if (content.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    Modifier.fillMaxWidth().padding(top = 78.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        when (state.activeTab) {
                            "vibes" -> Icons.Default.VideoLibrary
                            "saved" -> Icons.Default.BookmarkBorder
                            else -> Icons.Default.GridView
                        },
                        null,
                        tint = VybMuted,
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        when (state.activeTab) {
                            "vibes" -> "No vibes yet"
                            "saved" -> "No saved yet"
                            else -> "No posts yet"
                        },
                        color = VybMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }
        } else {
            items(content, key = SocialPost::id) { post ->
                ProfileGridTile(post)
            }
        }
    }
}

@Composable
private fun PwaProfileHeader(onSettings: () -> Unit, onCreatePost: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.vyb_logo),
            contentDescription = "Vyb",
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.weight(1f))
        CircleAction(Icons.Default.Settings, "Settings", onSettings)
        Surface(
            onClick = onCreatePost,
            modifier = Modifier.padding(start = 9.dp),
            color = VybIndigo,
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(
                "Post",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)
            )
        }
    }
}

@Composable
private fun ProfileSummary(
    profile: ProfileRecord,
    posts: Int,
    followers: Int,
    following: Int,
    links: List<ProfileLink>,
    onEdit: () -> Unit,
    onConnections: (String) -> Unit,
    onOpenLink: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(VybIndigo.copy(alpha = .18f), Color(0xFF00BFAE).copy(alpha = .10f))
                )
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfilePhoto(profile)
            Column(Modifier.weight(1f).padding(start = 15.dp)) {
                Text(
                    profile.fullName.uppercase(),
                    color = VybText,
                    fontWeight = FontWeight.Medium,
                    fontSize = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Metric(posts, "posts")
                    Metric(followers, "followers") { onConnections("followers") }
                    Metric(following, "following") { onConnections("following") }
                }
            }
            CircleAction(Icons.Default.Edit, "Edit profile", onEdit)
        }
        Text(
            "@${profile.username}",
            color = VybText,
            fontSize = 17.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
        Text(
            buildList {
                add(listOf(profile.course, profile.stream).filter(String::isNotBlank)
                    .joinToString(" / "))
                add(profile.collegeName)
            }.filter(String::isNotBlank).joinToString(" · "),
            color = VybMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
        profile.bio?.takeIf(String::isNotBlank)?.let {
            Text(it, color = VybText, modifier = Modifier.padding(top = 7.dp))
        }
        if (links.isNotEmpty()) {
            Row(Modifier.padding(top = 13.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                links.take(4).forEach { link ->
                    Surface(
                        onClick = { onOpenLink(link.url) },
                        modifier = Modifier.size(44.dp),
                        color = when (link.network) {
                            "instagram" -> Color(0xFFD62976)
                            "email" -> Color(0xFF00AFA2)
                            else -> VybPanelLifted
                        },
                        border = BorderStroke(1.dp, VybBorder),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when (link.network) {
                                    "instagram" -> Icons.Default.CameraAlt
                                    "email" -> Icons.Default.Email
                                    "twitter", "x" -> Icons.Default.AlternateEmail
                                    else -> Icons.Default.Code
                                },
                                link.network,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePhoto(profile: ProfileRecord) {
    Box(
        Modifier.size(82.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(VybIndigo, Color(0xFF00BFAE))))
            .padding(5.dp).clip(CircleShape).background(VybPanelLifted),
        contentAlignment = Alignment.Center
    ) {
        if (profile.avatarUrl.isNullOrBlank()) {
            Text(
                profile.fullName.take(1).uppercase(),
                color = VybText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            VybRemoteImage(
                profile.avatarUrl,
                "${profile.fullName} profile photo",
                Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Metric(value: Int, label: String, onClick: (() -> Unit)? = null) {
    Column(
        Modifier.then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value.toString(), color = VybText, fontWeight = FontWeight.Bold)
        Text(label, color = VybMuted, fontSize = 12.sp)
    }
}

@Composable
private fun ProfileUnderlineTabs(active: String, onTab: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            Triple("posts", "POSTS", Icons.Default.GridView),
            Triple("vibes", "VIBES", Icons.Default.VideoLibrary),
            Triple("saved", "SAVED", Icons.Default.BookmarkBorder)
        ).forEach { (id, label, icon) ->
            val selected = active == id
            Column(
                Modifier.weight(1f).clickable { onTab(id) }.padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        icon,
                        null,
                        tint = if (selected) VybText else VybMuted,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        label,
                        color = if (selected) VybText else VybMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 7.dp)
                    )
                }
                Box(
                    Modifier.fillMaxWidth().padding(top = 9.dp)
                        .background(if (selected) Color.White else Color.Transparent)
                        .size(height = 2.dp, width = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileGridTile(post: SocialPost) {
    Box(
        Modifier.fillMaxWidth().aspectRatio(1f).background(VybPanelLifted)
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
        Surface(
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
            color = Color(0xCC071426),
            shape = CircleShape
        ) {
            Text(
                "♡ ${post.reactions}",
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        color = VybPanelLifted.copy(alpha = .88f),
        border = BorderStroke(1.dp, VybBorder),
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = VybText, modifier = Modifier.size(21.dp))
        }
    }
}

private data class ProfileLink(val network: String, val url: String)
