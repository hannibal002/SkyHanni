package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class PlaySoundPacket
/**
 * @param soundId Sound id. may use namespace:id. default is minecraft so no:minecraft required.
 * Use [.streamFromUrl] to stream play an audio file from the given url.
 */(var soundId: String, var durationInSeconds: Int) : AbstractPacket() {
    var isStreamFromUrl: Boolean = false

    companion object {
        @JvmStatic
        fun streamFromUrl(url: String, durationInSeconds: Int): PlaySoundPacket {
            val packet = PlaySoundPacket(url, durationInSeconds)
            packet.isStreamFromUrl = true
            return packet
        }
    }
}
