package at.hannibal2.skyhanni.api.hypixelapi

import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelAPIServerChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import kotlin.jvm.optionals.getOrNull

@SkyHanniModule
object HypixelEventAPI {

    init {
        val modApi = HypixelModAPI.getInstance()
        modApi.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        modApi.createHandler(ClientboundLocationPacket::class.java, ::onLocationPacket)
    }

    private fun onLocationPacket(packet: ClientboundLocationPacket) {
        if (!HypixelLocationAPI.config) return
        HypixelAPIServerChangeEvent(
            packet.serverName,
            packet.serverType.getOrNull(),
            packet.lobbyName.getOrNull(),
            packet.mode.getOrNull(),
            packet.map.getOrNull(),
        ).post()
    }
}
