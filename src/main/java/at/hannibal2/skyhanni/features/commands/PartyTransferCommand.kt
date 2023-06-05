package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PartyTransferCommand {
    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!SkyHanniMod.feature.commands.usePartyTransferAlias) return

        val packet = event.packet
        if (packet is C01PacketChatMessage) {
            val pattern = "/pt (?<args>.*)".toPattern()
            pattern.matchMatcher(packet.message.lowercase()) {
                event.isCanceled = true
                val args = group("args")
                LorenzUtils.sendCommandToServer("party transfer $args")
            }
        }
    }

}