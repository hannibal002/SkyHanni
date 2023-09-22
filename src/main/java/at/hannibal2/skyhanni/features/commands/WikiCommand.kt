package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WikiCommand {

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        val packet = event.packet

        if (!SkyHanniMod.feature.commands.useFandomWiki) return

        if (packet is C01PacketChatMessage) {
            val message = packet.message.lowercase()
            if (message == "/wiki" || message == "/shwiki") {
                event.isCanceled = true
                OSUtils.openBrowser("https://hypixel-skyblock.fandom.com/wiki/Hypixel_SkyBlock_Wiki")
                LorenzUtils.chat("§e[SkyHanni] Opening the Fandom Wiki..")
            } else if (message.startsWith("/wiki ") || message.startsWith("/shwiki ")) {
                event.isCanceled = true
                val search = packet.message.substring(6).replace("i ", "")
                LorenzUtils.chat("§e[SkyHanni] Searching the Fandom Wiki for §c$search")
                val url = "https://hypixel-skyblock.fandom.com/wiki/Special:Search?query=$search&scope=internal"
                OSUtils.openBrowser(url.replace(' ', '+'))
            }
        }
    }
}