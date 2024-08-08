package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.hypixel.location.HypixelLocation
import at.hannibal2.skyhanni.events.modapi.HypixelHelloEvent
import at.hannibal2.skyhanni.events.modapi.HypixelLocationChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import kotlin.jvm.optionals.getOrNull

@SkyHanniModule
object ModAPI {
    private val api = HypixelModAPI.getInstance()

    init {
        api.createHandler(ClientboundHelloPacket::class.java) { packet ->
            HypixelHelloEvent(packet.environment).post()
        }

        api.createHandler(ClientboundLocationPacket::class.java) { packet ->
            val newLocation = HypixelLocation(
                packet.serverName,
                packet.serverType.getOrNull(),
                packet.lobbyName.getOrNull(),
                packet.mode.getOrNull(),
                packet.map.getOrNull()
            )
            HypixelLocationChangeEvent(newLocation).post()
        }
    }
}
