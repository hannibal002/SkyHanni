package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.misc.discordrpc.DiscordRPCManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RichPresenceCommand {

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        val packet = event.packet

        if (!SkyHanniMod.feature.misc.discordRPC.enabled.get()) return

        if (packet is C01PacketChatMessage) {
            val message = packet.message.lowercase()
            if (message == "/rpcstart") {
                event.isCanceled = true
                LorenzUtils.chat("§e[SkyHanni] Attempting to start Discord Rich Presence...")
                val discordRPCManager =
                    SkyHanniMod.modules.firstOrNull { it is DiscordRPCManager } as? DiscordRPCManager
                discordRPCManager?.start(true)
                    ?: run { LorenzUtils.chat("§c[SkyHanni] Unable to start Discord Rich Presence! Please report this on Discord and ping NetheriteMiner#6267.") }
            }
        }
    }
}