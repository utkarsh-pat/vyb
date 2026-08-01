package social.vyb.app.features.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import social.vyb.app.features.search.SearchRepository
import social.vyb.app.ui.VybMuted
import social.vyb.app.features.stories.toCompactMetric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialReactionsSheet(
    reactionCount: Int,
    members: List<ReactionMember>,
    onOpenProfile: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val repository = remember { SearchRepository() }
    var followedUsernames by remember { mutableStateOf(emptySet<String>()) }
    var busyUsernames by remember { mutableStateOf(emptySet<String>()) }
    var followError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(members) {
        followedUsernames = members
            .filter(ReactionMember::viewerIsFollowing)
            .mapTo(mutableSetOf(), ReactionMember::username)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.75f).padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                "Reactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
            )

            // Stats Row
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("♡ ${reactionCount.toCompactMetric()}", fontWeight = FontWeight.SemiBold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            followError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(50.dp),
                placeholder = { Text("Search", color = VybMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = VybMuted) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            // Member List
            val filteredMembers = members.filter {
                it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.username.contains(searchQuery, ignoreCase = true)
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredMembers, key = { it.membershipId }) { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenProfile(member.username)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SocialAvatar(
                            avatarUrl = member.avatarUrl,
                            displayName = member.displayName,
                            size = 48.dp
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                        ) {
                            Text(member.username, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(member.displayName, color = VybMuted, fontSize = 13.sp)
                        }
                        val isFollowing = member.username in followedUsernames
                        if (!member.isViewer) Button(
                            enabled = member.username !in busyUsernames,
                            onClick = {
                                val next = !isFollowing
                                busyUsernames = busyUsernames + member.username
                                followError = null
                                scope.launch {
                                    runCatching { repository.setFollowing(member.username, next) }
                                        .onSuccess { response ->
                                            followedUsernames = if (response.isFollowing) {
                                                followedUsernames + member.username
                                            } else {
                                                followedUsernames - member.username
                                            }
                                        }
                                        .onFailure { followError = it.message ?: "Could not update follow status." }
                                    busyUsernames = busyUsernames - member.username
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF4C51F7),
                                contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (member.username in busyUsernames) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                if (filteredMembers.isEmpty()) {
                    item {
                        Text(
                            "No users found.",
                            color = VybMuted,
                            modifier = Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}
