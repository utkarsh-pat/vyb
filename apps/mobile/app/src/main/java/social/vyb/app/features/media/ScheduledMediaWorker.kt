package social.vyb.app.features.media

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ScheduledMediaWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val payload = inputData.getString(KEY_PAYLOAD)?.let {
            runCatching { json.decodeFromString<ScheduledMediaPayload>(it) }.getOrNull()
        } ?: return Result.failure()

        val selected = payload.media.map { media ->
            val file = File(media.path)
            if (!file.isFile) return Result.failure()
            SelectedMedia(
                uri = Uri.fromFile(file),
                fileName = media.fileName,
                mimeType = media.mimeType,
                sizeBytes = media.sizeBytes,
                mediaType = media.mediaType,
                compositionJson = media.compositionJson
            )
        }

        return runCatching {
            MediaComposerRepository().publish(
                resolver = applicationContext.contentResolver,
                intent = MediaPublishIntent.valueOf(payload.intent),
                selected = selected,
                caption = payload.caption,
                location = payload.location,
                isAnonymous = payload.isAnonymous,
                allowAnonymousComments = payload.allowAnonymousComments,
                visibility = payload.visibility,
                communityId = payload.communityId
            ) { _, _ -> }
        }.fold(
            onSuccess = {
                File(payload.stagingDirectory).deleteRecursively()
                Result.success()
            },
            onFailure = {
                if (runAttemptCount < MAX_ATTEMPTS) {
                    Result.retry()
                } else {
                    File(payload.stagingDirectory).deleteRecursively()
                    Result.failure()
                }
            }
        )
    }

    companion object {
        private const val KEY_PAYLOAD = "scheduled_media_payload"
        private const val MAX_ATTEMPTS = 3
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun stageAndSchedule(
            context: Context,
            intent: MediaPublishIntent,
            selected: List<SelectedMedia>,
            caption: String,
            location: String?,
            isAnonymous: Boolean,
            allowAnonymousComments: Boolean,
            visibility: String,
            communityId: String?,
            publishAtMillis: Long
        ): UUID {
            require(selected.isNotEmpty()) { "Choose media before scheduling." }
            val jobId = UUID.randomUUID()
            val stagingDirectory = File(context.filesDir, "scheduled_media/$jobId")
            check(stagingDirectory.mkdirs()) { "Could not prepare scheduled media." }

            try {
                val staged = selected.mapIndexed { index, media ->
                    val extension = media.fileName.substringAfterLast('.', "")
                        .takeIf(String::isNotBlank)
                        ?.let { ".$it" }
                        .orEmpty()
                    val destination = File(stagingDirectory, "$index$extension")
                    context.contentResolver.openInputStream(media.uri)?.use { input ->
                        destination.outputStream().use(input::copyTo)
                    } ?: error("The selected file is no longer available. Choose it again.")
                    check(destination.length() == media.sizeBytes) {
                        "The selected file changed. Choose it again."
                    }
                    ScheduledMediaFile(
                        path = destination.absolutePath,
                        fileName = media.fileName,
                        mimeType = media.mimeType,
                        sizeBytes = media.sizeBytes,
                        mediaType = media.mediaType,
                        compositionJson = media.compositionJson
                    )
                }
                val payload = ScheduledMediaPayload(
                    intent = intent.name,
                    media = staged,
                    caption = caption.trim(),
                    location = location?.trim()?.takeIf(String::isNotEmpty),
                    isAnonymous = isAnonymous && intent != MediaPublishIntent.Story,
                    allowAnonymousComments = allowAnonymousComments,
                    visibility = visibility,
                    communityId = communityId,
                    stagingDirectory = stagingDirectory.absolutePath
                )
                val delayMillis =
                    (publishAtMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
                val request = OneTimeWorkRequestBuilder<ScheduledMediaWorker>()
                    .setInputData(
                        Data.Builder()
                            .putString(KEY_PAYLOAD, json.encodeToString(payload))
                            .build()
                    )
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .addTag("vybnet-scheduled-media")
                    .build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "vybnet-scheduled-media-$jobId",
                    ExistingWorkPolicy.KEEP,
                    request
                )
                return request.id
            } catch (error: Throwable) {
                stagingDirectory.deleteRecursively()
                throw error
            }
        }
    }
}

@Serializable
private data class ScheduledMediaPayload(
    val intent: String,
    val media: List<ScheduledMediaFile>,
    val caption: String,
    val location: String?,
    val isAnonymous: Boolean,
    val allowAnonymousComments: Boolean,
    val visibility: String,
    val communityId: String?,
    val stagingDirectory: String
)

@Serializable
private data class ScheduledMediaFile(
    val path: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val mediaType: String,
    val compositionJson: String? = null
)
