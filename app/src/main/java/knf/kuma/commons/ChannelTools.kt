package knf.kuma.commons

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

object ChannelTools {
    @Throws(IOException::class)
    fun fastChannelCopy(src: ReadableByteChannel, dest: WritableByteChannel) {
        val buffer = ByteBuffer.allocateDirect(16 * 1024)
        while (src.read(buffer) != -1) {
            // prepare the buffer to be drained
            buffer.flip()
            // write to the channel, may block
            dest.write(buffer)
            // If partial transfer, shift remainder down
            // If buffer is empty, same as doing clear()
            buffer.compact()
        }
        // EOF will leave buffer in fill state
        buffer.flip()
        // make sure the buffer is fully drained.
        while (buffer.hasRemaining()) {
            dest.write(buffer)
        }
    }


}
