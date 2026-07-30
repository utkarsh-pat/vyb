package social.vyb.app.features.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import java.io.File
import java.net.URI
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppUpdateInstaller(
    private val context: Context
) {
    private val appContext = context.applicationContext
    private val verifierScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun canInstallPackages(): Boolean =
        appContext.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        appContext.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                "package:${appContext.packageName}".toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun downloadAndInstall(
        manifest: AndroidUpdateManifest,
        onStarted: () -> Unit,
        onDownloaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val verifiedDownload = runCatching {
            validateUpdateDownload(manifest.apkUrl, manifest.apkSha256)
        }.getOrElse {
            onFailed(it.message ?: "Update metadata is invalid.")
            return
        }
        val downloadManager = appContext.getSystemService<DownloadManager>()
        if (downloadManager == null) {
            onFailed("Download service unavailable.")
            return
        }

        val fileName = "Vyb-${safeVersionLabel(manifest.latestVersionName)}.apk"
        val downloadDirectory = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(appContext.filesDir, "downloads").apply { mkdirs() }
        val apkFile = File(downloadDirectory, fileName)
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(verifiedDownload.uri.toString().toUri())
            .setTitle("Vyb ${manifest.latestVersionName}")
            .setDescription("Downloading update")
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(apkFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)
        onStarted()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return

                runCatching { appContext.unregisterReceiver(this) }
                val pendingResult = goAsync()
                verifierScope.launch {
                    val failure = runCatching {
                        val query = DownloadManager.Query().setFilterById(downloadId)
                        val completed = downloadManager.query(query)?.use { cursor ->
                            cursor.moveToFirst() &&
                                cursor.getInt(
                                    cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                                ) == DownloadManager.STATUS_SUCCESSFUL
                        } == true
                        check(completed) { "Download failed. Please try again." }
                        check(apkFile.isFile && apkFile.length() > 0L) {
                            "Downloaded update is empty."
                        }
                        val actualSha256 = apkFile.inputStream().buffered().use { input ->
                            val digest = MessageDigest.getInstance("SHA-256")
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                digest.update(buffer, 0, read)
                            }
                            digest.digest().joinToString("") { "%02x".format(it) }
                        }
                        check(
                            MessageDigest.isEqual(
                                actualSha256.toByteArray(Charsets.US_ASCII),
                                verifiedDownload.sha256.toByteArray(Charsets.US_ASCII)
                            )
                        ) { "Update verification failed. The file was removed." }
                    }.exceptionOrNull()

                    withContext(Dispatchers.Main.immediate) {
                        if (failure != null) {
                            apkFile.delete()
                            onFailed(failure.message ?: "Download could not be verified.")
                        } else {
                            onDownloaded()
                            install(apkFile)
                        }
                    }
                    pendingResult.finish()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun install(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile
        )
        appContext.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}

internal data class VerifiedUpdateDownload(
    val uri: URI,
    val sha256: String
)

internal fun validateUpdateDownload(
    apkUrl: String,
    apkSha256: String
): VerifiedUpdateDownload {
    val uri = runCatching { URI(apkUrl.trim()) }
        .getOrElse { throw IllegalArgumentException("Update URL is invalid.") }
    val host = uri.host?.lowercase()
    require(
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443) &&
            (host == "vybnet.app" || host?.endsWith(".vybnet.app") == true)
    ) { "Update URL is not an approved Vyb download." }

    val normalizedSha256 = apkSha256.trim().lowercase()
    require(normalizedSha256.matches(Regex("^[a-f0-9]{64}$"))) {
        "Update checksum is missing or invalid."
    }
    return VerifiedUpdateDownload(uri = uri, sha256 = normalizedSha256)
}

internal fun safeVersionLabel(value: String): String =
    value.trim()
        .replace(Regex("[^A-Za-z0-9._-]"), "-")
        .trim('-', '.')
        .take(40)
        .ifEmpty { "update" }
