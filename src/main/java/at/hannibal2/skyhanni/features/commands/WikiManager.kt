package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WikiManager {

    private val config get() = SkyHanniMod.feature.commands.fandomWiki

    companion object {
        private val urlPrefix = "https://hypixel-skyblock.fandom.com/wiki/"
        val urlSearchPrefix = "${urlPrefix}Special:Search?query="
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "commands.useFandomWiki", "commands.fandomWiki.enabled")
    }

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return

        val packet = event.packet

        if (packet is C01PacketChatMessage) {
            val message = packet.message.lowercase()
            if (!(message.startsWith("/wiki"))) return
            event.isCanceled = true
            if (message == "/wiki") {
                LorenzUtils.chat("Opening the Fandom Wiki..")
                OSUtils.openBrowser("${urlPrefix}Hypixel_SkyBlock_Wiki")
            } else if (message.startsWith("/wiki ") || message == ("/wikithis")) { //conditional to see if we need Special:Search page
                if (message == ("/wikithis")) {
                    val itemInHand = InventoryUtils.getItemInHand() ?: return
                    wikiTheItem(itemInHand)
                } else {
                    val search = packet.message.split("/wiki ").last()
                    LorenzUtils.chat("Searching the Fandom Wiki for §a$search")
                    val wikiUrlCustom = "$urlSearchPrefix$search&scope=internal"
                    OSUtils.openBrowser(wikiUrlCustom.replace(' ', '+'))
                }
            }
        }
    }

    @SubscribeEvent
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
        if (!LorenzUtils.inSkyBlock) return
        val gui = event.gui as? GuiContainer ?: return
        if (NEUItems.neuHasFocus()) return //because good heavens if this worked on neuitems...
        val stack = gui.slotUnderMouse?.stack ?: return

        if (!config.fandomWikiKeybind.isKeyHeld()) return
        wikiTheItem(stack)
    }

    private fun wikiTheItem(item: ItemStack) {
        val itemDisplayName = (item.nameWithEnchantment ?: return).replace("§a✔ ", "").replace("§c✖ ", "")
        val internalName = item.getInternalName().asString()
        LorenzUtils.chat("Searching the Fandom Wiki for §a$itemDisplayName")
        val wikiUrlSearch = if (internalName != "NONE") "$urlSearchPrefix$internalName&scope=internal"
        else "$urlSearchPrefix${itemDisplayName.removeColor()}&scope=internal"
        OSUtils.openBrowser(wikiUrlSearch.replace(' ', '+'))
    }

    private fun isEnabled() = config.enabled
}
