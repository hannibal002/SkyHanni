package at.hannibal2.skyhanni.features.misc.discordrpc

import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * A lightweight Discord IPC client implementing the Rich Presence protocol.
 *
 * Manages handshake, presence updates, and clean disconnection over a [DiscordIPCPipe]
 * obtained from [DiscordIPCPipeManager].
 *
 * @param clientId The Discord application client ID (from the Discord Developer Portal).
 * @param onDebugInfo Called with diagnostic key-value pairs if pipe discovery fails.
 */
class DiscordIPC(
    private val clientId: Long,
    private val onDebugInfo: (Map<String, String>) -> Unit = {},
) : Closeable {

    @Volatile
    private var _connected = false
    private var pipe: DiscordIPCPipe? = null

    private val gson = com.google.gson.GsonBuilder().create()

    /** Whether this client is currently connected and ready for Discord IPC. */
    val isConnected: Boolean get() = _connected
    private val clientPayload = """{"v":1,"client_id":"$clientId"}"""

    /**
     * Opens a pipe to Discord and performs the version-1 handshake.
     *
     * @throws DiscordIPCException If no Discord client is running, the pipe cannot be opened,
     *   or the handshake does not complete successfully.
     */
    fun connect() {
        pipe = DiscordIPCPipeManager.open(onDebugInfo)
        sendFrame(Opcode.HANDSHAKE, clientPayload)
        val (opcode, body) = readFrame()
        if (opcode != Opcode.FRAME) throw DiscordIPCException("Expected FRAME after handshake, got $opcode. Body: $body")
        _connected = true
    }

    /**
     * Updates the rich presence activity displayed on the user's Discord profile.
     *
     * @param presence The [DiscordRichPresence] to send. Null fields are omitted from the payload.
     * @throws DiscordIPCException If the client is not connected or writing to the pipe fails.
     */
    fun setActivity(presence: DiscordRichPresence) {
        if (!_connected) throw DiscordIPCException("setActivity called while not connected")
        sendFrame(Opcode.FRAME, gson.toJson(buildActivityPayload(presence)))
    }

    /**
     * Sends a CLOSE frame to Discord and releases all pipe resources.
     * Safe to call when not connected; errors during the close frame are silently swallowed.
     */
    override fun close() {
        if (_connected) runCatching { sendFrame(Opcode.CLOSE, clientPayload) }
        _connected = false
        pipe?.close()
        pipe = null
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
     * Writes a single framed IPC message to the pipe.
     *
     * Discord's IPC wire format: `[opcode: Int32LE][length: Int32LE][payload: UTF-8 bytes]`.
     *
     * Synchronized to guard against concurrent writes from the presence loop and [close].
     *
     * @throws DiscordIPCException If there is no active pipe connection.
     */
    @Synchronized
    private fun sendFrame(opcode: Opcode, json: String) {
        val out = pipe?.output ?: throw DiscordIPCException("sendFrame called with no active connection")
        val bytes = json.toByteArray(Charsets.UTF_8)
        val frame = ByteBuffer.allocate(8 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(opcode.id)
            .putInt(bytes.size)
            .put(bytes)
        out.write(frame.array())
        out.flush()
    }

    /**
     * Reads one framed IPC message from the pipe. Blocks until a full frame is available.
     *
     * Sets [isConnected] to false and throws if Discord closes the pipe mid-read.
     *
     * @return A pair of the received [Opcode] and its decoded JSON payload.
     * @throws DiscordIPCException If the connection is closed or an unrecognized opcode is received.
     */
    @Suppress("ThrowsCount")
    private fun readFrame(): Pair<Opcode, String> {
        val inp = pipe?.input ?: throw DiscordIPCException("readFrame called with no active connection")
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
     * Payload structure: `{ cmd, args: { pid, activity: { ... } }, nonce }`.
     */
    private fun buildActivityPayload(presence: DiscordRichPresence) = buildActivityPayload(
        presence,
        ProcessHandle.current().pid().toInt(),
        UUID.randomUUID().toString(),
    )
}

