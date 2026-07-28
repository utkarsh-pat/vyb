package social.vyb.app.features.media

import android.net.Uri
import kotlinx.serialization.Serializable

enum class MediaPublishIntent(val wireValue: String) {
    Post("post"),
    Story("story"),
    Vibe("vibe")
}

data class SelectedMedia(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val mediaType: String,
    val compositionJson: String? = null
)

@Serializable
internal data class UploadSocialMediaRequest(
    val intent: String,
    val fileName: String,
    val mimeType: String,
    val base64Data: String
)

@Serializable
data class UploadedSocialMediaAsset(
    val mediaType: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String,
    val url: String
)

@Serializable
internal data class UploadSocialMediaEnvelope(val asset: UploadedSocialMediaAsset)

@Serializable
internal data class MediaAssetRequest(
    val url: String,
    val kind: String,
    val mimeType: String,
    val sizeBytes: Long,
    val storagePath: String
)

@Serializable
internal data class CreateMediaPostRequest(
    val tenantId: String,
    val membershipId: String,
    val kind: String,
    val placement: String,
    val title: String = "Campus update",
    val body: String,
    val mediaUrl: String,
    val mediaStoragePath: String,
    val mediaMimeType: String,
    val mediaSizeBytes: Long,
    val mediaAssets: List<MediaAssetRequest>,
    val location: String? = null,
    val isAnonymous: Boolean = false,
    val allowAnonymousComments: Boolean = true,
    val visibility: String = "public",
    val communityId: String? = null
)

@Serializable
internal data class CreateMediaStoryRequest(
    val tenantId: String,
    val mediaType: String,
    val mediaUrl: String,
    val mediaStoragePath: String,
    val mediaMimeType: String,
    val mediaSizeBytes: Long,
    val caption: String,
    val allowAnonymousComments: Boolean = true,
    val visibility: String = "public",
    val communityId: String? = null,
    val compositionJson: String? = null
)

@Serializable
internal data class CreatedItemEnvelope(val item: CreatedMediaItem)

@Serializable
data class CreatedMediaItem(
    val id: String,
    val mediaUrl: String? = null,
    val body: String = "",
    val caption: String = ""
)

@Serializable
internal data class MediaViewerEnvelope(val membershipSummary: MediaMembership)

@Serializable
internal data class MediaMembership(
    val id: String,
    val tenantId: String,
    val verificationStatus: String
)

data class MediaComposerUiState(
    val intent: MediaPublishIntent = MediaPublishIntent.Post,
    val selected: List<SelectedMedia> = emptyList(),
    val caption: String = "",
    val location: String = "",
    val isAnonymous: Boolean = false,
    val isPublishing: Boolean = false,
    val progress: Float = 0f,
    val progressLabel: String? = null,
    val error: String? = null,
    val publishedItem: CreatedMediaItem? = null,
    val scheduled: Boolean = false
) {
    val canPublish: Boolean
        get() = selected.isNotEmpty() && !isPublishing &&
            (intent != MediaPublishIntent.Vibe || selected.singleOrNull()?.mediaType == "video")
}

internal const val MAX_SOCIAL_IMAGE_BYTES = 4L * 1024L * 1024L
internal const val MAX_SOCIAL_VIDEO_BYTES = 40L * 1024L * 1024L

internal val supportedSocialMimeTypes = setOf(
    "image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif",
    "video/mp4", "video/webm", "video/quicktime"
)
