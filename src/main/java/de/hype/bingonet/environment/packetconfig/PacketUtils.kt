package de.hype.bingonet.environment.packetconfig

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.BNConnection
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig.BNGson
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig.BNPacketManager
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig.Packet
import com.google.gson.Gson
import de.hype.bingonet.shared.packets.base.ExpectReplyPacket

object PacketUtils {
    val gson: Gson = BNGson.createNotPrettyPrinting()
    val knownPacketIssues: MutableSet<String> = HashSet<String>()

    fun parsePacketToJson(packet: AbstractPacket): String {
        return gson.toJson(packet).replace("\n", "/n")
    }

    fun parsePacket(message: String): Pair<Packet<out AbstractPacket>, AbstractPacket>? {
        if (!message.contains(".")) return null
        val packetName = message.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        val rawJson = message.substring(packetName.length + 1)
        for (packet in BNPacketManager.getPackets()) {
            if (packetName != packet.clazz.simpleName) continue
            return Pair(packet,gson.fromJson(rawJson.replace("/n", "\n"), packet.clazz))
        }
        return null
    }
}
