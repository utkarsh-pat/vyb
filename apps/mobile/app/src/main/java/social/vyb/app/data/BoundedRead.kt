package social.vyb.app.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Reads a content-provider stream without trusting its optional size metadata.
 *
 * Providers may report an unknown or incorrect length. Reading with [InputStream.readBytes]
 * would allow a malformed provider to allocate until the app runs out of memory.
 */
internal fun InputStream.readBytesAtMost(
    maxBytes: Long,
    tooLargeMessage: String = "The selected file is too large."
): ByteArray {
    require(maxBytes in 1..Int.MAX_VALUE.toLong()) { "Invalid byte limit." }
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE.toLong()).toInt())
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        if (count == 0) {
            val singleByte = read()
            if (singleByte < 0) break
            total += 1
            require(total <= maxBytes) { tooLargeMessage }
            output.write(singleByte)
            continue
        }
        total += count
        require(total <= maxBytes) { tooLargeMessage }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
