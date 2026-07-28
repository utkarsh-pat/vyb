package social.vyb.app.ui

import android.Manifest
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
import androidx.compose.material3.Icon
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
import social.vyb.app.features.realtime.NotificationDeviceRepository
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
fun VybApp(viewModel: VybViewModel = viewModel()) {
    var startupHoldComplete by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // Keep the branded loader visible long enough to avoid a one-frame auth flash.
        delay(2_400)
        startupHoldComplete = true
    }

    val showBrandedLoader =
        !startupHoldComplete ||
            (!viewModel.state.isAuthenticated && viewModel.state.isLoading)

    Crossfade(
        targetState = showBrandedLoader,
        animationSpec = tween(durationMillis = 280),
        label = "Vyb startup transition"
    ) { loading ->
        if (loading) VybLoadingScreen()
        else VybAppContent(viewModel)
    }
}

@Composable
private fun VybAppContent(viewModel: VybViewModel) {
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
            NotificationDeviceRepository(context).registerCurrentToken()
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

    val navController = rememberNavController()
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 700.dp && maxHeight < 600.dp
        Scaffold(
            containerColor = VybBackground,
            bottomBar = {
                if (!useNavigationRail) {
                BoxWithConstraints {
                    val compact = maxHeight < 680.dp || maxWidth < 350.dp
                NavigationBar(
                    modifier = Modifier.height(if (compact) 64.dp else 76.dp),
                    containerColor = VybPanel.copy(alpha = .97f),
                    tonalElevation = 0.dp
                ) {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navigateTo(destination.route) },
                            icon = { Icon(destination.icon, destination.label) },
                            label = if (compact) null else ({ Text(destination.label) }),
                            alwaysShowLabel = !compact,
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
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (useNavigationRail) {
                    NavigationRail(containerColor = VybPanel.copy(alpha = .97f)) {
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
                                repository = viewModel.repository,
                                onRefresh = viewModel::refreshHomeFeed,
                                onOpenMessages = { navController.navigate("messages") }
                            )
                        }
                        composable("vibes") { NativeVibesScreen() }
                        composable("messages") { MessagesFeatureScreen() }
                        composable("market") { MarketFeatureScreen() }
                        composable("hub") { UnifiedHubScreen() }
                        composable("profile") {
                            ProfileScreen(viewModel.state, onSignOut = { viewModel.signOut(context) })
                        }
                    }
                }
            }
        }
        AppUpdatePrompt(enabled = true)
    }
}

@Composable
private fun UnifiedHubScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }
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
                    listOf("Campus", "Games & Alerts").forEachIndexed { index, label ->
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
                                    text = if (compactTabs && index == 1) "Games" else label,
                                    color = if (selectedTab == index) VybText else VybMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        if (selectedTab == 0) CampusHubScreen() else FunHubScreen()
    }
}
