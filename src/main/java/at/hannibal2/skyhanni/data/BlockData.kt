package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket

@SkyHanniModule
object BlockData {

    @HandleEvent(priority = HandleEvent.LOW, receiveCancelled = true)
    fun onBlockReceivePacket(event: PacketReceivedEvent) {
        if (event.packet is BlockUpdateS2CPacket) {
            val blockPos = event.packet.pos ?: return
            val blockState = event.packet.state ?: return
            ServerBlockChangeEvent(blockPos, blockState).post()
        } else if (event.packet is ChunkDeltaUpdateS2CPacket) {
            //#if MC < 1.21
            //$$ for (block in event.packet.changedBlocks) {
            //$$     ServerBlockChangeEvent(block.pos, block.blockState).post()
            //$$ }
            //#else
            event.packet.visitUpdates { pos, state ->
                ServerBlockChangeEvent(pos, state).post()
            }
            //#endif
        }
    }
}
