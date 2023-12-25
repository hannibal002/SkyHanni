package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class WikiManager {

    private val config get() = SkyHanniMod.feature.commands.betterWiki

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "commands.useFandomWiki", "commands.fandomWiki.enabled")
    }

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return

        val urlPrefix = if (config.useFandom) "https://hypixel-skyblock.fandom.com/wiki/" else "https://wiki.hypixel.net/"
        val urlSearchPrefix = if (config.useFandom) "${urlPrefix}Special:Search?query=" else "${urlPrefix}index.php?search="

        val message = event.message.lowercase()
        if (!(message.startsWith("/wiki"))) return
        event.isCanceled = true
        if (message == "/wiki") {
            LorenzUtils.clickableLinkChat("Click to open the wiki!", urlPrefix,config.autoOpenWiki,"Open the wiki!")
        } else if (message.startsWith("/wiki ") || message == ("/wikithis")) { //conditional to see if we need Special:Search page
            if (message == ("/wikithis")) {
                val itemInHand = InventoryUtils.getItemInHand() ?: return
                wikiTheItem(itemInHand,config.autoOpenWiki)
            } else {
                val search = event.message.split("/wiki ").last()
                val wikiUrlCustom = "$urlSearchPrefix$search"
                LorenzUtils.clickableLinkChat("Click to search for §a${search}§e on the wiki!",
                    wikiUrlCustom.replace(' ', '+'),config.autoOpenWiki,"Search for §a$search§e on the wiki!")
            }
        }
    }

    @SubscribeEvent
    fun onKeybind(event: GuiScreenEvent.KeyboardInputEvent.Post) {
        if (!LorenzUtils.inSkyBlock) return
        val gui = event.gui as? GuiContainer ?: return
        if (NEUItems.neuHasFocus()) return //because good heavens if this worked on neuitems...
        val stack = gui.slotUnderMouse?.stack ?: return

        if (!config.wikiKeybind.isKeyHeld()) return
        wikiTheItem(stack,config.menuOpenWiki)
    }

    private fun wikiTheItem(item: ItemStack, autoOpen: Boolean) {
        val urlPrefix = if (config.useFandom) "https://hypixel-skyblock.fandom.com/wiki/" else "https://wiki.hypixel.net/"
        val urlSearchPrefix = if (config.useFandom) "${urlPrefix}Special:Search?query=" else "${urlPrefix}index.php?search="

        val itemDisplayName = (item.nameWithEnchantment ?: return).replace("§a✔ ", "").replace("§c✖ ", "")
        val internalName = item.getInternalName().asString()
        val wikiUrlSearch = if (internalName != "NONE") "$urlSearchPrefix$internalName"
        else "$urlSearchPrefix${itemDisplayName.removeColor()}"
        LorenzUtils.clickableLinkChat("Click to search for §a$itemDisplayName§e on the wiki!",
            wikiUrlSearch.replace(' ', '+'),autoOpen,"Search for §a$itemDisplayName§e on the wiki!")
    }

    private fun isEnabled() = config.enabled
}
