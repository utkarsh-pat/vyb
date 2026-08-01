package social.vyb.app.features.media

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink

/** Streams a content URI into OkHttp without materializing the media in memory. */
internal class ContentUriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType,
    private val expectedBytes: Long,
) : RequestBody() {
    override fun contentType(): MediaType = mediaType

    override fun contentLength(): Long = expectedBytes

    override fun writeTo(sink: BufferedSink) {
        val source = resolver.openInputStream(uri)
            ?: throw IOException("The selected file is no longer available. Choose it again.")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var written = 0L
        source.use { input ->
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                written += read
                if (written > expectedBytes) {
                    throw IOException("The selected file changed. Choose it again.")
                }
                sink.write(buffer, 0, read)
            }
        }
        if (written != expectedBytes) {
            throw IOException("The selected file changed. Choose it again.")
        }
    }
}

internal fun ContentResolver.measureContentBytes(uri: Uri, maxBytes: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var measured = 0L
    val source = openInputStream(uri)
        ?: error("The selected file is no longer available. Choose it again.")
    source.use { input ->
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            measured += read
            require(measured <= maxBytes) { "The selected file is too large." }
        }
    }
    require(measured > 0L) { "The selected file is empty." }
    return measured
}
