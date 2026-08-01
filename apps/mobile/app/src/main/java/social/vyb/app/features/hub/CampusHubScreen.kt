package social.vyb.app.features.hub

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import social.vyb.app.ui.VybBorder
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybTeal
import social.vyb.app.ui.VybText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CampusHubScreen(
    modifier: Modifier = Modifier,
    initialEventId: String? = null,
    onInitialEventConsumed: (() -> Unit)? = null,
    refreshSignal: Int = 0,
) {
    val repository = remember { CampusHubRepository() }
    val viewModel: CampusHubViewModel = viewModel(
        factory = CampusHubViewModelFactory(repository)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) viewModel.refresh()
    }

    LaunchedEffect(initialEventId) {
        val eventId = initialEventId?.trim()?.takeIf(String::isNotEmpty) ?: return@LaunchedEffect
        viewModel.openEventById(eventId) { onInitialEventConsumed?.invoke() }
    }

    when {
        state.hostEditorOpen -> HostEventEditor(
            existing = state.hostEditorEvent,
            busy = state.busyId == "host-event",
            error = state.error,
            onBack = viewModel::closeHostEditor,
            onSave = viewModel::saveHostedEvent,
            modifier = modifier,
        )
        state.registrationAdminEvent != null -> RegistrationAdmin(
            event = state.registrationAdminEvent!!,
            registrations = state.hostRegistrations,
            loading = state.registrationsLoading,
            busyId = state.busyId,
            error = state.error,
            onBack = viewModel::closeRegistrationAdmin,
            onReview = viewModel::reviewRegistration,
            modifier = modifier,
        )
        state.selectedEvent != null -> EventDetail(
            event = state.selectedEvent!!,
            busy = state.busyId == state.selectedEvent!!.id,
            error = state.error,
            onBack = viewModel::closeEvent,
            onSave = { viewModel.toggleSave(state.selectedEvent!!.id) },
            onRegister = { viewModel.register(state.selectedEvent!!.id) },
            onEdit = { viewModel.openHostEditor(state.selectedEvent) },
            onManageRegistrations = { viewModel.openRegistrationAdmin(state.selectedEvent!!) },
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
            onSaveEvent = viewModel::toggleSave,
            onHostEvent = { viewModel.openHostEditor() },
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
    onSaveEvent: (String) -> Unit,
    onHostEvent: () -> Unit,
    modifier: Modifier
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        Column(Modifier.fillMaxSize()) {
            if (!compact) {
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
                IconButton(onClick = onHostEvent) {
                    Icon(Icons.Default.Add, contentDescription = "Host an event", tint = VybText)
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
                        CampusHubTab.Events -> EventDashboard(
                            events = state.events,
                            busyId = state.busyId,
                            onOpen = onEvent,
                            onSave = onSaveEvent,
                            onHostEvent = onHostEvent
                        )
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
private fun EventDashboard(
    events: List<HubEvent>,
    busyId: String?,
    onOpen: (HubEvent) -> Unit,
    onSave: (String) -> Unit,
    onHostEvent: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf("Upcoming") }
    var category by remember { mutableStateOf("All") }
    var notificationsOpen by remember { mutableStateOf(false) }
    val categories = remember(events) {
        listOf("All") + events.map(HubEvent::category).filter(String::isNotBlank).distinct()
    }
    val filtered = remember(events, query, scope, category) {
        filterHubEvents(events, query, scope, category)
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 700.dp
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search clubs, nights, workshops") },
                trailingIcon = {
                    IconButton(onClick = { notificationsOpen = !notificationsOpen }) {
                        Icon(Icons.Default.NotificationsNone, "Event notifications")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            if (notificationsOpen) {
                Surface(
                    color = VybPanel,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Saved and registered event reminders will appear here.",
                        color = VybMuted,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Upcoming", "Saved", "Ended").forEach { option ->
                    val selected = scope == option
                    Column(
                        Modifier.width(100.dp).clickable { scope = option },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            option.uppercase(),
                            color = if (selected) VybText else VybMuted,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                        Box(
                            Modifier.fillMaxWidth().height(2.dp)
                                .background(if (selected) VybText else VybBorder)
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { option ->
                    FilterChip(
                        selected = category == option,
                        onClick = { category = option },
                        label = { Text(option) }
                    )
                }
            }
            if (filtered.isEmpty()) {
                HubEmptyState(
                    icon = { Icon(Icons.Default.CalendarMonth, null, tint = VybTeal) },
                    title = "No events match this lane",
                    body = "Try another category, switch scopes, or clear the search to see more campus activity."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (tablet) 2 else 1),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(filtered, key = HubEvent::id) { event ->
                        EventFeedCard(
                            event = event,
                            busy = busyId == event.id,
                            onOpen = { onOpen(event) },
                            onSave = { onSave(event.id) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
        Button(
            onClick = onHostEvent,
            modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(Icons.Default.Add, null)
            Text("Host event", modifier = Modifier.padding(start = 7.dp))
        }
    }
}

@Composable
private fun EventFeedCard(
    event: HubEvent,
    busy: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = VybPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder)
    ) {
        Column {
            val media = event.media.firstOrNull()
            if (media != null && media.kind != "video") {
                VybRemoteImage(
                    url = media.url,
                    contentDescription = event.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 7f)
                        .background(VybIndigo.copy(alpha = .16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = VybTeal, modifier = Modifier.size(36.dp))
                }
            }
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        event.club.ifBlank { event.category.ifBlank { "Campus event" } },
                        color = VybTeal,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onSave, enabled = !busy) {
                        Icon(
                            if (event.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            if (event.isSaved) "Remove saved event" else "Save event",
                            tint = if (event.isSaved) VybTeal else VybMuted
                        )
                    }
                }
                Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${formatDate(event.startsAt)} · ${event.location.ifBlank { "Campus" }}",
                    color = VybMuted
                )
                Text(
                    event.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                event.viewerRegistration != null ->
                                    event.viewerRegistration.status.replaceFirstChar { it.uppercase() }
                                event.spotsLeft != null -> "${event.spotsLeft} spots left"
                                else -> event.passLabel
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${event.interestCount} interested · ${event.registrationSummary.total} registrations",
                            color = VybMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { shareEvent(context, event) }) {
                        Icon(Icons.Default.Share, "Share event", tint = VybText)
                    }
                }
            }
        }
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
    onEdit: () -> Unit,
    onManageRegistrations: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
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
        event.media.firstOrNull()?.takeIf { it.kind != "video" }?.let { media ->
            item {
                VybRemoteImage(
                    url = media.url,
                    contentDescription = event.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop
                )
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
                if (event.isHostedByViewer) {
                    Text(
                        "${event.registrationSummary.total} registrations · " +
                            "${event.registrationSummary.submitted} awaiting review",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = onEdit, enabled = !busy) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Text("Edit")
                        }
                        Button(onClick = onManageRegistrations, enabled = !busy) {
                            Icon(Icons.Default.HowToReg, contentDescription = null)
                            Text("Registrations")
                        }
                        IconButton(onClick = { shareEvent(context, event) }) {
                            Icon(Icons.Default.Share, "Share event")
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                        IconButton(onClick = { shareEvent(context, event) }) {
                            Icon(Icons.Default.Share, "Share event")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HostEventEditor(
    existing: HubEvent?,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onSave: (HubEventHostDraft) -> Unit,
    modifier: Modifier,
) {
    var draft by remember(existing?.id) {
        mutableStateOf(existing?.toHostDraft() ?: HubEventHostDraft())
    }
    val localError = draft.validationError()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, enabled = !busy) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (existing == null) "Host an event" else "Edit event",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Core event and registration settings",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HostField(draft.title, { draft = draft.copy(title = it) }, "Event title")
                HostField(draft.club, { draft = draft.copy(club = it) }, "Hosting club")
                HostField(draft.category, { draft = draft.copy(category = it) }, "Category")
                HostField(
                    draft.description,
                    { draft = draft.copy(description = it) },
                    "Description",
                    minLines = 3,
                )
                HostField(draft.location, { draft = draft.copy(location = it) }, "Location")
                HostField(
                    draft.startsAt,
                    { draft = draft.copy(startsAt = it) },
                    "Starts at (ISO UTC)",
                    supporting = "Example: 2026-08-15T10:00:00Z",
                )
                HostField(
                    draft.endsAt,
                    { draft = draft.copy(endsAt = it) },
                    "Ends at (optional ISO UTC)",
                )
                Text("Pass type", fontWeight = FontWeight.SemiBold)
                ChoiceChips(
                    options = listOf("free" to "Free", "rsvp" to "RSVP", "paid" to "Paid"),
                    selected = draft.passKind,
                    onSelected = { draft = draft.copy(passKind = it) },
                )
                HostField(draft.passLabel, { draft = draft.copy(passLabel = it) }, "Pass label")
                HostField(
                    draft.capacity,
                    { draft = draft.copy(capacity = it.filter(Char::isDigit)) },
                    "Capacity (optional)",
                )
                Text("Response mode", fontWeight = FontWeight.SemiBold)
                ChoiceChips(
                    options = listOf(
                        "interest" to "Interest",
                        "register" to "Register",
                        "apply" to "Application",
                    ),
                    selected = draft.responseMode,
                    onSelected = { draft = draft.copy(responseMode = it) },
                )
                if (draft.responseMode != "interest") {
                    HostField(
                        draft.registrationClosesAt,
                        { draft = draft.copy(registrationClosesAt = it) },
                        "Registration closes (optional ISO UTC)",
                    )
                }
                if (existing?.media?.isNotEmpty() == true) {
                    Text(
                        "${existing.media.size} existing media asset(s) will be preserved. " +
                            "Media replacement remains available on web until native R2 upload ships.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (
                    existing?.registrationConfig?.formFields?.isNotEmpty() == true ||
                    existing?.registrationConfig?.allowAttachments == true ||
                    existing?.registrationConfig?.entryMode == "team"
                ) {
                    Text(
                        "Existing team, attachment and custom-form settings are preserved. " +
                            "Use the web host editor to change those advanced fields.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                (error ?: localError)?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = { onSave(draft) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy && localError == null,
                ) {
                    Text(if (existing == null) "Publish event" else "Save changes")
                }
            }
        }
    }
}

@Composable
private fun HostField(
    value: String,
    onValueChanged: (String) -> Unit,
    label: String,
    minLines: Int = 1,
    supporting: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChanged,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines,
        maxLines = if (minLines > 1) 5 else 1,
        supportingText = supporting?.let { text -> ({ Text(text) }) },
    )
}

@Composable
private fun ChoiceChips(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun RegistrationAdmin(
    event: HubEvent,
    registrations: List<HubEventRegistration>,
    loading: Boolean,
    busyId: String?,
    error: String?,
    onBack: () -> Unit,
    onReview: (String, String, String?) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, enabled = busyId == null) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Registrations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(event.title, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RegistrationMetric("Total", event.registrationSummary.total)
                RegistrationMetric("Pending", event.registrationSummary.submitted)
                RegistrationMetric("Approved", event.registrationSummary.approved)
            }
        }
        error?.let { message ->
            item {
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        if (!loading && registrations.isEmpty()) {
            item {
                Text(
                    "No registrations yet.",
                    modifier = Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(registrations, key = HubEventRegistration::id) { registration ->
            RegistrationReviewCard(
                registration = registration,
                busy = busyId == "registration:${registration.id}",
                onReview = { status, note -> onReview(registration.id, status, note) },
            )
        }
    }
}

@Composable
private fun RegistrationMetric(label: String, value: Int) {
    Column {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RegistrationReviewCard(
    registration: HubEventRegistration,
    busy: Boolean,
    onReview: (String, String?) -> Unit,
) {
    var reviewNote by remember(registration.id, registration.reviewNote) {
        mutableStateOf(registration.reviewNote.orEmpty())
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = VybPanel),
        border = androidx.compose.foundation.BorderStroke(1.dp, VybBorder),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(registration.attendee.displayName, fontWeight = FontWeight.Bold)
                    Text(
                        "@${registration.attendee.username} · ${registration.teamSize} attendee(s)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    registration.status.replaceFirstChar(Char::uppercase),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            registration.teamName?.takeIf(String::isNotBlank)?.let { Text("Team: $it") }
            registration.note?.takeIf(String::isNotBlank)?.let { Text(it) }
            registration.answers.forEach { answer ->
                Text(
                    "${answer.label}: ${answer.value}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (registration.attachments.isNotEmpty()) {
                Text(
                    "${registration.attachments.size} attachment(s) submitted; review/download remains web-only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = reviewNote,
                onValueChange = { reviewNote = it.take(500) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Review note (optional)") },
                maxLines = 3,
                enabled = !busy,
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf(
                    "approved" to "Approve",
                    "waitlisted" to "Waitlist",
                    "rejected" to "Reject",
                ).forEach { (status, label) ->
                    OutlinedButton(
                        onClick = { onReview(status, reviewNote) },
                        enabled = !busy && registration.status != status,
                    ) {
                        Text(label)
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
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
    val compactHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() < 700.dp
    }
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

private fun shareEvent(context: Context, event: HubEvent) {
    val route = "https://www.vybnet.app/events?event=${event.id}"
    val text = buildString {
        append(event.title)
        append("\n")
        append(formatDate(event.startsAt))
        append(" · ")
        append(event.location.ifBlank { "Campus" })
        append("\n")
        append(route)
    }
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, event.title)
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Share event"
        )
    )
}

private fun formatDate(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("EEE, d MMM • h:mm a")
        .format(Instant.parse(value).atZone(ZoneId.systemDefault()))
}.getOrDefault(value.ifBlank { "Date to be announced" })
