package social.vyb.app.features.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import social.vyb.app.ui.VybBackground
import social.vyb.app.ui.VybIndigo
import social.vyb.app.ui.VybMuted
import social.vyb.app.ui.VybPanel
import social.vyb.app.ui.VybText
import social.vyb.app.ui.VybEmptyState
import social.vyb.app.ui.VybLoadingMark
import social.vyb.app.ui.VybRemoteImage
import social.vyb.app.ui.VybRemoteVideo
import social.vyb.app.ui.VybResponsiveFrame
import java.text.NumberFormat
import java.util.Locale

/**
 * Single integration entry point for the native marketplace feature.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketFeatureScreen(
    modifier: Modifier = Modifier,
    initialTargetId: String? = null,
    initialTargetType: String? = null,
    onInitialTargetConsumed: (() -> Unit)? = null,
    refreshSignal: Int = 0,
    marketViewModel: MarketViewModel = viewModel(),
) {
    val state by marketViewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) marketViewModel.refresh()
    }

    LaunchedEffect(initialTargetId, initialTargetType, state.dashboard) {
        val targetId = initialTargetId?.trim()?.takeIf(String::isNotEmpty) ?: return@LaunchedEffect
        if (state.dashboard != null && marketViewModel.selectById(targetId, initialTargetType)) {
            onInitialTargetConsumed?.invoke()
        }
    }

    LaunchedEffect(state.error, state.notice) {
        val message = state.error ?: state.notice
        if (message != null) {
            snackbar.showSnackbar(message)
            marketViewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = VybBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Unit
        },
        floatingActionButton = {
            if (state.dashboard != null) {
                FloatingActionButton(
                    onClick = { marketViewModel.setComposer(true) },
                    containerColor = VybIndigo,
                    contentColor = VybText
                ) { Icon(Icons.Default.Add, contentDescription = "Create marketplace post") }
            }
        },
    ) { padding ->
        VybResponsiveFrame(
            modifier = Modifier.fillMaxSize().padding(padding),
            maxContentWidth = 900.dp
        ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.mutating) LinearProgressIndicator(Modifier.fillMaxWidth())
            PwaMarketControls(
                state = state,
                dashboard = state.dashboard,
                onTab = marketViewModel::selectTab,
                onQuery = marketViewModel::setQuery,
                onCategory = marketViewModel::selectCategory,
                onSort = marketViewModel::selectSort
            )
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    VybLoadingMark(width = 104.dp)
                }

                state.dashboard == null -> MarketEmpty(
                    title = "Market could not load",
                    body = state.error ?: "Check your connection and try again.",
                    action = marketViewModel::refresh,
                )

                else -> MarketContent(
                    state = state,
                    onSelect = marketViewModel::select,
                    onSave = marketViewModel::toggleSave,
                )
            }
        }
        }
    }

    state.selected?.let { selected ->
        MarketDetailDialog(
            detail = selected,
            viewerId = state.dashboard?.viewer?.userId.orEmpty(),
            busy = state.mutating,
            onDismiss = { marketViewModel.select(null) },
            onSave = marketViewModel::toggleSave,
            onContact = marketViewModel::contact,
            onSold = marketViewModel::markSold,
        )
    }
    if (state.showComposer) {
        MarketComposerDialog(
            busy = state.mutating,
            onDismiss = { marketViewModel.setComposer(false) },
            onCreate = marketViewModel::create,
        )
    }
}

@Composable
private fun MarketDiscoveryControls(
    state: MarketUiState,
    categories: List<String>,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSortSelected: (MarketSort) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search market") },
            placeholder = {
                Text(if (state.tab == "sale") "Items, sellers or categories" else "Requests, people or categories")
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear market search")
                    }
                }
            },
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.category == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All categories") },
            )
            categories.forEach { category ->
                FilterChip(
                    selected = state.category?.equals(category, ignoreCase = true) == true,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MarketSort.entries.forEach { option ->
                FilterChip(
                    selected = state.sort == option,
                    onClick = { onSortSelected(option) },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VybIndigo.copy(alpha = 0.24f),
                        selectedLabelColor = VybText,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MarketTabs(selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("sale" to "For sale", "buying" to "Wanted", "lend" to "Lend").forEach { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelected(id) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = VybBackground,
                    labelColor = VybMuted,
                    selectedContainerColor = VybIndigo.copy(alpha = 0.24f),
                    selectedLabelColor = VybText,
                ),
            )
        }
    }
}

@Composable
private fun MarketContent(
    state: MarketUiState,
    onSelect: (MarketDetail) -> Unit,
    onSave: (String) -> Unit,
) {
    val dashboard = checkNotNull(state.dashboard)
    val visible = dashboard.visibleContent(state)
    val listings = visible.listings
    val requests = visible.requests

    if ((state.tab == "sale" && listings.isEmpty()) || (state.tab != "sale" && requests.isEmpty())) {
        val hasFilters = state.query.isNotBlank() || state.category != null
        MarketEmpty(
            title = when {
                state.showSavedOnly -> "No saved listings"
                hasFilters -> "No listings match that search yet."
                else -> "No listings match that search yet."
            },
            body = when {
                state.showSavedOnly -> "Bookmark a listing and it will appear here."
                else -> "Try another category or clear the search to see more items from the campus marketplace."
            },
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 96.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.tab == "sale") {
            items(listings, key = { it.id }) { listing ->
                ListingCard(
                    listing = listing,
                    isOwner = listing.seller.userId == dashboard.viewer.userId,
                    onClick = { onSelect(MarketDetail.Listing(listing)) },
                    onSave = { onSave(listing.id) },
                )
            }
        } else {
            items(requests, key = { it.id }) { request ->
                RequestCard(
                    request = request,
                    onClick = { onSelect(MarketDetail.Request(request)) },
                )
            }
        }
    }
}

@Composable
private fun ListingCard(
    listing: MarketListing,
    isOwner: Boolean,
    onClick: () -> Unit,
    onSave: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            listing.media.firstOrNull { it.url.isNotBlank() }?.let { media ->
                MarketMediaItem(
                    media = media,
                    contentDescription = "${listing.title} media",
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = listing.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!isOwner) {
                    IconButton(onClick = onSave) {
                        Icon(
                            if (listing.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (listing.isSaved) "Remove saved listing" else "Save listing",
                        )
                    }
                }
            }
            Text(rupees(listing.priceAmount), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "${listing.category} · ${listing.condition}",
                style = MaterialTheme.typography.labelMedium,
            )
            Text(listing.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "@${listing.seller.username} · ${listing.campusSpot.ifBlank { listing.location }}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "${listing.savedCount} saved · ${listing.inquiryCount} inquiries",
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun RequestCard(request: MarketRequest, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            request.media.firstOrNull { it.url.isNotBlank() }?.let { media ->
                MarketMediaItem(
                    media = media,
                    contentDescription = "${request.title} media",
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(14.dp)),
                )
            }
            Text(request.tag, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(request.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(request.detail, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                request.budgetLabel.ifBlank { request.budgetAmount?.let(::rupees) ?: "Budget open" },
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "@${request.requester.username} · ${request.campusSpot} · ${request.responseCount} responses",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MarketDetailDialog(
    detail: MarketDetail,
    viewerId: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onContact: (MarketDetail, String) -> Unit,
    onSold: (String) -> Unit,
) {
    var message by remember(detail.id) { mutableStateOf("") }
    val listing = (detail as? MarketDetail.Listing)?.value
    val request = (detail as? MarketDetail.Request)?.value
    val ownerId = listing?.seller?.userId ?: request?.requester?.userId
    val isOwner = viewerId == ownerId

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(listing?.title ?: request?.title.orEmpty()) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MarketMediaGallery(
                    media = listing?.media ?: request?.media.orEmpty(),
                    title = listing?.title ?: request?.title.orEmpty(),
                )
                Text(
                    listing?.let { rupees(it.priceAmount) }
                        ?: request?.budgetLabel.orEmpty().ifBlank { "Budget open" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(listing?.description ?: request?.detail.orEmpty())
                Text("Category: ${listing?.category ?: request?.category}")
                Text("Campus spot: ${listing?.campusSpot ?: request?.campusSpot}")
                HorizontalDivider()
                if (!isOwner) {
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Message to owner") },
                        minLines = 2,
                        maxLines = 4,
                    )
                } else {
                    Text("This is your post.", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        confirmButton = {
            when {
                isOwner && listing != null -> Button(
                    onClick = { onSold(listing.id) },
                    enabled = !busy,
                ) { Text("Mark sold") }

                !isOwner -> Button(
                    onClick = { onContact(detail, message) },
                    enabled = !busy && message.isNotBlank(),
                ) { Text("Contact") }
            }
        },
        dismissButton = {
            Row {
                if (!isOwner && listing != null) {
                    TextButton(onClick = { onSave(listing.id) }, enabled = !busy) {
                        Text(if (listing.isSaved) "Unsave" else "Save")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
    )
}

@Composable
private fun MarketMediaGallery(media: List<MarketMedia>, title: String) {
    val visibleMedia = media.filter { it.url.isNotBlank() }
    if (visibleMedia.isEmpty()) return

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(visibleMedia, key = { it.id.ifBlank { it.url } }) { item ->
            MarketMediaItem(
                media = item,
                contentDescription = "$title media",
                modifier = Modifier.fillParentMaxWidth().aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp)),
            )
        }
    }
}

@Composable
private fun MarketMediaItem(
    media: MarketMedia,
    contentDescription: String,
    modifier: Modifier,
) {
    val isVideo = media.kind.equals("video", ignoreCase = true) ||
        media.mimeType.startsWith("video/", ignoreCase = true)
    if (isVideo) {
        VybRemoteVideo(
            url = media.url,
            contentDescription = contentDescription,
            modifier = modifier,
            autoPlay = false,
        )
    } else {
        VybRemoteImage(
            url = media.url,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MarketComposerDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (MarketPostDraft) -> Unit,
) {
    var tab by remember { mutableStateOf("sale") }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var campusSpot by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New market post") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { MarketTabs(selected = tab, onSelected = { tab = it }) }
                item {
                    OutlinedTextField(
                        title,
                        { title = it.take(120) },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        category,
                        { category = it.take(60) },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        description,
                        { description = it.take(2_000) },
                        label = { Text("Description") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        amount,
                        { amount = it.filter(Char::isDigit).take(9) },
                        label = { Text(if (tab == "sale") "Price (₹)" else "Budget (₹, optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                item {
                    OutlinedTextField(
                        campusSpot,
                        { campusSpot = it.take(120) },
                        label = { Text("Campus spot") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (tab == "sale") {
                    item {
                        OutlinedTextField(
                            condition,
                            { condition = it.take(80) },
                            label = { Text("Condition") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        MarketPostDraft(
                            tab = tab,
                            title = title,
                            category = category,
                            description = description,
                            amount = amount.toLongOrNull(),
                            campusSpot = campusSpot,
                            condition = condition,
                        ),
                    )
                },
                enabled = !busy &&
                    title.isNotBlank() &&
                    category.isNotBlank() &&
                    description.isNotBlank() &&
                    (tab != "sale" || (amount.toLongOrNull() ?: 0L) > 0L),
            ) { Text("Publish") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun MarketEmpty(title: String, body: String, action: (() -> Unit)? = null) {
    Box(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        VybEmptyState(
            icon = Icons.Default.Storefront,
            title = title,
            body = body,
            actionLabel = if (action != null) "Try again" else null,
            onAction = action
        )
    }
}

private fun rupees(value: Long): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
