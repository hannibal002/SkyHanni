package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.ServerBlockChangeEvent
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BlockData {

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet

        if (packet is S23PacketBlockChange) {
            ServerBlockChangeEvent(packet.blockPosition, packet.blockState).postAndCatch()
        } else if (packet is S22PacketMultiBlockChange) {
            for (block in packet.changedBlocks) {
                ServerBlockChangeEvent(block.pos, block.blockState).postAndCatch()
            }
        }
    }
}