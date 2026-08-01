package social.vyb.app.features.media

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.HttpException
import social.vyb.app.data.network.VybNetwork
import social.vyb.app.data.network.requireBearerToken
import social.vyb.app.data.readBytesAtMost

class MediaComposerRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val api: MediaComposerApi = VybNetwork.create(
        connectTimeoutSeconds = 20,
        readTimeoutSeconds = 120,
        writeTimeoutSeconds = 120
    )

    suspend fun uploadCommentImage(
        resolver: ContentResolver,
        uri: Uri,
        fileName: String,
        mimeType: String
    ): UploadedSocialMediaAsset = apiCall {
        require(mimeType in supportedSocialMimeTypes && mimeType.startsWith("image/")) {
            "Choose a supported image."
        }
        val sizeBytes = resolver.measureContentBytes(uri, MAX_SOCIAL_IMAGE_BYTES)
        require(sizeBytes <= MAX_SOCIAL_IMAGE_BYTES) { "Keep comment images under 4 MB." }
        uploadAsset(
            bearer = bearerToken(),
            resolver = resolver,
            uri = uri,
            intent = MediaPublishIntent.Post,
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = sizeBytes
        )
    }

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
        selected.mapNotNull(SelectedMedia::compositionJson).forEach {
            require(it.toByteArray().size <= 64 * 1024) {
                "Story composition metadata must stay under 64 KB."
            }
        }

        val bearer = bearerToken()
        val viewer = api.viewer(bearer).membershipSummary
        check(viewer.verificationStatus == "verified") {
            "Your campus membership is not verified yet."
        }

        val completedUploads = AtomicInteger(0)
        val uploadGate = Semaphore(permits = minOf(3, selected.size))
        onProgress(0f, "Uploading ${selected.size} files in parallel")
        val uploaded = coroutineScope {
            selected.mapIndexed { index, media ->
                async {
                    val asset = uploadGate.withPermit {
                        val expectedMediaType = if (media.mimeType.startsWith("video/")) "video" else "image"
                        require(media.mimeType in supportedSocialMimeTypes && media.mediaType == expectedMediaType) {
                            "The selected file type is not supported."
                        }
                        val byteLimit = if (expectedMediaType == "video") {
                            MAX_SOCIAL_VIDEO_BYTES
                        } else {
                            MAX_SOCIAL_IMAGE_BYTES
                        }
                        val sizeBytes = media.sizeBytes.takeIf { it > 0L }
                            ?: resolver.measureContentBytes(media.uri, byteLimit)
                        require(sizeBytes <= byteLimit) {
                            if (expectedMediaType == "video") "Keep videos under 40 MB." else "Keep images under 4 MB."
                        }
                        uploadAsset(
                            bearer = bearer,
                            resolver = resolver,
                            uri = media.uri,
                            intent = intent,
                            fileName = media.fileName,
                            mimeType = media.mimeType,
                            sizeBytes = sizeBytes
                        )
                    }
                    val completed = completedUploads.incrementAndGet()
                    onProgress(
                        completed.toFloat() / (selected.size + 1),
                        "Uploaded $completed of ${selected.size}"
                    )
                    index to asset
                }
            }.awaitAll().sortedBy { it.first }.map { it.second }
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

    /**
     * Prefer the constant-memory binary endpoint. Older backend revisions expected the legacy
     * JSON envelope, so retry that exact contract only for a 400 response during rollout.
     */
    private suspend fun uploadAsset(
        bearer: String,
        resolver: ContentResolver,
        uri: Uri,
        intent: MediaPublishIntent,
        fileName: String,
        mimeType: String,
        sizeBytes: Long
    ): UploadedSocialMediaAsset = try {
        api.uploadStream(
            bearer,
            intent.wireValue,
            fileName,
            ContentUriRequestBody(
                resolver = resolver,
                uri = uri,
                mediaType = mimeType.toMediaType(),
                expectedBytes = sizeBytes
            )
        ).asset
    } catch (error: HttpException) {
        if (error.code() != 400) throw error
        val bytes = resolver.openInputStream(uri)?.use { input ->
            input.readBytesAtMost(sizeBytes, "The selected file changed. Choose it again.")
        } ?: error("The selected file is no longer available. Choose it again.")
        api.uploadLegacy(
            bearer,
            LegacySocialMediaUploadRequest(
                intent = intent.wireValue,
                fileName = fileName,
                mimeType = mimeType,
                base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
            )
        ).asset
    }

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
