package social.vyb.app.features.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import social.vyb.app.BuildConfig

private sealed interface AppUpdateState {
    data object Idle : AppUpdateState
    data object Checking : AppUpdateState
    data class Available(val manifest: AndroidUpdateManifest) : AppUpdateState
    data class PermissionNeeded(val manifest: AndroidUpdateManifest) : AppUpdateState
    data class Downloading(val manifest: AndroidUpdateManifest) : AppUpdateState
    data class Downloaded(val manifest: AndroidUpdateManifest) : AppUpdateState
    data class Failed(val manifest: AndroidUpdateManifest, val message: String) : AppUpdateState
}

@Composable
fun AppUpdatePrompt(
    enabled: Boolean,
    repository: AppUpdateRepository = remember { AppUpdateRepository() }
) {
    val context = LocalContext.current
    val installer = remember(context) { AppUpdateInstaller(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<AppUpdateState>(AppUpdateState.Idle) }
    var dismissedVersionCode by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(enabled) {
        if (!enabled || state !is AppUpdateState.Idle) return@LaunchedEffect
        state = AppUpdateState.Checking
        state = runCatching { repository.check() }
            .map { manifest ->
                if (manifest.updateAvailable && manifest.latestVersionCode != dismissedVersionCode) {
                    AppUpdateState.Available(manifest)
                } else {
                    AppUpdateState.Idle
                }
            }
            .getOrElse { AppUpdateState.Idle }
    }

    val currentState = state
    val manifest = when (currentState) {
        is AppUpdateState.Available -> currentState.manifest
        is AppUpdateState.PermissionNeeded -> currentState.manifest
        is AppUpdateState.Downloading -> currentState.manifest
        is AppUpdateState.Downloaded -> currentState.manifest
        is AppUpdateState.Failed -> currentState.manifest
        AppUpdateState.Checking,
        AppUpdateState.Idle -> null
    } ?: return

    AlertDialog(
        onDismissRequest = {
            if (!manifest.forceUpdate) {
                dismissedVersionCode = manifest.latestVersionCode
                state = AppUpdateState.Idle
            }
        },
        icon = {
            if (currentState is AppUpdateState.Downloading) {
                CircularProgressIndicator()
            } else {
                Icon(Icons.Default.SystemUpdate, contentDescription = null)
            }
        },
        title = { Text("Vybnet update available") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Version ${manifest.latestVersionName} is ready. You are on ${BuildConfig.VERSION_NAME}.",
                    fontWeight = FontWeight.SemiBold
                )
                manifest.releaseNotes.take(4).forEach { note ->
                    Text(text = "- $note")
                }
                when (currentState) {
                    is AppUpdateState.PermissionNeeded -> Text("Install permission is needed once for APK updates.")
                    is AppUpdateState.Downloading -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Downloading APK...")
                    }
                    is AppUpdateState.Downloaded -> Text("Download complete. Opening installer...")
                    is AppUpdateState.Failed -> Text(currentState.message)
                    else -> Unit
                }
            }
        },
        confirmButton = {
            Button(
                enabled = currentState !is AppUpdateState.Downloading,
                onClick = {
                    if (!installer.canInstallPackages()) {
                        state = AppUpdateState.PermissionNeeded(manifest)
                        installer.openInstallPermissionSettings()
                        return@Button
                    }
                    scope.launch {
                        installer.downloadAndInstall(
                            manifest = manifest,
                            onStarted = { state = AppUpdateState.Downloading(manifest) },
                            onDownloaded = { state = AppUpdateState.Downloaded(manifest) },
                            onFailed = { state = AppUpdateState.Failed(manifest, it) }
                        )
                    }
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text(if (currentState is AppUpdateState.PermissionNeeded) "Retry update" else "Update now")
                }
            }
        },
        dismissButton = {
            if (!manifest.forceUpdate) {
                TextButton(
                    enabled = currentState !is AppUpdateState.Downloading,
                    onClick = {
                        dismissedVersionCode = manifest.latestVersionCode
                        state = AppUpdateState.Idle
                    }
                ) {
                    Text("Later")
                }
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}
