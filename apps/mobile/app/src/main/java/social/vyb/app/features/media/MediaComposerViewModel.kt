package social.vyb.app.features.media

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaComposerViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = MediaComposerRepository()
    private val _uiState = MutableStateFlow(MediaComposerUiState())
    val uiState: StateFlow<MediaComposerUiState> = _uiState.asStateFlow()

    fun setIntent(intent: MediaPublishIntent) {
        _uiState.update {
            it.copy(
                intent = intent,
                selected = if (intent == MediaPublishIntent.Post) it.selected else it.selected.take(1),
                isAnonymous = if (intent == MediaPublishIntent.Story) false else it.isAnonymous,
                error = null,
                publishedItem = null
            )
        }
    }

    fun setCaption(value: String) {
        _uiState.update { it.copy(caption = value.take(2_000), error = null) }
    }

    fun setLocation(value: String) {
        _uiState.update { it.copy(location = value.take(120), error = null) }
    }

    fun setAnonymous(value: Boolean) {
        _uiState.update { it.copy(isAnonymous = value) }
    }

    fun addSelection(uris: List<Uri>) {
        val intent = _uiState.value.intent
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val max = if (intent == MediaPublishIntent.Post) 4 else 1
                    uris.take(max).map(::inspectSelection)
                }
            }.onSuccess { selections ->
                val invalidVibe = intent == MediaPublishIntent.Vibe &&
                    selections.firstOrNull()?.mediaType != "video"
                _uiState.update {
                    it.copy(
                        selected = if (invalidVibe) emptyList() else selections,
                        error = if (invalidVibe) "Choose a video for your Vibe." else null,
                        publishedItem = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(error = error.message ?: "We could not read that media.")
                }
            }
        }
    }

    fun removeSelection(uri: Uri) {
        _uiState.update { state ->
            state.copy(selected = state.selected.filterNot { it.uri == uri }, error = null)
        }
    }

    fun replaceSelection(media: SelectedMedia) {
        _uiState.update {
            it.copy(selected = listOf(media), error = null, publishedItem = null)
        }
    }

    fun publish(
        isAnonymous: Boolean,
        allowAnonymousComments: Boolean,
        visibility: String,
        communityId: String?
    ) {
        val snapshot = _uiState.value
        if (!snapshot.canPublish) return
        _uiState.update {
            it.copy(
                isPublishing = true,
                progress = 0f,
                progressLabel = "Preparing media",
                error = null,
                publishedItem = null
            )
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    repository.publish(
                        resolver = getApplication<Application>().contentResolver,
                        intent = snapshot.intent,
                        selected = snapshot.selected,
                        caption = snapshot.caption,
                        location = snapshot.location,
                        isAnonymous = isAnonymous && snapshot.intent != MediaPublishIntent.Story,
                        allowAnonymousComments = allowAnonymousComments,
                        visibility = visibility,
                        communityId = communityId
                    ) { progress, label ->
                        _uiState.update { it.copy(progress = progress, progressLabel = label) }
                    }
                }
            }.onSuccess { item ->
                _uiState.update {
                    MediaComposerUiState(
                        intent = snapshot.intent,
                        progress = 1f,
                        progressLabel = "Published",
                        publishedItem = item
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        progressLabel = null,
                        error = error.message ?: "Publish failed. Your draft is still here."
                    )
                }
            }
        }
    }

    fun schedule(
        publishAtMillis: Long,
        isAnonymous: Boolean,
        allowAnonymousComments: Boolean,
        visibility: String,
        communityId: String?
    ) {
        val snapshot = _uiState.value
        if (!snapshot.canPublish) return
        _uiState.update {
            it.copy(
                isPublishing = true,
                progressLabel = "Saving scheduled media",
                error = null,
                scheduled = false
            )
        }
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    ScheduledMediaWorker.stageAndSchedule(
                        context = getApplication(),
                        intent = snapshot.intent,
                        selected = snapshot.selected,
                        caption = snapshot.caption,
                        location = snapshot.location,
                        isAnonymous = isAnonymous && snapshot.intent != MediaPublishIntent.Story,
                        allowAnonymousComments = allowAnonymousComments,
                        visibility = visibility,
                        communityId = communityId,
                        publishAtMillis = publishAtMillis
                    )
                }
            }.onSuccess {
                _uiState.update {
                    MediaComposerUiState(
                        intent = snapshot.intent,
                        progress = 1f,
                        progressLabel = "Scheduled",
                        scheduled = true
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        progressLabel = null,
                        error = error.message ?: "Could not schedule this ${snapshot.intent.name.lowercase()}."
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, publishedItem = null) }
    }

    private fun inspectSelection(uri: Uri): SelectedMedia {
        val resolver = getApplication<Application>().contentResolver
        val mimeType = resolver.getType(uri)?.substringBefore(';')?.lowercase()
            ?: error("This file has no supported media type.")
        require(mimeType in supportedSocialMimeTypes) {
            "Use JPG, PNG, WebP, GIF, HEIC, MP4, WebM, or MOV media."
        }
        val metadata = resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) null
            else {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                Pair(
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null,
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
                )
            }
        }
        val name = metadata?.first?.takeIf(String::isNotBlank) ?: "vyb-upload"
        val size = metadata?.second ?: -1L
        require(size > 0L) { "The selected file is empty or its size cannot be read." }
        val mediaType = if (mimeType.startsWith("video/")) "video" else "image"
        val max = if (mediaType == "video") MAX_SOCIAL_VIDEO_BYTES else MAX_SOCIAL_IMAGE_BYTES
        require(size <= max) {
            if (mediaType == "video") "Keep videos under 40 MB." else "Keep images under 4 MB."
        }
        return SelectedMedia(uri, name, mimeType, size, mediaType)
    }
}
