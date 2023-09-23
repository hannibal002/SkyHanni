package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WarpIsCommand {

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!SkyHanniMod.feature.commands.replaceWarpIs) return

        val packet = event.packet
        if (packet is C01PacketChatMessage && packet.message.lowercase() == "/warp is") {
            event.isCanceled = true
            LorenzUtils.sendMessageToServer("/is")
        }
    }
}