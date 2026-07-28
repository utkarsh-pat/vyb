package social.vyb.app.features.update

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

@Serializable
data class AndroidUpdateManifest(
    val platform: String = "android",
    val latestVersionCode: Int,
    val latestVersionName: String,
    val minimumSupportedVersionCode: Int = 1,
    val forceUpdate: Boolean = false,
    val apkUrl: String,
    val releaseNotes: List<String> = emptyList(),
    val updateAvailable: Boolean = false,
    val updatedAt: String? = null
)

interface AppUpdateApi {
    @GET("v1/app-updates/android")
    suspend fun androidUpdate(
        @Query("versionCode") versionCode: Int,
        @Query("versionName") versionName: String
    ): AndroidUpdateManifest
}
