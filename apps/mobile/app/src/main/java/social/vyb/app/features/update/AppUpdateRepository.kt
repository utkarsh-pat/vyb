package social.vyb.app.features.update

import social.vyb.app.BuildConfig
import social.vyb.app.data.network.VybNetwork

class AppUpdateRepository {
    private val api: AppUpdateApi = VybNetwork.create(connectTimeoutSeconds = 15)

    suspend fun check(): AndroidUpdateManifest {
        val manifest = api.androidUpdate(
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME
        )
        if (manifest.updateAvailable) {
            // Reject poisoned/incomplete metadata before showing a force-update
            // prompt that the user may not be able to dismiss.
            validateUpdateDownload(manifest.apkUrl, manifest.apkSha256)
        }
        return manifest
    }
}
