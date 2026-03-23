package at.hannibal2.skyhanni.features.misc.discordrpc

import at.hannibal2.skyhanni.config.ConfigManager
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.SocketChannel
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * A lightweight Discord IPC client implementing the Rich Presence protocol over Discord's local IPC pipe.
 *
 * Manages pipe discovery, handshake, presence updates, and clean disconnection.
 * Uses named pipes on Windows and Unix domain sockets on Linux/macOS.
 *
 * @param clientId The Discord application client ID (from the Discord Developer Portal).
 */
class DiscordIPC(private val clientId: Long) : Closeable {

    @Volatile
    private var _connected = false
    private var connection: IPCConnection? = null

    /** Whether this client is currently connected to and ready for Discord IPC. */
    val isConnected: Boolean get() = _connected
    private val clientPayload = """{"v":1,"client_id":"$clientId"}"""

    /**
     * Discovers an active Discord IPC pipe, opens a connection, and performs the version-1 handshake.
     * Blocks until a READY frame is received from Discord, confirming the connection is active.
     *
     * @throws DiscordIPCException If no Discord client is running, the pipe cannot be opened,
     *   or the handshake does not complete successfully.
     */
    fun connect() {
        connection = findConnection()
        sendFrame(Opcode.HANDSHAKE, clientPayload)
        val (opcode, body) = readFrame()
        if (opcode != Opcode.FRAME) throw DiscordIPCException("Expected FRAME after handshake, got $opcode. Body: $body")
        _connected = true
    }

    /**
     * Updates the rich presence activity currently displayed on the user's Discord profile.
     *
     * @param presence The [DiscordRichPresence] data to send. Null fields are omitted from the payload.
     * @throws DiscordIPCException If the client is not connected or if writing to the pipe fails.
     */
    fun setActivity(presence: DiscordRichPresence) {
        if (!_connected) throw DiscordIPCException("setActivity called while not connected")
        sendFrame(Opcode.FRAME, ConfigManager.gson.toJson(buildActivityPayload(presence)))
    }

    /**
     * Sends a CLOSE frame to Discord and releases all pipe resources.
     * Safe to call when not connected; any errors during the close frame write are silently swallowed
     * since the connection is being torn down regardless.
     */
    override fun close() {
        if (_connected) runCatching { sendFrame(Opcode.CLOSE, clientPayload) }
        _connected = false
        connection?.close()
        connection = null
    }

    private enum class Opcode(val id: Int) {
        HANDSHAKE(0),
        FRAME(1),
        CLOSE(2),
        PING(3),
        PONG(4);

        companion object {
            fun fromId(id: Int) = entries.firstOrNull { it.id == id }
        }
    }

