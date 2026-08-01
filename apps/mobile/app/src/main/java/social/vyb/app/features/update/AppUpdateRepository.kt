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
        validateUpdateManifest(manifest, BuildConfig.VERSION_CODE)
        return manifest
    }
}

internal fun validateUpdateManifest(
    manifest: AndroidUpdateManifest,
    currentVersionCode: Int
) {
    require(manifest.platform.equals("android", ignoreCase = true)) {
        "Update metadata targets the wrong platform."
    }
    require(manifest.latestVersionCode >= manifest.minimumSupportedVersionCode) {
        "Update version metadata is inconsistent."
    }
    if (!manifest.updateAvailable) return
    require(manifest.latestVersionCode > currentVersionCode) {
        "Update metadata does not contain a newer version."
    }
    require(manifest.latestVersionName.isNotBlank()) {
        "Update version name is missing."
    }
    // Reject poisoned/incomplete metadata before showing a mandatory prompt
    // that the user may not be able to dismiss.
    validateUpdateDownload(manifest.apkUrl, manifest.apkSha256)
}

internal fun AndroidUpdateManifest.isMandatoryFor(currentVersionCode: Int): Boolean =
    forceUpdate || currentVersionCode < minimumSupportedVersionCode
