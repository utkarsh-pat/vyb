@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package social.vyb.app.ui

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.mandatorySystemGestures
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import social.vyb.app.features.messages.MessagesFeatureScreen
import social.vyb.app.features.market.MarketFeatureScreen
import social.vyb.app.features.hub.CampusHubScreen
import social.vyb.app.features.funhub.FunHubScreen
import social.vyb.app.features.media.MediaComposerScreen
import social.vyb.app.features.media.MediaPublishIntent
import social.vyb.app.features.realtime.NotificationDeviceRepository
import social.vyb.app.features.search.SearchScreen
import social.vyb.app.features.profile.ProfileFeatureScreen
import social.vyb.app.features.notifications.NotificationScreen
import social.vyb.app.features.social.SocialActionsViewModel
import social.vyb.app.features.stories.NativeVibesScreen
import social.vyb.app.features.update.AppUpdatePrompt

private data class Destination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val destinations = listOf(
    Destination("home", "Home", Icons.Outlined.Home),
    Destination("hub", "Hub", Icons.Outlined.Explore),
    Destination("vibes", "Vibes", Icons.Outlined.AutoAwesome),
    Destination("market", "Market", Icons.Outlined.Storefront),
    Destination("profile", "Profile", Icons.Outlined.PersonOutline)
)

@Composable
fun VybApp(
    viewModel: VybViewModel = viewModel(),
    notificationHref: String? = null,
    onNotificationHrefConsumed: () -> Unit = {}
) {
    var startupHoldComplete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // A short anti-flash window keeps transitions stable without delaying a ready app.
        delay(250)
        startupHoldComplete = true
    }

    val showBrandedLoader =
        !startupHoldComplete ||
            viewModel.state.isLoading

    Crossfade(
        targetState = showBrandedLoader,
        animationSpec = tween(durationMillis = 280),
        label = "Vyb startup transition"
    ) { loading ->
        if (loading) VybLoadingScreen()
        else VybAppContent(
            viewModel = viewModel,
            notificationHref = notificationHref,
            onNotificationHrefConsumed = onNotificationHrefConsumed
        )
    }
}

