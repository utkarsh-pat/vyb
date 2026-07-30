package social.vyb.app.ui

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
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
    Destination("home", "Home", Icons.Default.Home),
    Destination("hub", "Hub", Icons.Default.Explore),
    Destination("vibes", "Vibes", Icons.Default.AutoAwesome),
    Destination("market", "Market", Icons.Default.Storefront),
    Destination("profile", "Profile", Icons.Default.Person)
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
    val socialActionsViewModel: SocialActionsViewModel = viewModel()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    LaunchedEffect(notificationHref) {
        val href = notificationHref ?: return@LaunchedEffect
        navigateTo(notificationDestination(href))
        onNotificationHrefConsumed()
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Bottom navigation mirrors the compact PWA. Tablets keep the same
        // destinations in a rail so content is not squeezed into a phone-width
        // strip or covered by an oversized bottom bar.
        val useNavigationRail = maxWidth >= 600.dp
        Scaffold(
            containerColor = VybBackground,
            bottomBar = {
                if (!useNavigationRail) {
                BoxWithConstraints {
                    val iconOnly = maxHeight < 620.dp || maxWidth < 330.dp
                    Column {
                        HorizontalDivider(color = VybBorder)
                        NavigationBar(
                            modifier = Modifier.height(if (iconOnly) 58.dp else 66.dp),
                            containerColor = VybPanel.copy(alpha = .90f),
                            tonalElevation = 0.dp
                        ) {
                            destinations.forEach { destination ->
                                NavigationBarItem(
                                    selected = currentRoute == destination.route,
                                    onClick = { navigateTo(destination.route) },
                                    icon = { Icon(destination.icon, destination.label) },
                                    label = if (iconOnly) null else ({
                                        Text(destination.label, style = MaterialTheme.typography.labelSmall)
                                    }),
                                    alwaysShowLabel = !iconOnly,
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VybText,
                                        selectedTextColor = VybText,
                                        indicatorColor = VybIndigo.copy(alpha = .24f),
                                        unselectedIconColor = VybMuted,
                                        unselectedTextColor = VybMuted
                                    )
                                )
                            }
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
                                onClick = { navigateTo(destination.route) },
                                icon = { Icon(destination.icon, destination.label) },
                                label = { Text(destination.label) }
                            )
                        }
                    }
                }
                VybPageBackground(Modifier.weight(1f).fillMaxSize()) {
                    NavHost(navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                state = viewModel.state,
                                onRefresh = viewModel::refreshHomeFeed,
                                onOpenSearch = { navController.navigate("search") },
                                onOpenMessages = { navController.navigate("messages") },
                                onOpenNotifications = { navController.navigate("notifications") },
                                onCreateStory = { navController.navigate("create-story") },
                                socialViewModel = socialActionsViewModel
                            )
                        }
                        composable("vibes") {
                            NativeVibesScreen(
                                viewerUserId = viewModel.state.userId,
                                socialViewModel = socialActionsViewModel,
                                onCreateVibe = { navController.navigate("create-vibe") },
                                onSearch = { navController.navigate("search") },
                                onOpenProfile = { username ->
                                    navController.navigate(
                                        "search-profile/${encodeRouteSegment(username)}"
                                    )
                                }
                            )
                        }
                        composable("create-vibe") {
                            MediaComposerScreen(
                                initialIntent = MediaPublishIntent.Vibe,
                                showIntentPicker = false,
                                displayName = viewModel.state.displayName,
                                username = viewModel.state.email.substringBefore("@"),
                                onCancelCreation = navController::navigateUp,
                                onPublished = { navController.navigateUp() }
                            )
                        }
                        composable("create-story") {
                            MediaComposerScreen(
                                initialIntent = MediaPublishIntent.Story,
                                showIntentPicker = false,
                                displayName = viewModel.state.displayName,
                                username = viewModel.state.email.substringBefore("@"),
                                onCancelCreation = navController::navigateUp,
                                onPublished = { navController.navigateUp() }
                            )
                        }
                        composable("messages") {
                            MessagesFeatureScreen(
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
                                onOpenPost = { navigateTo("home") },
                                onOpenVibe = { navigateTo("vibes") },
                                onOpenMarket = { navigateTo("market") }
                            )
                        }
                        composable("notifications") {
                            NotificationScreen(
                                onBack = navController::navigateUp,
                                onNavigateHref = { href ->
                                    navigateTo(notificationDestination(href))
                                }
                            )
                        }
                        composable("search-profile/{username}") { entry ->
                            SearchScreen(
                                onBack = navController::navigateUp,
                                initialUsername = entry.arguments?.getString("username"),
                                onOpenPost = { navigateTo("home") },
                                onOpenVibe = { navigateTo("vibes") },
                                onOpenMarket = { navigateTo("market") }
                            )
                        }
                        composable("market") { MarketFeatureScreen() }
                        composable("hub") { UnifiedHubScreen() }
                        composable("profile") {
                            ProfileFeatureScreen(
                                email = viewModel.state.email,
                                onSignOut = { viewModel.signOut(context) },
                                onCreatePost = { navigateTo("home") }
                            )
                        }
                    }
                }
            }
        }
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
    val uri = runCatching { java.net.URI(href.trim()) }.getOrNull() ?: return "home"
    val host = uri.host?.lowercase()
    if (host != null && host != "vybnet.app" && !host.endsWith(".vybnet.app")) {
        return "home"
    }
    val path = uri.path?.takeIf(String::isNotBlank) ?: href.substringBefore("?")
    return when {
        path.startsWith("/u/") -> {
            val username = path.removePrefix("/u/").substringBefore("/").trim()
            if (username.isBlank()) {
                "search"
            } else {
                "search-profile/${encodeRouteSegment(username)}"
            }
        }
        path.startsWith("/messages/community/") -> {
            val slug = path
                .removePrefix("/messages/community/")
                .substringBefore("/")
                .trim()
            if (slug.isBlank()) "messages" else {
                "messages/community/${encodeRouteSegment(slug)}"
            }
        }
        path.startsWith("/messages") -> "messages"
        path.startsWith("/market") -> "market"
        path.startsWith("/vibes") || path.startsWith("/reels") -> "vibes"
        path.startsWith("/search") -> "search"
        path.startsWith("/dashboard") || path.startsWith("/profile") || path.startsWith("/settings") -> "profile"
        path.startsWith("/hub") || path.startsWith("/events") -> "hub"
        else -> "home"
    }
}

private fun encodeRouteSegment(value: String): String =
    java.net.URLEncoder.encode(
        value,
        java.nio.charset.StandardCharsets.UTF_8.toString()
    ).replace("+", "%20")

@Composable
private fun UnifiedHubScreen() {
    var selectedTab by remember { mutableIntStateOf(1) }
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            color = VybPanel.copy(alpha = .92f),
            shape = RoundedCornerShape(18.dp)
        ) {
            BoxWithConstraints {
                val compactTabs = maxWidth < 360.dp
                Row(Modifier.padding(4.dp)) {
                    listOf("Games Hub", "Events Hub").forEachIndexed { index, label ->
                        Surface(
                            onClick = { selectedTab = index },
                            modifier = Modifier.weight(1f),
                            color = if (selectedTab == index) VybIndigo.copy(alpha = .28f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Box(
                                Modifier.padding(
                                    horizontal = if (compactTabs) 6.dp else 10.dp,
                                    vertical = 11.dp
                                ),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text(
                                    text = if (compactTabs) {
                                        if (index == 0) "Games" else "Events"
                                    } else {
                                        label
                                    },
                                    color = if (selectedTab == index) VybText else VybMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        if (selectedTab == 0) FunHubScreen() else CampusHubScreen()
    }
}
