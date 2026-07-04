package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket

@SkyHanniModule
object BlockData {

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
    fun onBlockReceivePacket(event: PacketReceivedEvent) {
        // The server can send us block update packets while the [ClientLevel] doesn't exist,
        // apparently
        if (!MinecraftCompat.localWorldExists) return

        if (event.packet is ClientboundBlockUpdatePacket) {
            val blockPos = event.packet.pos ?: return
            val blockState = event.packet.blockState ?: return
            ServerBlockChangeEvent(blockPos, blockState).post()
        } else if (event.packet is ClientboundSectionBlocksUpdatePacket) {
            event.packet.runUpdates { pos, state ->
                ServerBlockChangeEvent(pos, state).post()
            }
        }
    }
}