@Composable
private fun VybAppContent(
    viewModel: VybViewModel,
    notificationHref: String?,
    onNotificationHrefConsumed: () -> Unit
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(viewModel.state.isAuthenticated) {
        if (!viewModel.state.isAuthenticated) return@LaunchedEffect

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        runCatching {
            NotificationDeviceRepository(context).registerCurrentInstallation()
        }
    }

    if (!viewModel.state.isAuthenticated) {
        LoginScreen(
            state = viewModel.state,
            onEmailSignIn = viewModel::signInWithEmail,
            onCreateAccount = viewModel::createAccount,
            onPasswordReset = viewModel::sendPasswordReset,
            onGoogleSignIn = viewModel::signInWithGoogle,
            onClearError = viewModel::clearError
        )
        return
    }

    if (viewModel.state.profileCompleted == null) {
        VybPageBackground(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                VybEmptyState(
                    icon = Icons.Default.CloudOff,
                    title = "Could not load your campus",
                    body = userFacingCampusLoadError(viewModel.state.profileError),
                    actionLabel = "Try again",
                    onAction = viewModel::retryAppSession,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        return
    }

    if (viewModel.state.profileCompleted == false) {
        OnboardingScreen(
            displayName = viewModel.state.displayName,
            email = viewModel.state.email,
            collegeName = viewModel.state.college,
            saving = viewModel.state.profileSaving,
            error = viewModel.state.profileError,
            catalog = viewModel.state.profileCatalog,
            usernameAvailable = viewModel.state.usernameAvailability,
            usernameChecking = viewModel.state.usernameChecking,
            onLoadCatalog = viewModel::loadOnboardingCatalog,
            onUsernameChanged = viewModel::checkUsername,
            onSubmit = viewModel::completeProfile
        )
        return
    }

    val navController = rememberNavController()
    var pendingPostId by remember { mutableStateOf<String?>(null) }
    var pendingVibeId by remember { mutableStateOf<String?>(null) }
    var pendingConversationId by remember { mutableStateOf<String?>(null) }
    var pendingMarketId by remember { mutableStateOf<String?>(null) }
    var pendingMarketType by remember { mutableStateOf<String?>(null) }
    var pendingEventId by remember { mutableStateOf<String?>(null) }
    val socialActionsViewModel: SocialActionsViewModel = viewModel()
    val haptic = LocalHapticFeedback.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var reselectedRoute by remember { mutableStateOf<String?>(null) }
    var reselectVersion by remember { mutableIntStateOf(0) }
    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    val selectPrimaryDestination: (String) -> Unit = { route ->
        if (currentRoute == route) {
            reselectedRoute = route
            reselectVersion += 1
            if (route == "home") viewModel.refreshHomeFeed()
        } else {
            navigateTo(route)
        }
    }
    LaunchedEffect(notificationHref) {
        val href = notificationHref ?: return@LaunchedEffect
        val target = notificationTarget(href)
        pendingPostId = target.postId
        pendingVibeId = target.vibeId
        pendingConversationId = target.conversationId
        pendingMarketId = target.marketId
        pendingMarketType = target.marketType
        pendingEventId = target.eventId
        navigateTo(target.route)
        onNotificationHrefConsumed()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Bottom navigation mirrors the compact PWA. Tablets keep the same
        // destinations in a rail so content is not squeezed into a phone-width
        // strip or covered by an oversized bottom bar.
        val useNavigationRail = maxWidth >= 600.dp
        val iconOnly = maxHeight < 620.dp || maxWidth < 330.dp
        // Some OEMs briefly report the visible navigation-bar inset as zero
        // when switching between gesture and three-button navigation at
        // runtime. The ignoring-visibility, tappable-element and mandatory
        // gesture union keeps the app bar above every system navigation mode.
        val bottomSystemInsets = WindowInsets.navigationBarsIgnoringVisibility
            .union(WindowInsets.tappableElement)
            .union(WindowInsets.mandatorySystemGestures)
            .only(WindowInsetsSides.Bottom)

        Scaffold(
            containerColor = VybBackground,
            // The bottom bar owns the bottom system inset. Keeping it out of
            // Scaffold's content insets prevents parent consumption from
            // starving the bottom bar after an OEM navigation-mode change.
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Top + WindowInsetsSides.Horizontal
            ),
            bottomBar = {
                if (!useNavigationRail) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(VybPanel.copy(alpha = .85f))
                            .windowInsetsPadding(bottomSystemInsets)
                    ) {
                        HorizontalDivider(color = VybBorder)
                        NavigationBar(
                            modifier = Modifier.height(if (iconOnly) 52.dp else 56.dp),
                            containerColor = VybPanel.copy(alpha = .85f),
                            tonalElevation = 0.dp,
                            // The compact bar owns a fixed content height. The
                            // surrounding Column reserves the changing gesture/
                            // three-button system navigation inset separately.
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectPrimaryDestination(destination.route)
                                    },
                                    icon = {
                                        Icon(
                                            destination.icon,
                                            destination.label,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    },
                                    label = if (iconOnly) null else ({
                                        Text(destination.label, style = MaterialTheme.typography.labelSmall)
                                    }),
                                    alwaysShowLabel = !iconOnly,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VybIndigo,
                                        selectedTextColor = VybText,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = VybMuted,
                                        unselectedTextColor = VybMuted
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (useNavigationRail) {
                    NavigationRail(containerColor = VybPanel.copy(alpha = .90f)) {
                        destinations.forEach { destination ->
                            NavigationRailItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectPrimaryDestination(destination.route)
                                },
                                icon = { Icon(destination.icon, destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
                VybPageBackground(Modifier.weight(1f).fillMaxSize()) {
                    NavHost(navController, startDestination = "home") {
                        composable("home") {
                            key(if (reselectedRoute == "home") reselectVersion else 0) { HomeScreen(
                                state = viewModel.state,
                                initialPostId = pendingPostId,
                                onInitialPostConsumed = { pendingPostId = null },
                                onRefresh = viewModel::refreshHomeFeed,
                                onOpenSearch = { navController.navigate("search") },
                                onOpenMessages = { navController.navigate("messages") },
                                onOpenNotifications = { navController.navigate("notifications") },
                                onCreateStory = { navController.navigate("create-story") },
                                onOpenProfile = { username -> navController.navigate("search-profile/${encodeRouteSegment(username)}") },
                                socialViewModel = socialActionsViewModel
                            ) }
                        }
                        composable("vibes") {
                            key(if (reselectedRoute == "vibes") reselectVersion else 0) { NativeVibesScreen(
                                initialVibeId = pendingVibeId,
                                onInitialVibeConsumed = { pendingVibeId = null },
                                viewerUserId = viewModel.state.userId,
                                socialViewModel = socialActionsViewModel,
                                onCreateVibe = { navController.navigate("create-vibe") },
                                onCreateStory = { navController.navigate("create-story") },
                                onSearch = { navController.navigate("search") },
                                onOpenProfile = { username ->
                                    if (username == viewModel.state.email.substringBefore("@")) {
                                        navigateTo("profile")
                                    } else {
                                        navController.navigate("search-profile/${encodeRouteSegment(username)}")
                                    }
                                },
                                refreshSignal = if (reselectedRoute == "vibes") reselectVersion else 0
                            ) }
                        }
                        composable("create-vibe") {
                            MediaComposerScreen(
                                initialIntent = MediaPublishIntent.Vibe,
                                showIntentPicker = false,
                                displayName = viewModel.state.displayName,
                                username = viewModel.state.email.substringBefore("@"),
                                onCancelCreation = navController::navigateUp,
                                onDraftSaved = {
                                    android.widget.Toast.makeText(
                                        context,
                                        "saved as draft",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onPublished = {
                                    viewModel.refreshHomeFeed()
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable("create-post") {
                            MediaComposerScreen(
                                initialIntent = MediaPublishIntent.Post,
                                showIntentPicker = false,
                                displayName = viewModel.state.displayName,
                                username = viewModel.state.email.substringBefore("@"),
                                onCancelCreation = navController::navigateUp,
                                onDraftSaved = {
                                    android.widget.Toast.makeText(
                                        context,
                                        "saved as draft",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onPublished = {
                                    viewModel.refreshHomeFeed()
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable("create-story") {
                            MediaComposerScreen(
                                initialIntent = MediaPublishIntent.Story,
                                showIntentPicker = false,
                                displayName = viewModel.state.displayName,
                                username = viewModel.state.email.substringBefore("@"),
                                onCancelCreation = navController::navigateUp,
                                onDraftSaved = {
                                    android.widget.Toast.makeText(
                                        context,
                                        "saved as draft",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onPublished = {
                                    viewModel.refreshHomeFeed()
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable("messages") {
                            MessagesFeatureScreen(
                                initialConversationId = pendingConversationId,
                                onInitialConversationConsumed = { pendingConversationId = null },
                                onOpenCommunity = { slug ->
                                    navController.navigate(
                                        "messages/community/${Uri.encode(slug)}"
                                    )
                                }
                            )
                        }
                        composable("messages/community/{slug}") { entry ->
                            MessagesFeatureScreen(
                                communitySlug = entry.arguments
                                    ?.getString("slug")
                                    ?.let(Uri::decode),
                                onCloseCommunity = navController::navigateUp
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                onBack = navController::navigateUp,
                                onOpenPost = { postId -> pendingPostId = postId; navigateTo("home") },
                                onOpenVibe = { vibeId -> pendingVibeId = vibeId; navigateTo("vibes") },
                                onOpenMarket = { item ->
                                    pendingMarketId = item.id
                                    pendingMarketType = if (item.kind.name == "Request") "request" else "listing"
                                    navigateTo("market")
                                }
                            )
                        }
                        composable("notifications") {
                            NotificationScreen(
                                onBack = navController::navigateUp,
                                onNavigateHref = { href ->
                                    val target = notificationTarget(href)
                                    pendingPostId = target.postId
                                    pendingVibeId = target.vibeId
                                    pendingConversationId = target.conversationId
                                    pendingMarketId = target.marketId
                                    pendingMarketType = target.marketType
                                    pendingEventId = target.eventId
                                    navigateTo(target.route)
                                }
                            )
                        }
                        composable("search-profile/{username}") { entry ->
                            SearchScreen(
                                onBack = navController::navigateUp,
                                initialUsername = entry.arguments?.getString("username"),
                                onOpenPost = { postId -> pendingPostId = postId; navigateTo("home") },
                                onOpenVibe = { vibeId -> pendingVibeId = vibeId; navigateTo("vibes") },
                                onOpenMarket = { item ->
                                    pendingMarketId = item.id
                                    pendingMarketType = if (item.kind.name == "Request") "request" else "listing"
                                    navigateTo("market")
                                }
                            )
                        }
                        composable("market") {
                            key(if (reselectedRoute == "market") reselectVersion else 0) { MarketFeatureScreen(
                                initialTargetId = pendingMarketId,
                                initialTargetType = pendingMarketType,
                                onInitialTargetConsumed = {
                                    pendingMarketId = null
                                    pendingMarketType = null
                                },
                                refreshSignal = if (reselectedRoute == "market") reselectVersion else 0,
                            ) }
                        }
                        composable("hub") {
                            key(if (reselectedRoute == "hub") reselectVersion else 0) { UnifiedHubScreen(
                                initialEventId = pendingEventId,
                                onInitialEventConsumed = { pendingEventId = null },
                                refreshSignal = if (reselectedRoute == "hub") reselectVersion else 0,
                            ) }
                        }
                        composable("profile") {
                            key(if (reselectedRoute == "profile") reselectVersion else 0) { ProfileFeatureScreen(
                                email = viewModel.state.email,
                                onSignOut = { viewModel.signOut(context) },
                                onCreatePost = { navController.navigate("create-post") },
                                onOpenPost = { postId ->
                                    pendingPostId = postId
                                    navigateTo("home")
                                },
                                onOpenVibe = { vibeId ->
                                    pendingVibeId = vibeId
                                    navigateTo("vibes")
                                },
                                refreshSignal = if (reselectedRoute == "profile") reselectVersion else 0
                            ) }
                        }
                    }
                }
            }
        }

        // Removed overlay bottom bar since it's now fixed in Scaffold

        AppUpdatePrompt(enabled = true)
    }
}

internal fun userFacingCampusLoadError(error: String?): String {
    val normalized = error?.lowercase().orEmpty()
    return when {
        "permission" in normalized || "unauthorized" in normalized ->
            "Your campus access could not be verified. Sign in again and retry."
        else -> "We couldn't reach Vyb. Check your connection and try again."
    }
}

internal fun notificationDestination(href: String): String {
    return notificationTarget(href).route
}

private data class NotificationTarget(
    val route: String,
    val postId: String? = null,
    val vibeId: String? = null,
    val conversationId: String? = null,
    val marketId: String? = null,
    val marketType: String? = null,
    val eventId: String? = null,
)

private fun notificationTarget(href: String): NotificationTarget {
    val uri = runCatching { java.net.URI(href.trim()) }.getOrNull() ?: return NotificationTarget("home")
    val host = uri.host?.lowercase()
    if (host != null && host != "vybnet.app" && !host.endsWith(".vybnet.app")) {
        return NotificationTarget("home")
    }
    val path = uri.path?.takeIf(String::isNotBlank) ?: href.substringBefore("?")
    val query = uri.rawQuery.orEmpty().split("&")
        .mapNotNull { part ->
            val pieces = part.split("=", limit = 2)
            val key = pieces.firstOrNull()?.let(::decodeQueryValue)?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            key to decodeQueryValue(pieces.getOrElse(1) { "" })
        }
        .toMap()
    val postId = query["postId"] ?: query["post"]
    if (!postId.isNullOrBlank()) {
        return if (path.startsWith("/vibes") || path.startsWith("/reels")) {
            NotificationTarget(route = "vibes", vibeId = postId)
        } else {
            NotificationTarget(route = "home", postId = postId)
        }
    }
    return when {
        path.startsWith("/u/") -> {
            val username = path.removePrefix("/u/").substringBefore("/").trim()
            if (username.isBlank()) {
                NotificationTarget("search")
            } else {
                NotificationTarget("search-profile/${encodeRouteSegment(username)}")
            }
        }
        path.startsWith("/messages/community/") -> {
            val slug = path
                .removePrefix("/messages/community/")
                .substringBefore("/")
                .trim()
            if (slug.isBlank()) NotificationTarget("messages") else {
                NotificationTarget("messages/community/${encodeRouteSegment(slug)}")
            }
        }
        path.startsWith("/messages/") -> NotificationTarget(
            route = "messages",
            conversationId = path.removePrefix("/messages/").substringBefore("/").takeIf(String::isNotBlank),
        )
        path.startsWith("/messages") -> NotificationTarget("messages")
        path.startsWith("/market") -> NotificationTarget(
            route = "market",
            marketId = query["id"] ?: query["listing"] ?: query["request"],
            marketType = query["type"] ?: when {
                query.containsKey("request") -> "request"
                query.containsKey("listing") -> "listing"
                else -> null
            },
        )
        path.startsWith("/vibes") || path.startsWith("/reels") -> NotificationTarget("vibes")
        path.startsWith("/search") -> NotificationTarget("search")
        path.startsWith("/dashboard") || path.startsWith("/profile") || path.startsWith("/settings") -> NotificationTarget("profile")
        path.startsWith("/hub") || path.startsWith("/events") -> NotificationTarget("hub", eventId = query["eventId"])
        else -> NotificationTarget("home")
    }
}

private fun decodeQueryValue(value: String): String =
    runCatching {
        java.net.URLDecoder.decode(
            value,
            java.nio.charset.StandardCharsets.UTF_8.name()
        )
    }
        .getOrDefault("")

private fun encodeRouteSegment(value: String): String =
    java.net.URLEncoder.encode(
        value,
        java.nio.charset.StandardCharsets.UTF_8.toString()
    ).replace("+", "%20")

@Composable
private fun UnifiedHubScreen(
    initialEventId: String? = null,
    onInitialEventConsumed: (() -> Unit)? = null,
    refreshSignal: Int = 0,
) {
    var selectedTab by remember { mutableIntStateOf(1) }
    Column(
        Modifier.pointerInput(selectedTab) {
            var drag = 0f
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f },
                onDragCancel = { drag = 0f },
                onDragEnd = {
                    if (drag < -90f && selectedTab == 0) selectedTab = 1
                    if (drag > 90f && selectedTab == 1) selectedTab = 0
                    drag = 0f
                },
                onHorizontalDrag = { change, amount ->
                    drag += amount
                    if (kotlin.math.abs(drag) > 12f) change.consume()
                }
            )
        }
    ) {
        VybConnectedTabSelector(
            tabs = listOf(
                VybConnectedTab("Games Hub"),
                VybConnectedTab("Events Hub")
            ),
            selectedIndex = selectedTab,
            onSelected = { selectedTab = it }
        )
        if (selectedTab == 0) FunHubScreen(refreshSignal = refreshSignal) else CampusHubScreen(
            initialEventId = initialEventId,
            onInitialEventConsumed = onInitialEventConsumed,
            refreshSignal = refreshSignal,
        )
    }
}
