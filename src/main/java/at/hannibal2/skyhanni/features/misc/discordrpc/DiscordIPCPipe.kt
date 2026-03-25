package at.hannibal2.skyhanni.features.misc.discordrpc

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.nio.file.Path

/**
 * An open, platform-specific IPC pipe to a Discord client.
 *
 * Obtain instances via [DiscordIPCPipeManager.open].
 */
sealed class DiscordIPCPipe : Closeable {
    abstract val input: InputStream
    abstract val output: OutputStream

    /**
     * Windows named-pipe backed by [java.io.RandomAccessFile].
     *
     * [java.io.RandomAccessFile] is used because Windows named pipes are not openable via
     * [java.io.FileInputStream] directly.
     */
    class Windows(path: String) : DiscordIPCPipe() {
        private val pipe = RandomAccessFile(path, "rw")
        override val input = object : InputStream() {
            override fun read() = pipe.read()
            override fun read(b: ByteArray, off: Int, len: Int) = pipe.read(b, off, len)
        }
        override val output = object : OutputStream() {
            override fun write(b: Int) = pipe.write(b)
            override fun write(b: ByteArray, off: Int, len: Int) = pipe.write(b, off, len)
        }

        override fun close() = pipe.close()
    }

    /** Unix domain socket backed by a Java 16+ [java.nio.channels.SocketChannel]. */
    class Unix(path: Path) : DiscordIPCPipe() {
        private val channel = SocketChannel.open(StandardProtocolFamily.UNIX).apply {
            connect(UnixDomainSocketAddress.of(path))
        }
        override val input = object : InputStream() {
            override fun read(): Int {
                val buf = ByteBuffer.allocate(1)
                return if (channel.read(buf) == -1) -1 else (buf.flip().get().toInt() and 0xFF)
            }

            override fun read(b: ByteArray, off: Int, len: Int) = channel.read(ByteBuffer.wrap(b, off, len))
        }
        override val output = object : OutputStream() {
            override fun write(b: Int) = write(byteArrayOf(b.toByte()))
            override fun write(b: ByteArray, off: Int, len: Int) {
                val buf = ByteBuffer.wrap(b, off, len)
                while (buf.hasRemaining()) channel.write(buf)
            }
        }

        override fun close() = channel.close()
    }
}