    /**
     * Writes a single framed IPC message to the pipe output stream.
     *
     * Discord's IPC wire format is: `[opcode: Int32LE][length: Int32LE][payload: UTF-8 bytes]`.
     *
     * Synchronized to guard against concurrent writes from the presence update loop and [close].
     *
     * @param opcode The [Opcode] for this frame.
     * @param json The JSON payload string to send.
     * @throws DiscordIPCException If there is no active pipe connection.
     */
    @Synchronized
    private fun sendFrame(opcode: Opcode, json: String) {
        val out = connection?.output ?: throw DiscordIPCException("sendFrame called with no active connection")
        val bytes = json.toByteArray(Charsets.UTF_8)
        val frame = ByteBuffer.allocate(8 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(opcode.id)
            .putInt(bytes.size)
            .put(bytes)
        out.write(frame.array())
        out.flush()
    }

    /**
     * Reads one framed IPC message from the pipe input stream. Blocks until a full frame is available.
     *
     * Sets [isConnected] to false and throws if Discord closes the pipe mid-read (EOF in header).
     *
     * @return A pair of the received [Opcode] and its decoded JSON payload string.
     * @throws DiscordIPCException If the connection is closed by Discord or an unrecognized opcode is received.
     */
    @Suppress("ThrowsCount")
    private fun readFrame(): Pair<Opcode, String> {
        val inp = connection?.input ?: throw DiscordIPCException("readFrame called with no active connection")
        val header = inp.readNBytes(8)
        if (header.size < 8) {
            _connected = false
            throw DiscordIPCException("Discord closed the IPC pipe unexpectedly (EOF in frame header)")
        }
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        val opcodeId = buffer.int
        val opcode = Opcode.fromId(opcodeId) ?: throw DiscordIPCException("Received unknown opcode: $opcodeId")
        val length = buffer.int
        return opcode to String(inp.readNBytes(length), Charsets.UTF_8)
    }

    /**
     * Builds the full `SET_ACTIVITY` JSON payload for the given [presence].
     *
     * The payload structure follows the Discord RPC protocol:
     * `{ cmd, args: { pid, activity: { ... } }, nonce }`.
     *
     * @param presence The [DiscordRichPresence] to serialize into the payload.
     * @return A [JsonObject] ready to be serialized and sent as a FRAME.
     */
    private fun buildActivityPayload(presence: DiscordRichPresence): JsonObject {
        val activity = JsonObject().apply {
            presence.details?.let { addProperty("details", it) }
            presence.state?.let { addProperty("state", it) }
            presence.startTimestamp?.let { start ->
                JsonObject().apply {
                    addProperty("start", start)
                }.also { add("timestamps", it) }
            }

            if (presence.largeImageKey != null || presence.largeImageText != null) JsonObject().apply {
                presence.largeImageKey?.let { addProperty("large_image", it) }
                presence.largeImageText?.let { addProperty("large_text", it) }
            }.let { add("assets", it) }

            if (presence.buttons.isNotEmpty()) JsonArray().apply {
                presence.buttons.forEach { (label, url) ->
                    JsonObject().apply {
                        addProperty("label", label)
                        addProperty("url", url)
                    }.let { add(it) }
                }
            }.let { add("buttons", it) }
        }

        return JsonObject().apply {
            addProperty("cmd", "SET_ACTIVITY")
            add(
                "args",
                JsonObject().apply {
                    addProperty("pid", ProcessHandle.current().pid().toInt())
                    add("activity", activity)
                },
            )
            addProperty("nonce", UUID.randomUUID().toString())
        }
    }

    /**
     * Locates an active Discord IPC pipe and returns an open [IPCConnection] to it.
     *
     * On Windows, tries named pipes `\\.\pipe\discord-ipc-{0..9}` in order, returning
     * the first that opens without error.
     *
     * On Unix, resolves candidate socket directories from environment variables
     * (`XDG_RUNTIME_DIR`, `TMPDIR`, `TMP`, `TEMP`, `/tmp`) and tries
     * `discord-ipc-{0..9}` within each, returning the first that exists and connects.
     *
     * @return An open [IPCConnection] backed by either a named pipe or Unix domain socket.
     * @throws DiscordIPCException If no Discord IPC pipe is found across all candidates.
     */
    private fun findConnection(): IPCConnection {
        if (isWindows) {
            for (i in 0..9) runCatching { return WindowsIPCConnection("\\\\.\\pipe\\discord-ipc-$i") }
            throw DiscordIPCException("No Discord IPC pipe found on Windows. Is Discord running?")
        }

        val uid = runCatching {
            ProcessBuilder("id", "-u").start().inputStream.bufferedReader().readLine()?.trim()
        }.getOrNull()

        val flatpakDirs = uid?.let {
            runCatching {
                Files.list(Path("/run/user/$it/.flatpak")).map { app -> "$app/xdg-run" }.toList()
            }.getOrDefault(emptyList())
        }.orEmpty()

        val dirs = listOfNotNull(
            System.getenv("XDG_RUNTIME_DIR"),
            uid?.let { "/run/user/$it" },
            uid?.let { "/run/user/$it/app/com.discordapp.Discord" },
            uid?.let { "/run/user/$it/app/com.discordapp.DiscordCanary" },
            uid?.let { "/run/user/$it/app/com.discordapp.DiscordPTB" },
            uid?.let { "/run/user/$it/snap.discord" },
            uid?.let { "/run/user/$it/snap.discord-canary" },
            uid?.let { "/run/user/$it/snap.discord-ptb" },
            System.getenv("TMPDIR"),
            System.getenv("TMP"),
            System.getenv("TEMP"),
            "/tmp",
        ) + flatpakDirs

        var lastError: Throwable? = null
        for (dir in dirs) {
            for (i in 0..9) {
                val path = Path("$dir/discord-ipc-$i")
                if (!path.exists()) continue
                runCatching { return UnixIPCConnection(path) }.onFailure { lastError = it }
            }
        }
        throw DiscordIPCException("No Discord IPC socket found on Unix. Is Discord running? Last error: ${lastError?.message}", lastError)
    }

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    /**
     * Abstracts the raw byte I/O channel to the Discord IPC pipe,
     * regardless of whether it is backed by a Windows named pipe or a Unix domain socket.
     */
    private interface IPCConnection : Closeable {
        val input: InputStream
        val output: OutputStream
    }

    /**
     * Windows named-pipe connection via [RandomAccessFile].
     *
     * [RandomAccessFile] is used because Windows named pipes are not files in the traditional
     * sense and are not openable via [java.io.FileInputStream] directly. Input and output
     * streams are derived from the underlying [java.io.FileDescriptor].
     *
     * @param path The Windows named pipe path (e.g. `\\.\pipe\discord-ipc-0`).
     */
    private class WindowsIPCConnection(path: String) : IPCConnection {
        private val pipe = RandomAccessFile(path, "rw")
        override val input: InputStream = object : InputStream() {
            override fun read() = pipe.read()
            override fun read(b: ByteArray, off: Int, len: Int) = pipe.read(b, off, len)
        }
        override val output: OutputStream = object : OutputStream() {
            override fun write(b: Int) = pipe.write(b)
            override fun write(b: ByteArray, off: Int, len: Int) = pipe.write(b, off, len)
        }
        override fun close() = pipe.close()
    }

    /**
     * Unix domain socket connection using Java 16+ [StandardProtocolFamily.UNIX].
     *
     * @param path The filesystem path of the Discord IPC socket file.
     */
    private class UnixIPCConnection(path: Path) : IPCConnection {
        private val channel = SocketChannel.open(StandardProtocolFamily.UNIX).apply {
            connect(UnixDomainSocketAddress.of(path))
        }
        override val input: InputStream = object : InputStream() {
            override fun read(): Int {
                val buf = ByteBuffer.allocate(1)
                return if (channel.read(buf) == -1) -1 else (buf.flip().get().toInt() and 0xFF)
            }
            override fun read(b: ByteArray, off: Int, len: Int) = channel.read(ByteBuffer.wrap(b, off, len))
        }
        override val output: OutputStream = object : OutputStream() {
            override fun write(b: Int) = write(byteArrayOf(b.toByte()))
            override fun write(b: ByteArray, off: Int, len: Int) {
                val buf = ByteBuffer.wrap(b, off, len)
                while (buf.hasRemaining()) channel.write(buf)
            }
        }
        override fun close() = channel.close()
    }
}

/**
 * Thrown when the Discord IPC client encounters a connectivity or protocol-level error.
 *
 * Common causes include Discord not running (no pipe found), an unexpected EOF during
 * frame reading, or an unrecognized opcode in a server response.
 *
 * @param message A human-readable description of the failure.
 * @param cause The underlying exception, if any.
 */
class DiscordIPCException(message: String, cause: Throwable? = null) : Exception(message, cause)
