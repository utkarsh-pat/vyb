package social.vyb.app.features.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import social.vyb.app.data.ProfileRecord
import social.vyb.app.features.social.SocialPost
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybPanelLifted
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybRemoteVideo
import social.vyb.app.ui.VybResponsiveFrame
import social.vyb.app.ui.VybText

@Composable
internal fun ProfileParityOverview(
    state: ProfileUiState,
    onTab: (String) -> Unit,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    onConnections: (String) -> Unit,
    onDismissMessage: () -> Unit
) {
    val profile = requireNotNull(state.privateProfile)
    val publicProfile = requireNotNull(state.publicProfile)
    val uriHandler = LocalUriHandler.current
    val links = listOf(
        "github",
        "instagram",
        "email",
        "twitter",
        "x",
        "linkedin",
        "codeforces",
        "leetcode"
    ).mapNotNull { network ->
        safeSocialUrl(network, profile.socialLinks?.get(network))?.let { url ->
            ProfileSocialLink(network, url)
        }
    }
    val posts = when (state.activeTab) {
        "vibes" -> publicProfile.posts.filter {
            it.kind == "video" || it.placement == "vibe"
        }
        "saved" -> emptyList()
        else -> publicProfile.posts.filterNot {
            it.kind == "video" || it.placement == "vibe"
        }
    }

    VybResponsiveFrame(Modifier.fillMaxSize(), maxContentWidth = 1040.dp) { layout ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(if (layout.wide) 10.dp else 2.dp),
            verticalArrangement = Arrangement.spacedBy(if (layout.wide) 10.dp else 2.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    ProfileParityTopBar(profile.username, onEdit, onSettings, layout.horizontalPadding)
                    ProfileHero(
                        profile = profile,
                        posts = publicProfile.stats.posts,
                        followers = publicProfile.stats.followers,
                        following = publicProfile.stats.following,
                        links = links,
                        wide = layout.wide,
                        horizontalPadding = layout.horizontalPadding,
                        onConnections = onConnections,
                        onOpenLink = uriHandler::openUri
                    )
                    ProfileParityMessage(
                        state = state,
                        onDismiss = onDismissMessage,
                        modifier = Modifier.padding(horizontal = layout.horizontalPadding)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = layout.horizontalPadding,
                                end = layout.horizontalPadding,
                                top = 12.dp,
                                bottom = 10.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProfileTab(
                            label = "Posts",
                            selected = state.activeTab == "posts",
                            icon = Icons.Default.GridView,
                            modifier = Modifier.weight(1f)
                        ) { onTab("posts") }
                        ProfileTab(
                            label = "Vibes",
                            selected = state.activeTab == "vibes",
                            icon = Icons.Default.VideoLibrary,
                            modifier = Modifier.weight(1f)
                        ) { onTab("vibes") }
                        ProfileTab(
                            label = "Saved",
                            selected = state.activeTab == "saved",
                            icon = Icons.Default.BookmarkBorder,
                            modifier = Modifier.weight(1f)
                        ) { onTab("saved") }
                    }
                }
            }
            if (posts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    VybEmptyState(
                        icon = when (state.activeTab) {
                            "vibes" -> Icons.Default.VideoLibrary
                            "saved" -> Icons.Default.BookmarkBorder
                            else -> Icons.Default.GridView
                        },
                        title = when (state.activeTab) {
                            "vibes" -> "No vibes yet"
                            "saved" -> "Your saved shelf is empty"
                            else -> "No posts yet"
                        },
                        body = when (state.activeTab) {
                            "saved" -> "Posts and vibes you save will appear here."
                            else -> "Your published ${state.activeTab} will appear here."
                        },
                        modifier = Modifier.padding(20.dp)
                    )
                }
            } else {
                items(posts, key = SocialPost::id) { post ->
                    ProfileMediaTile(post, layout.wide)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ProfileParityTopBar(
    username: String,
    onEdit: () -> Unit,
    onSettings: () -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Your profile", color = VybText, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text("@$username", color = VybMuted, fontSize = 13.sp)
        }
        ProfileCircleAction(Icons.Default.Edit, "Edit profile", onEdit)
        Spacer(Modifier.size(6.dp))
        ProfileCircleAction(Icons.Default.Settings, "Settings", onSettings)
    }
}

