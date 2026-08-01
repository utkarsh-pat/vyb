package social.vyb.app.data

import java.io.ByteArrayInputStream
import java.io.InputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BoundedReadTest {
    @Test
    fun `reads payload exactly at configured limit`() {
        val payload = ByteArray(16_384) { (it % 251).toByte() }

        val result = ByteArrayInputStream(payload).readBytesAtMost(payload.size.toLong())

        assertArrayEquals(payload, result)
    }

    @Test
    fun `rejects stream as soon as it exceeds configured limit`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ByteArrayInputStream(ByteArray(9_000))
                .readBytesAtMost(8_192, "Media exceeds limit.")
        }

        org.junit.Assert.assertEquals("Media exceeds limit.", error.message)
    }

    @Test
    fun `rejects invalid limits before reading`() {
        assertThrows(IllegalArgumentException::class.java) {
            ByteArrayInputStream(byteArrayOf(1)).readBytesAtMost(0)
        }
    }

    @Test
    fun `zero-length provider reads cannot spin forever`() {
        val source = byteArrayOf(7, 8, 9)
        var index = 0
        var returnedZero = false
        val stream = object : InputStream() {
            override fun read(target: ByteArray, offset: Int, length: Int): Int {
                if (!returnedZero) {
                    returnedZero = true
                    return 0
                }
                if (index >= source.size) return -1
                val count = minOf(length, source.size - index)
                source.copyInto(target, offset, index, index + count)
                index += count
                return count
            }

            override fun read(): Int =
                if (index >= source.size) -1 else source[index++].toInt() and 0xff
        }

        assertArrayEquals(source, stream.readBytesAtMost(source.size.toLong()))
    }
}
