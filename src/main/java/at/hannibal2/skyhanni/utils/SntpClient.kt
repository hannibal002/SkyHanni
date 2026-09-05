package at.hannibal2.skyhanni.utils

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Minimal SNTP client (RFC 4330) that queries an NTP server for the offset between its clock and the local one.
 */
object SntpClient {
    private const val NTP_PORT = 123
    private const val PACKET_SIZE = 48
    private const val NTP_VERSION = 3
    private const val MODE_CLIENT = 3
    private const val MODE_SERVER = 4

    /** Seconds between the NTP epoch (1900-01-01) and the unix epoch (1970-01-01). */
    private const val EPOCH_OFFSET_SECONDS = 2_208_988_800L

    private const val REFERENCE_IDENTIFIER_INDEX = 12
    private const val KISS_CODE_LENGTH = 4
    private const val ORIGINATE_TIMESTAMP_INDEX = 24
    private const val RECEIVE_TIMESTAMP_INDEX = 32
    private const val TRANSMIT_TIMESTAMP_INDEX = 40

    private val TIMEOUT = 10.seconds

    /**
     * Thrown when the server answers with a kiss-of-death packet, asking the client to stop querying or to back off.
     *
     * @param code the kiss code sent by the server, e.g. `RATE`, `DENY` or `RSTR`
     */
    class KissOfDeathException(val code: String) : IOException("NTP server sent a kiss-of-death ($code)")

    /**
     * Queries [server] and returns how far the local clock is behind the server clock.
     *
     * @throws java.net.UnknownHostException if [server] can not be resolved
     * @throws java.net.SocketTimeoutException if the server does not respond in time
     * @throws KissOfDeathException if the server asks the client to stop or slow down
     * @throws IOException if the response is not a valid NTP server reply
     */
    @Suppress("ThrowsCount")
    fun getOffset(server: String): Duration {
        val address = InetAddress.getByName(server)
        val buffer = ByteArray(PACKET_SIZE)
        buffer[0] = ((NTP_VERSION shl 3) or MODE_CLIENT).toByte()

        DatagramSocket().use { socket ->
            socket.soTimeout = TIMEOUT.inWholeMilliseconds.toInt()

            val originateTime = System.currentTimeMillis()
            buffer.writeTimestamp(TRANSMIT_TIMESTAMP_INDEX, originateTime)
            val request = buffer.copyOfRange(TRANSMIT_TIMESTAMP_INDEX, PACKET_SIZE)
            socket.send(DatagramPacket(buffer, buffer.size, address, NTP_PORT))

            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            val destinationTime = System.currentTimeMillis()

            if (response.length < PACKET_SIZE) throw IOException("NTP response is too short (${response.length} bytes)")
            val mode = buffer[0].toInt() and 0x7
            if (mode != MODE_SERVER) throw IOException("NTP response has unexpected mode $mode")
            val stratum = buffer[1].toInt() and 0xFF
            if (stratum == 0) throw KissOfDeathException(buffer.readKissCode())
            val originate = buffer.copyOfRange(ORIGINATE_TIMESTAMP_INDEX, RECEIVE_TIMESTAMP_INDEX)
            if (!originate.contentEquals(request)) throw IOException("NTP response does not match the request")

            val receiveTime = buffer.readTimestamp(RECEIVE_TIMESTAMP_INDEX)
            val transmitTime = buffer.readTimestamp(TRANSMIT_TIMESTAMP_INDEX)
            return (((receiveTime - originateTime) + (transmitTime - destinationTime)) / 2).milliseconds
        }
    }

    private fun ByteArray.readKissCode(): String =
        String(this, REFERENCE_IDENTIFIER_INDEX, KISS_CODE_LENGTH, Charsets.US_ASCII)
            .filter { it.isLetterOrDigit() }
            .ifEmpty { "unknown" }

    private fun ByteArray.writeTimestamp(index: Int, millis: Long) {
        val seconds = millis / 1000 + EPOCH_OFFSET_SECONDS
        val fraction = (millis % 1000) * (1L shl 32) / 1000
        for (i in 0 until 4) {
            this[index + i] = (seconds shr (24 - i * 8)).toByte()
            this[index + 4 + i] = (fraction shr (24 - i * 8)).toByte()
        }
    }

    private fun ByteArray.readTimestamp(index: Int): Long {
        var seconds = 0L
        var fraction = 0L
        for (i in 0 until 4) {
            seconds = (seconds shl 8) or (this[index + i].toLong() and 0xFF)
            fraction = (fraction shl 8) or (this[index + 4 + i].toLong() and 0xFF)
        }
        return (seconds - EPOCH_OFFSET_SECONDS) * 1000 + fraction * 1000 / (1L shl 32)
    }
}
