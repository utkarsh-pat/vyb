package social.vyb.app.features.messages

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChatScreenshotEvents {
    private val mutableEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = mutableEvents.asSharedFlow()

    fun notifyCaptured() {
        mutableEvents.tryEmit(Unit)
    }
}
