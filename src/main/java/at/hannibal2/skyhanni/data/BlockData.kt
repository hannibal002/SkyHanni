package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket

@SkyHanniModule
object BlockData {

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
    fun onBlockReceivePacket(event: PacketReceivedEvent) {
        if (event.packet is ClientboundBlockUpdatePacket) {
            val blockPos = event.packet.pos ?: return
            val blockState = event.packet.blockState ?: return
            ServerBlockChangeEvent(blockPos, blockState).post()
        } else if (event.packet is ClientboundSectionBlocksUpdatePacket) {
            //#if MC < 1.21
            //$$ for (block in event.packet.changedBlocks) {
            //$$     ServerBlockChangeEvent(block.pos, block.blockState).post()
            //$$ }
            //#else
            event.packet.runUpdates { pos, state ->
                ServerBlockChangeEvent(pos, state).post()
            }
            //#endif
        }
    }
}
