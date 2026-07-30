package social.vyb.app.features.media

import android.content.ContentResolver
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken

class MediaComposerRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: MediaComposerApi = VybNetwork.create(
        connectTimeoutSeconds = 20,
        readTimeoutSeconds = 120,
        writeTimeoutSeconds = 120
    )

    suspend fun publish(
        resolver: ContentResolver,
        intent: MediaPublishIntent,
        selected: List<SelectedMedia>,
        caption: String,
        location: String?,
        isAnonymous: Boolean,
        allowAnonymousComments: Boolean = true,
        visibility: String = "public",
        communityId: String? = null,
        onProgress: (Float, String) -> Unit
    ): CreatedMediaItem = apiCall {
        require(selected.isNotEmpty()) { "Choose media before publishing." }
        require(intent != MediaPublishIntent.Vibe || selected.singleOrNull()?.mediaType == "video") {
            "A Vibe needs exactly one video."
        }
        selected.firstOrNull()?.compositionJson?.let {
            require(it.toByteArray().size <= 64 * 1024) {
                "Story composition metadata must stay under 64 KB."
            }
        }

        val bearer = bearerToken()
        val viewer = api.viewer(bearer).membershipSummary
        check(viewer.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }

        val uploaded = selected.mapIndexed { index, media ->
            onProgress(
                index.toFloat() / (selected.size + 1),
                "Uploading ${index + 1} of ${selected.size}"
            )
            val bytes = resolver.openInputStream(media.uri)?.use { it.readBytes() }
                ?: error("The selected file is no longer available. Choose it again.")
            check(bytes.size.toLong() == media.sizeBytes || media.sizeBytes < 0) {
                "The selected file changed. Choose it again."
            }
            api.upload(
                bearer,
                UploadSocialMediaRequest(
                    intent = intent.wireValue,
                    fileName = media.fileName,
                    mimeType = media.mimeType,
                    base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
                )
            ).asset
        }

        onProgress(selected.size.toFloat() / (selected.size + 1), "Publishing")
        val primary = uploaded.first()
        val result = when (intent) {
            MediaPublishIntent.Story -> api.createStory(
                bearer,
                CreateMediaStoryRequest(
                    tenantId = viewer.tenantId,
                    mediaType = primary.mediaType,
                    mediaUrl = primary.url,
                    mediaStoragePath = primary.storagePath,
                    mediaMimeType = primary.mimeType,
                    mediaSizeBytes = primary.sizeBytes,
                    caption = caption.trim(),
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = visibility,
                    communityId = communityId,
                    compositionJson = selected.first().compositionJson
                )
            )
            MediaPublishIntent.Post, MediaPublishIntent.Vibe -> api.createPost(
                bearer,
                CreateMediaPostRequest(
                    tenantId = viewer.tenantId,
                    membershipId = viewer.id,
                    kind = primary.mediaType,
                    placement = if (intent == MediaPublishIntent.Vibe) "vibe" else "feed",
                    body = caption.trim(),
                    mediaUrl = primary.url,
                    mediaStoragePath = primary.storagePath,
                    mediaMimeType = primary.mimeType,
                    mediaSizeBytes = primary.sizeBytes,
                    mediaAssets = uploaded.map {
                        MediaAssetRequest(
                            url = it.url,
                            kind = it.mediaType,
                            mimeType = it.mimeType,
                            sizeBytes = it.sizeBytes,
                            storagePath = it.storagePath
                        )
                    },
                    location = location?.trim()?.takeIf(String::isNotEmpty),
                    isAnonymous = isAnonymous && intent != MediaPublishIntent.Story,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = visibility,
                    communityId = communityId
                )
            )
        }
        onProgress(1f, "Published")
        result.item
    }

    private suspend fun bearerToken(): String = auth.requireBearerToken()

    private suspend fun <T> apiCall(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val backendMessage = error.response()?.errorBody()?.string()?.let {
            runCatching {
                VybNetwork.json.decodeFromString<MediaErrorEnvelope>(it).error?.message
            }.getOrNull()
        }
        throw MediaComposerException(
            backendMessage ?: "Request failed (${error.code()}). Please try again.",
            error
        )
    }
}

class MediaComposerException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

@Serializable
private data class MediaErrorEnvelope(val error: MediaErrorBody? = null)

@Serializable
private data class MediaErrorBody(val message: String? = null)
