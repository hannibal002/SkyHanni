package at.hannibal2.skyhanni.api.hypixelapi

import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiJoinEvent
import at.hannibal2.skyhanni.events.hypixel.modapi.HypixelApiServerChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.hypixel.data.region.Environment
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundVersionedPacket
import kotlin.jvm.optionals.getOrNull
//#if MC < 1.21
import at.hannibal2.skyhanni.features.misc.CurrentPing
//#endif

@SkyHanniModule
object HypixelEventApi {

    private val modApi: HypixelModAPI = HypixelModAPI.getInstance()

    init {
        modApi.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        modApi.createHandler(ClientboundHelloPacket::class.java, ::onHelloPacket)
        modApi.createHandler(ClientboundLocationPacket::class.java, ::onLocationPacket)
        //#if MC < 1.21
        modApi.createHandler(ClientboundPingPacket::class.java, CurrentPing::onPongPacket)
        //#endif
    }

    private fun onHelloPacket(packet: ClientboundHelloPacket) {
        if (!HypixelLocationApi.config) return
        val isAlpha = packet.environment != Environment.PRODUCTION
        HypixelApiJoinEvent(isAlpha).post()
    }

    private fun onLocationPacket(packet: ClientboundLocationPacket) {
        if (!HypixelLocationApi.config) return
        HypixelApiServerChangeEvent(
            packet.serverName,
            packet.serverType.getOrNull(),
            packet.lobbyName.getOrNull(),
            packet.mode.getOrNull(),
            packet.map.getOrNull(),
        ).post()
    }

    fun sendPacket(packet: ServerboundVersionedPacket) {
        // TODO cache the error, or investigate further.
        try {
            modApi.sendPacket(packet)
        } catch (_: Exception) {
        }
    }
}
