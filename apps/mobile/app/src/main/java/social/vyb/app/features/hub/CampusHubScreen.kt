package social.vyb.app.features.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CampusHubScreen(modifier: Modifier = Modifier) {
    val repository = remember { CampusHubRepository() }
    val viewModel: CampusHubViewModel = viewModel(
        factory = CampusHubViewModelFactory(repository)
    )
    val state by viewModel.state.collectAsState()

    when {
        state.selectedEvent != null -> EventDetail(
            event = state.selectedEvent!!,
            busy = state.busyId == state.selectedEvent!!.id,
            error = state.error,
            onBack = viewModel::closeEvent,
            onSave = { viewModel.toggleSave(state.selectedEvent!!.id) },
            onRegister = { viewModel.register(state.selectedEvent!!.id) },
            modifier = modifier
        )
        state.selectedCommunity != null -> CommunityDetail(
            detail = state.selectedCommunity!!,
            members = state.communityMembers,
            onBack = viewModel::closeCommunity,
            modifier = modifier
        )
        else -> HubRoot(
            state = state,
            onTab = viewModel::selectTab,
            onRefresh = viewModel::refresh,
            onEvent = viewModel::openEvent,
            onCommunity = viewModel::openCommunity,
            modifier = modifier
        )
    }
}

@Composable
private fun HubRoot(
    state: CampusHubUiState,
    onTab: (CampusHubTab) -> Unit,
    onRefresh: () -> Unit,
    onEvent: (HubEvent) -> Unit,
    onCommunity: (HubCommunity) -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxWidth < 360.dp || maxHeight < 700.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 18.dp, vertical = if (compact) 8.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Campus Hub",
                        style = if (compact) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = VybText
                    )
                    Text(
                        "Events, knowledge and your circles",
                        color = VybMuted,
                        maxLines = if (compact) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onRefresh, enabled = !state.isLoading && !state.isRefreshing) {
                    if (state.isRefreshing) CircularProgressIndicator(Modifier.padding(9.dp))
                    else Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = VybText)
                }
            }
            ScrollableTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
                containerColor = VybPanel,
                contentColor = VybIndigo,
                edgePadding = 0.dp
            ) {
                CampusHubTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onTab(tab) },
                        text = { Text(tab.title, maxLines = 1) }
                    )
                }
            }
            when {
                state.isLoading -> CenterMessage { VybLoadingMark(width = 96.dp) }
                state.error != null &&
                    state.events.isEmpty() &&
                    state.resources.isEmpty() &&
                    state.communities.isEmpty() -> CenterMessage {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRefresh) { Text("Try again") }
                    }
                }
                else -> Column(Modifier.fillMaxSize()) {
                    state.error?.let {
                        Surface(color = MaterialTheme.colorScheme.errorContainer) {
                            Text(
                                it,
                                Modifier.fillMaxWidth().padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    when (state.selectedTab) {
                        CampusHubTab.Events -> EventList(state.events, onEvent)
                        CampusHubTab.Resources -> ResourceList(state.resources)
                        CampusHubTab.Communities -> CommunityList(
                            state.communities,
                            state.busyId,
                            onCommunity
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventList(events: List<HubEvent>, onOpen: (HubEvent) -> Unit) {
    if (events.isEmpty()) {
        HubEmptyState(
            icon = { Icon(Icons.Default.CalendarMonth, null, tint = VybTeal) },
            title = "No campus events yet",
            body = "Events hosted by your college and communities will appear here."
        )
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }
        items(events, key = HubEvent::id) { event ->
            Card(
                Modifier.fillMaxWidth().clickable { onOpen(event) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = VybPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(event.category.ifBlank { "Campus event" }, color = MaterialTheme.colorScheme.primary)
                    Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${formatDate(event.startsAt)} • ${event.location.ifBlank { "Campus" }}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(event.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            event.viewerRegistration != null -> "Registered • ${event.viewerRegistration.status}"
                            event.spotsLeft != null -> "${event.spotsLeft} spots left"
                            else -> event.passLabel
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ResourceList(resources: List<HubResource>) {
    if (resources.isEmpty()) {
        HubEmptyState(
            icon = { Icon(Icons.Default.Description, null, tint = VybTeal) },
            title = "No resources published",
            body = "Notes, guides and previous-year papers will show up here."
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(resources, key = HubResource::id) { resource ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(resource.type.uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                Text(resource.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (resource.description.isNotBlank()) {
                    Text(resource.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    "${resource.files.size} file(s) • ${resource.downloads} downloads",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun CommunityList(
    communities: List<HubCommunity>,
    busyId: String?,
    onOpen: (HubCommunity) -> Unit
) {
    if (communities.isEmpty()) {
        HubEmptyState(
            icon = { Icon(Icons.Default.Groups, null, tint = VybTeal) },
            title = "Find your campus circles",
            body = "Communities you join will be collected here."
        )
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(communities, key = HubCommunity::id) { community ->
            Row(
                Modifier.fillMaxWidth().clickable(enabled = busyId == null) { onOpen(community) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(community.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${community.type.replaceFirstChar(Char::uppercase)} • ${community.memberCount} members",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (busyId == community.id) CircularProgressIndicator()
                else Text(community.membershipRole ?: "Member", color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EventDetail(
    event: HubEvent,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Event details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(Modifier.padding(20.dp)) {
                Text(event.category.ifBlank { "Campus event" }, color = MaterialTheme.colorScheme.primary)
                Text(event.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Text(formatDate(event.startsAt), style = MaterialTheme.typography.titleMedium)
                Text(event.location.ifBlank { "Campus" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Hosted by ${event.host.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Text(event.description.ifBlank { "No description provided." })
                Spacer(Modifier.height(18.dp))
                Text(
                    "${event.passLabel} • ${event.spotsLeft?.let { "$it spots left" } ?: "Open capacity"}",
                    fontWeight = FontWeight.SemiBold
                )
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onSave, enabled = !busy) {
                        Icon(
                            if (event.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = null
                        )
                        Text(if (event.isSaved) "Saved" else "Save")
                    }
                    Button(
                        onClick = onRegister,
                        enabled = !busy && event.isRegistrationOpen && event.viewerRegistration == null
                    ) {
                        if (busy) CircularProgressIndicator()
                        else Text(
                            if (event.viewerRegistration != null) "Registered"
                            else if (event.responseMode == "apply") "Apply"
                            else "Register"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityDetail(
    detail: CommunityDetailDto,
    members: List<HubCommunityMember>,
    onBack: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Community", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Column(Modifier.padding(20.dp)) {
                Text(detail.community.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${detail.community.memberCount} members • ${detail.viewer.role ?: "Viewer"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "${detail.summary.postCount ?: 0} posts • ${detail.summary.resourceCount ?: 0} resources • ${detail.summary.eventCount ?: 0} events"
                )
                Spacer(Modifier.height(20.dp))
                Text("Members", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        if (members.isEmpty()) {
            item { Text("No members to show.", Modifier.padding(20.dp)) }
        } else {
            items(members, key = HubCommunityMember::membershipId) { member ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text(member.displayName, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOfNotNull(member.username?.let { "@$it" }, member.course, member.branch)
                            .joinToString(" • "),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CenterMessage(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun HubEmptyState(
    icon: @Composable () -> Unit,
    title: String,
    body: String
) {
    val compactHeight = LocalConfiguration.current.screenHeightDp < 700
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 18.dp,
            vertical = if (compactHeight) 12.dp else 28.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                shape = RoundedCornerShape(24.dp),
                color = VybPanel.copy(alpha = .82f),
                border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder)
            ) {
                Column(
                    Modifier.padding(
                        horizontal = if (compactHeight) 18.dp else 24.dp,
                        vertical = if (compactHeight) 16.dp else 28.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(if (compactHeight) 46.dp else 58.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = VybTeal.copy(alpha = .12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) { icon() }
                    }
                    Text(
                        title,
                        Modifier.padding(top = if (compactHeight) 10.dp else 16.dp),
                        color = VybText,
                        style = if (compactHeight) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        body,
                        Modifier.padding(top = 6.dp),
                        color = VybMuted,
                        style = if (compactHeight) MaterialTheme.typography.bodySmall
                        else MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun formatDate(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a")
        .format(Instant.parse(value).atZone(ZoneId.systemDefault()))
}.getOrDefault(value.ifBlank { "Date to be announced" })