@Composable
private fun ProfileCircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(36.dp),
            color = VybPanelLifted,
            border = BorderStroke(1.dp, VybBorder),
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = VybText, modifier = Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun ProfileHero(
    profile: ProfileRecord,
    posts: Int,
    followers: Int,
    following: Int,
    links: List<ProfileSocialLink>,
    wide: Boolean,
    horizontalPadding: androidx.compose.ui.unit.Dp,
    onConnections: (String) -> Unit,
    onOpenLink: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        color = VybPanel.copy(alpha = .88f),
        border = BorderStroke(1.dp, VybBorder),
        shape = RoundedCornerShape(if (wide) 24.dp else 18.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (wide) 112.dp else 70.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                VybIndigo.copy(alpha = .8f),
                                Color(0xFF7C3AED).copy(alpha = .66f),
                                Color(0xFF14B8A6).copy(alpha = .58f)
                            )
                        )
                    )
            )
            if (wide) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(profile.fullName, profile.avatarUrl, 104)
                    ProfileIdentity(
                        profile,
                        links,
                        onOpenLink,
                        Modifier.weight(1f).padding(horizontal = 20.dp)
                    )
                    ProfileStats(posts, followers, following, onConnections)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatar(profile.fullName, profile.avatarUrl, 60)
                        ProfileStats(
                            posts,
                            followers,
                            following,
                            onConnections,
                            Modifier.weight(1f).padding(start = 4.dp)
                        )
                    }
                    ProfileIdentity(
                        profile,
                        links,
                        onOpenLink,
                        Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileIdentity(
    profile: ProfileRecord,
    links: List<ProfileSocialLink>,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(profile.fullName, color = VybText, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text("@${profile.username}", color = VybMuted, fontSize = 13.sp)
        Text(
            listOf(profile.course, profile.stream).filter(String::isNotBlank).joinToString(" · "),
            color = VybMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        profile.bio?.takeIf(String::isNotBlank)?.let {
            Text(it, color = VybText, modifier = Modifier.padding(top = 8.dp))
        }
        if (links.isNotEmpty()) {
            Row(Modifier.padding(top = 6.dp)) {
                links.distinctBy(ProfileSocialLink::label).forEach { link ->
                    Box(
                        modifier = Modifier.size(48.dp).clickable { onOpenLink(link.url) },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            color = VybPanelLifted,
                            border = BorderStroke(1.dp, VybBorder),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    socialIcon(link.network),
                                    link.label,
                                    tint = VybText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ProfileSocialLink(
    val network: String,
    val url: String
) {
    val label: String
        get() = when (network) {
            "twitter", "x" -> "X"
            "linkedin" -> "LinkedIn"
            "github" -> "GitHub"
            "instagram" -> "Instagram"
            "codeforces" -> "Codeforces"
            "leetcode" -> "LeetCode"
            else -> "Email"
        }
}

private fun socialIcon(network: String): androidx.compose.ui.graphics.vector.ImageVector =
    when (network) {
        "instagram" -> Icons.Default.CameraAlt
        "email" -> Icons.Default.Email
        "twitter", "x" -> Icons.Default.AlternateEmail
        "linkedin" -> Icons.Default.Work
        else -> Icons.Default.Code
    }

@Composable
private fun ProfileStats(
    posts: Int,
    followers: Int,
    following: Int,
    onConnections: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileMetric(posts, "Posts")
        ProfileMetric(followers, "Followers") { onConnections("followers") }
        ProfileMetric(following, "Following") { onConnections("following") }
    }
}

@Composable
private fun ProfileMetric(value: Int, label: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value.toString(), color = VybText, fontWeight = FontWeight.Black, fontSize = 15.sp)
        Text(label, color = VybMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileTab(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = if (selected) VybIndigo.copy(alpha = .18f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) VybIndigo.copy(alpha = .42f) else VybBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (selected) VybText else VybMuted, modifier = Modifier.size(17.dp))
            Text(
                label,
                color = if (selected) VybText else VybMuted,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 7.dp)
            )
        }
    }
}

@Composable
private fun ProfileMediaTile(post: SocialPost, wide: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(if (wide) 14.dp else 2.dp)),
        color = VybPanelLifted,
        shape = RoundedCornerShape(if (wide) 14.dp else 2.dp)
    ) {
        Box {
            val mediaUrl = post.mediaUrl?.takeIf(String::isNotBlank)
            when {
                mediaUrl != null && post.kind == "video" -> VybRemoteVideo(
                    url = mediaUrl,
                    contentDescription = "${post.author.displayName} vibe",
                    modifier = Modifier.fillMaxSize()
                )
                mediaUrl != null -> VybRemoteImage(
                    url = mediaUrl,
                    contentDescription = "${post.author.displayName} post",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                else -> Column(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(VybIndigo.copy(alpha = .28f), VybPanel)
                            )
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        post.title.ifBlank { post.body },
                        color = VybText,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(7.dp),
                color = Color.Black.copy(alpha = .58f),
                shape = CircleShape
            ) {
                Text(
                    "${post.reactions} likes",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, url: String?, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(VybIndigo, Color(0xFF14B8A6)))),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Text(
                name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = (size / 3).sp
            )
        } else {
            VybRemoteImage(
                url = url,
                contentDescription = "$name profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun ProfileParityMessage(
    state: ProfileUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = state.error ?: state.notice ?: return
    Surface(
        modifier = modifier.fillMaxWidth().padding(top = 10.dp),
        color = if (state.error != null) Color(0xFF5B2031) else Color(0xFF123F3B),
        shape = RoundedCornerShape(14.dp),
        onClick = onDismiss
    ) {
        Text(message, color = VybText, modifier = Modifier.padding(13.dp))
    }
}
