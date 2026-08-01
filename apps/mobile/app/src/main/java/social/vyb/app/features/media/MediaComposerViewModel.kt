package social.vyb.app.features.media

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID

class MediaComposerViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val repository = MediaComposerRepository()
    private val _uiState = MutableStateFlow(MediaComposerUiState())
    val uiState: StateFlow<MediaComposerUiState> = _uiState.asStateFlow()
    private var selectionJob: Job? = null
    private var scheduledPublishJob: Job? = null
    private val draftStore = application.getSharedPreferences("media_composer_drafts", 0)
    private val draftJson = Json { ignoreUnknownKeys = true }
    private val draftsKey = "drafts_v2"

    init {
        refreshDraftSummaries()
    }

    fun loadDraft(id: String) {
        val draft = readDrafts().firstOrNull { it.id == id } ?: return
        val intent = MediaPublishIntent.entries.firstOrNull { it.wireValue == draft.intent }
            ?: MediaPublishIntent.Post
        _uiState.update { current ->
            current.copy(
                intent = intent,
                caption = draft.caption,
                location = draft.location,
                selected = draft.selected.map { stored ->
                    SelectedMedia(
                        uri = Uri.parse(stored.uri),
                        fileName = stored.fileName,
                        mimeType = stored.mimeType,
                        sizeBytes = stored.sizeBytes,
                        mediaType = stored.mediaType,
                        compositionJson = stored.compositionJson
                    )
                }.take(if (intent == MediaPublishIntent.Post) MAX_POST_MEDIA_ITEMS else 1),
                activeDraftId = draft.id,
                scheduledForMillis = draft.scheduledForMillis,
                notice = "Draft loaded",
                error = null
            )
        }
    }

    fun saveDraft(
        isAnonymous: Boolean,
        allowAnonymousComments: Boolean,
        visibility: String,
        communityId: String?,
        announce: Boolean = false,
        scheduledForMillis: Long? = _uiState.value.scheduledForMillis
    ): String? {
        val state = _uiState.value
        if (state.caption.isBlank() && state.selected.isEmpty()) {
            return null
        }
        val draftId = state.activeDraftId ?: UUID.randomUUID().toString()
        val draft = PersistedMediaDraft(
            id = draftId,
            savedAtMillis = System.currentTimeMillis(),
            scheduledForMillis = scheduledForMillis,
            intent = state.intent.wireValue,
            caption = state.caption,
            location = state.location,
            selected = state.selected.map { media ->
                PersistedSelectedMedia(
                    uri = media.uri.toString(),
                    fileName = media.fileName,
                    mimeType = media.mimeType,
                    sizeBytes = media.sizeBytes,
                    mediaType = media.mediaType,
                    compositionJson = media.compositionJson
                )
            },
            isAnonymous = isAnonymous,
            allowAnonymousComments = allowAnonymousComments,
            visibility = visibility,
            communityId = communityId
        )
        val drafts = readDrafts().filterNot { it.id == draftId }.toMutableList().apply {
            add(draft)
        }.sortedByDescending { it.savedAtMillis }
        writeDrafts(drafts)
        _uiState.update {
            it.copy(
                activeDraftId = draftId,
                drafts = drafts.map(PersistedMediaDraft::toSummary),
                notice = if (announce) "Saved as draft" else it.notice
            )
        }
        return draftId
    }

    fun discardDraft(id: String) {
        val drafts = readDrafts().filterNot { it.id == id }
        writeDrafts(drafts)
        _uiState.update { state ->
            if (state.activeDraftId == id) {
                MediaComposerUiState(
                    intent = state.intent,
                    drafts = drafts.map(PersistedMediaDraft::toSummary),
                    notice = "Draft discarded"
                )
            } else {
                state.copy(drafts = drafts.map(PersistedMediaDraft::toSummary))
            }
        }
    }

    fun resetComposer(intent: MediaPublishIntent = _uiState.value.intent) {
        selectionJob?.cancel()
        _uiState.update { state ->
            MediaComposerUiState(intent = intent, drafts = state.drafts)
        }
    }

    fun setIntent(intent: MediaPublishIntent) {
        selectionJob?.cancel()
        _uiState.update {
            it.copy(
                intent = intent,
                selected = if (intent == MediaPublishIntent.Post) it.selected else it.selected.take(1),
                isAnonymous = if (intent == MediaPublishIntent.Story) false else it.isAnonymous,
                error = null,
                publishedItem = null,
                activeDraftId = if (it.intent == intent) it.activeDraftId else null
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
        selectionJob?.cancel()
        selectionJob = viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val max = if (intent == MediaPublishIntent.Post) MAX_POST_MEDIA_ITEMS else 1
                    uris.take(max).map(::inspectSelection)
                }
            }
            currentCoroutineContext().ensureActive()
            if (_uiState.value.intent != intent) return@launch
            result.onSuccess { selections ->
                val invalidVibe = intent == MediaPublishIntent.Vibe &&
                    selections.firstOrNull()?.mediaType != "video"
                _uiState.update {
                    val max = if (intent == MediaPublishIntent.Post) MAX_POST_MEDIA_ITEMS else 1
                    val merged = if (intent == MediaPublishIntent.Post) {
                        (it.selected + selections).distinctBy { media -> media.uri }.take(max)
                    } else selections.take(1)
                    it.copy(
                        selected = if (invalidVibe) emptyList() else merged,
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

    fun updateSelection(originalUri: Uri, media: SelectedMedia) {
        _uiState.update { state ->
            state.copy(
                selected = state.selected.map { if (it.uri == originalUri) media else it },
                error = null,
                publishedItem = null
            )
        }
    }

    fun moveSelection(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val reordered = reorderMediaItems(state.selected, fromIndex, toIndex)
            if (reordered === state.selected) state
            else state.copy(
                selected = reordered,
                publishedItem = null
            )
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
                snapshot.activeDraftId?.let(::removeDraftSilently)
                _uiState.update {
                    MediaComposerUiState(
                        intent = snapshot.intent,
                        drafts = readDrafts().map(PersistedMediaDraft::toSummary),
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

    fun schedulePublish(
        delayMillis: Long,
        isAnonymous: Boolean,
        allowAnonymousComments: Boolean,
        visibility: String,
        communityId: String?
    ) {
        val snapshot = _uiState.value
        if (!snapshot.canPublish || delayMillis <= 0L) return
        scheduledPublishJob?.cancel()
        val scheduledFor = System.currentTimeMillis() + delayMillis
        saveDraft(
            isAnonymous,
            allowAnonymousComments,
            visibility,
            communityId,
            scheduledForMillis = scheduledFor
        )
        _uiState.update {
            it.copy(
                scheduledForMillis = scheduledFor,
                notice = "Scheduled. Keep Vyb installed; publishing resumes while the app is open.",
                error = null
            )
        }
        scheduledPublishJob = viewModelScope.launch {
            delay(delayMillis)
            _uiState.update {
                it.copy(isPublishing = true, progress = 0f, progressLabel = "Publishing scheduled post")
            }
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
                _uiState.value.activeDraftId?.let(::removeDraftSilently)
                _uiState.update {
                    MediaComposerUiState(
                        intent = snapshot.intent,
                        drafts = readDrafts().map(PersistedMediaDraft::toSummary),
                        progress = 1f,
                        progressLabel = "Published",
                        publishedItem = item
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPublishing = false,
                        scheduledForMillis = null,
                        progressLabel = null,
                        error = error.message ?: "Scheduled publish failed. Your draft is safe."
                    )
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, notice = null, publishedItem = null) }
    }

    private fun refreshDraftSummaries() {
        val drafts = readDrafts()
        _uiState.update { it.copy(drafts = drafts.map(PersistedMediaDraft::toSummary)) }
    }

    private fun readDrafts(): List<PersistedMediaDraft> {
        val current = draftStore.getString(draftsKey, null)?.let { raw ->
            runCatching {
                draftJson.decodeFromString<PersistedMediaDraftCollection>(raw).drafts
            }.getOrNull()
        }
        if (current != null) return current.sortedByDescending { it.savedAtMillis }

        // One-time migration from the previous one-draft-per-intent format.
        val migrated = MediaPublishIntent.entries.mapNotNull { intent ->
            draftStore.getString(intent.wireValue, null)?.let { raw ->
                runCatching { draftJson.decodeFromString<PersistedMediaDraft>(raw) }.getOrNull()
            }?.copy(
                id = UUID.randomUUID().toString(),
                savedAtMillis = System.currentTimeMillis()
            )
        }
        if (migrated.isNotEmpty()) {
            writeDrafts(migrated)
            draftStore.edit().apply {
                MediaPublishIntent.entries.forEach { remove(it.wireValue) }
            }.apply()
        }
        return migrated
    }

    private fun writeDrafts(drafts: List<PersistedMediaDraft>) {
        draftStore.edit().putString(
            draftsKey,
            draftJson.encodeToString(PersistedMediaDraftCollection(drafts))
        ).apply()
    }

    private fun removeDraftSilently(id: String) {
        writeDrafts(readDrafts().filterNot { it.id == id })
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
        require(size != 0L) { "The selected file is empty." }
        val mediaType = if (mimeType.startsWith("video/")) "video" else "image"
        val max = if (mediaType == "video") MAX_SOCIAL_VIDEO_BYTES else MAX_SOCIAL_IMAGE_BYTES
        require(size < 0L || size <= max) {
            if (mediaType == "video") "Keep videos under 40 MB." else "Keep images under 4 MB."
        }
        return SelectedMedia(uri, name, mimeType, size, mediaType)
    }
}

private fun PersistedMediaDraft.toSummary(): MediaDraftSummary = MediaDraftSummary(
    id = id,
    intent = MediaPublishIntent.entries.firstOrNull { it.wireValue == intent }
        ?: MediaPublishIntent.Post,
    caption = caption,
    mediaCount = selected.size,
    savedAtMillis = savedAtMillis,
    scheduledForMillis = scheduledForMillis
)

internal fun <T> reorderMediaItems(items: List<T>, fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return items
    return items.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
}
