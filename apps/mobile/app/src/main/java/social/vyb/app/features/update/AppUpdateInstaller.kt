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
import java.io.File

class AppUpdateInstaller(
    private val context: Context
) {
    private val appContext = context.applicationContext

    fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || appContext.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${appContext.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun downloadAndInstall(
        manifest: AndroidUpdateManifest,
        onStarted: () -> Unit,
        onDownloaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val downloadManager = appContext.getSystemService<DownloadManager>()
        if (downloadManager == null) {
            onFailed("Download service unavailable.")
            return
        }

        val fileName = "Vybnet-${manifest.latestVersionName}.apk"
        val apkFile = File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(manifest.apkUrl))
            .setTitle("Vybnet ${manifest.latestVersionName}")
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
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (!cursor.moveToFirst()) {
                        onFailed("Download could not be verified.")
                        return
                    }
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (status != DownloadManager.STATUS_SUCCESSFUL) {
                        onFailed("Download failed. Please try again.")
                        return
                    }
                }

                onDownloaded()
                install(apkFile)
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
