package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.ServerBlockChangeEvent
import at.hannibal2.hanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange

@HanniModule
object BlockData {

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
    fun onBlockReceivePacket(event: PacketReceivedEvent) {
        if (event.packet is S23PacketBlockChange) {
            val blockPos = event.packet.blockPosition ?: return
            val blockState = event.packet.blockState ?: return
            ServerBlockChangeEvent(blockPos, blockState).post()
        } else if (event.packet is S22PacketMultiBlockChange) {
            //#if MC < 1.21
            for (block in event.packet.changedBlocks) {
                ServerBlockChangeEvent(block.pos, block.blockState).post()
            }
            //#else
            //$$ event.packet.visitUpdates { pos, state ->
            //$$     ServerBlockChangeEvent(pos, state).post()
            //$$ }
            //#endif
        }
    }
}
