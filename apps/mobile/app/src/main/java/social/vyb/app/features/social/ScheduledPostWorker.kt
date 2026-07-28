package social.vyb.app.features.social

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.UUID
import java.util.concurrent.TimeUnit

class ScheduledPostWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val text = inputData.getString(KEY_TEXT)?.trim().orEmpty()
        if (text.isEmpty()) return Result.failure()

        return runCatching {
            SocialActionsRepository().createTextPost(
                text = text,
                isAnonymous = inputData.getBoolean(KEY_ANONYMOUS, false),
                allowAnonymousComments = inputData.getBoolean(
                    KEY_ALLOW_ANONYMOUS_COMMENTS,
                    true
                ),
                visibility = inputData.getString(KEY_VISIBILITY)
                    ?: PostReach.Public.wireValue,
                communityId = inputData.getString(KEY_COMMUNITY_ID)
            )
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        )
    }

    companion object {
        private const val KEY_TEXT = "post_text"
        private const val KEY_ANONYMOUS = "post_anonymous"
        private const val KEY_ALLOW_ANONYMOUS_COMMENTS = "allow_anonymous_comments"
        private const val KEY_VISIBILITY = "post_visibility"
        private const val KEY_COMMUNITY_ID = "post_community_id"

        fun schedule(
            context: Context,
            text: String,
            isAnonymous: Boolean,
            allowAnonymousComments: Boolean,
            visibility: String,
            communityId: String?,
            publishAtMillis: Long
        ): UUID {
            val delayMillis = (publishAtMillis - System.currentTimeMillis()).coerceAtLeast(1_000L)
            val request = OneTimeWorkRequestBuilder<ScheduledPostWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_TEXT, text.trim())
                        .putBoolean(KEY_ANONYMOUS, isAnonymous)
                        .putBoolean(
                            KEY_ALLOW_ANONYMOUS_COMMENTS,
                            allowAnonymousComments
                        )
                        .putString(
                            KEY_VISIBILITY,
                            PostReach.fromWireValue(visibility).wireValue
                        )
                        .putString(KEY_COMMUNITY_ID, communityId)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .addTag("vybnet-scheduled-post")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "vybnet-scheduled-post-${request.id}",
                ExistingWorkPolicy.KEEP,
                request
            )
            return request.id
        }
    }
}
