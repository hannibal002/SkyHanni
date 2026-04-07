package at.hannibal2.skyhanni.features.misc.discordrpc

import at.hannibal2.skyhanni.utils.ChatUtils
import com.google.gson.JsonObject
import java.io.Closeable
import java.io.IOException
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

    // Because IPC is separate from the mod, we need our own registered hook
    private var shutdownHook: Thread? = null

    // We need to use our own GSON here, rather than the ConfigManager one,
    // as we need NON-null serialization, as discord does not play nice with nulls.
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
        ChatUtils.debug("Discord RPC: pipe opened, sending handshake")
        sendFrame(Opcode.HANDSHAKE, clientPayload)
        val (opcode, body) = readFrame()
        ChatUtils.debug("Discord RPC: handshake response opcode=$opcode")
        if (opcode != Opcode.FRAME) throw DiscordIPCException("Expected FRAME after handshake, got $opcode. Body: $body")
        validateReadyBody(body)
        _connected = true
        shutdownHook = Thread(::close, "discord-rpc-shutdown").also(Runtime.getRuntime()::addShutdownHook)
    }

    /**
     * Parses the handshake response body and verifies it contains a READY event.
     *
     * @param body The raw JSON string received from Discord after the handshake frame.
     * @throws DiscordIPCException If the body is not valid JSON, contains an ERROR event,
     *   or contains an unexpected event type.
     */
    @Suppress("ThrowsCount")
    private fun validateReadyBody(body: String) {
        val obj = runCatching { gson.fromJson(body, JsonObject::class.java) }.getOrNull()
            ?: throw DiscordIPCException("Handshake response was not valid JSON. Body: $body")
        when (val evt = obj.get("evt")?.asString) {
            "READY" -> return
            "ERROR" -> {
                val data = obj.getAsJsonObject("data")
                val code = data?.get("code")?.asInt
                val message = data?.get("message")?.asString ?: "unknown error"
                throw DiscordIPCException("Handshake rejected by Discord (error $code): $message")
            }
            else -> throw DiscordIPCException("Expected READY event after handshake, got evt=$evt. Body: $body")
        }
    }

    var lastActivityJson: String? = null
        private set

    var lastDiscordResponse: String? = null
        private set

    /**
     * Updates the rich presence activity displayed on the user's Discord profile.
     *
     * Writes the SET_ACTIVITY frame then reads Discord's response inline (sequentially),
     * handling any PING frames along the way. This avoids needing a concurrent reader loop,
     * which would deadlock on Windows where [java.io.RandomAccessFile] serializes all I/O
     * on a single synchronous named-pipe handle.
     *
     * @param presence The [DiscordRichPresence] to send. Null fields are omitted from the payload.
     * @throws DiscordIPCException If the client is not connected or writing to the pipe fails.
     */
    fun setActivity(presence: DiscordRichPresence) {
        if (!_connected) throw DiscordIPCException("setActivity called while not connected")
        val json = gson.toJson(buildActivityPayload(presence))
        lastActivityJson = json
        ChatUtils.debug("Discord RPC: sending SET_ACTIVITY (${json.length} bytes)")
        sendFrame(Opcode.FRAME, json)
        ChatUtils.debug("Discord RPC: SET_ACTIVITY write complete, reading response")
        readUntilFrame()
    }

    /**
     * Reads frames from Discord until a non-PING frame is received or the connection closes.
     *
     * PING frames are answered with PONG inline. A CLOSE frame sets [isConnected] to false.
     * Any other frame (e.g. the SET_ACTIVITY response) is stored in [lastDiscordResponse].
     */
    private fun readUntilFrame() {
        while (_connected) {
            val (opcode, body) = readFrame()
            when (opcode) {
                Opcode.PING -> sendFrame(Opcode.PONG, body)
                Opcode.CLOSE -> {
                    _connected = false
                    ChatUtils.debug("Discord RPC: CLOSE frame received: $body")
                    return
                }
                else -> {
                    lastDiscordResponse = body
                    ChatUtils.debug("Discord RPC: frame received (opcode=$opcode): $body")
                    return
                }
            }
        }
    }

    /**
     * Releases all pipe resources and marks the client as disconnected.
     *
     * Closes the underlying pipe directly rather than sending a CLOSE frame first.
     * This prevents a deadlock where [sendFrame] holds the lock while blocked
     * on a native I/O write (e.g. [java.io.RandomAccessFile.write] on a Windows named pipe),
     * and a shutdown or disconnect handler on another thread tries to acquire the same lock.
     *
     * Closing the pipe from outside the lock causes the blocked write to fail with an
     * [java.io.IOException], which releases the lock without needing to acquire it here.
     * The CLOSE frame itself is optional. Discord handles disconnects gracefully without it.
     */
    override fun close() {
        ChatUtils.debug("Discord RPC: close() called, was connected=$_connected")
        shutdownHook?.let { runCatching { Runtime.getRuntime().removeShutdownHook(it) } }
        shutdownHook = null
        _connected = false
        val oldPipe = pipe
        pipe = null
        runCatching { oldPipe?.close() }
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
     * @throws DiscordIPCException If there is no active pipe connection or the write fails.
     */
    @Synchronized
    private fun sendFrame(opcode: Opcode, json: String) {
        val out = pipe?.output ?: throw DiscordIPCException("sendFrame called with no active connection")
        val bytes = json.toByteArray(Charsets.UTF_8)
        val frame = ByteBuffer.allocate(8 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(opcode.id)
            .putInt(bytes.size)
            .put(bytes)
        try {
            out.write(frame.array())
            out.flush()
        } catch (e: IOException) {
            _connected = false
            throw DiscordIPCException("IPC write failed: ${e.message}", e)
        }
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
        try {
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
        } catch (e: IOException) {
            _connected = false
            throw DiscordIPCException("IPC read failed: ${e.message}", e)
        }
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
