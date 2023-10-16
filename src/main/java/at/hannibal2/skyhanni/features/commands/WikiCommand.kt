package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WikiCommand {

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        val packet = event.packet
        val message = packet.message.lowercase()

        if (!SkyHanniMod.feature.commands.useFandomWiki || !(message.startsWith("/wiki"))) return

        if (packet is C01PacketChatMessage) {
            event.isCanceled = true
            if (message == "/wiki") {
                OSUtils.openBrowser("https://hypixel-skyblock.fandom.com/wiki/Hypixel_SkyBlock_Wiki")
                LorenzUtils.chat("§e[SkyHanni] Opening the Fandom Wiki..")
            } else if (message.startsWith("/wiki ") || message == ("/wikithis")) { //conditional to see if we need Special:Search page
                var url = "https://hypixel-skyblock.fandom.com/wiki/Special:Search?query="
                if (message == ("/wikithis")) {
                    val itemInHand = InventoryUtils.getItemInHand() ?: return
                    val itemInHandName = itemInHand.nameWithEnchantment ?: return
                    val internalName = itemInHand.getInternalName().asString() ?: return
                    LorenzUtils.chat("§e[SkyHanni] Searching the Fandom Wiki for §a$itemInHandName")
                    url = "$url$internalName&scope=internal"
                } else {
                    LorenzUtils.chat("§e[SkyHanni] Searching the Fandom Wiki for §a$search")
                    url = "$url$search&scope=internal"
                }
                OSUtils.openBrowser(url.replace(' ', '+'))
            }
        }
    }
}