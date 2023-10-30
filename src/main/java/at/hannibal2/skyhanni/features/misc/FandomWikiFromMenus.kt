package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.anyContains
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FandomWikiFromMenus {

    private val config get() = SkyHanniMod.feature.commands.fandomWiki

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "fandomWiki", "commands.fandomWiki")
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return
        val chestName = InventoryUtils.openInventoryName()

        if (chestName.isEmpty()) return

        val itemClickedStack = event.slot.stack ?: return
        val itemClickedName = itemClickedStack.displayName
        val itemInHand = InventoryUtils.getItemInHand() ?: return
        val itemInHandName = itemInHand.nameWithEnchantment ?: return

        val wikiDisplayName: String
        val wikiInternalName: String

        val inWikiInventory = // TODO better name for this inventory
            event.slotId == 11 && itemClickedName.contains("Wiki Command") && chestName.contains("Wiki")
        if ((itemInHandName == "") || inWikiInventory) {
            LorenzUtils.clickableChat("Click here to visit the Hypixel Skyblock Fandom Wiki!", "wiki")
            return
        }

        val inOtherWikiInventory = // TODO better name for this inventory
            event.slotId == 15 && itemClickedName.contains("Wikithis Command") && chestName.contains("Wiki")
        if (inOtherWikiInventory) {
            wikiDisplayName = itemInHandName
            val internalName = itemInHand.getInternalName().asString()
            wikiInternalName = internalName
        } else {
            //.lowercase() to match "Wiki!" and ".*wiki.*" lore lines in one fell swoop
            val inThirdWikiInventory = // TODO better name for this inventory
                (itemClickedStack.getLore().anyContains("Wiki") || itemClickedStack.getLore().anyContains("wiki"))
                    && !itemClickedStack.getLore().anyContains("wikipedia")
            if (inThirdWikiInventory) {
                wikiDisplayName = itemClickedName.removeColor().replace("✔ ", "").replace("✖ ", "")
                wikiInternalName = wikiDisplayName
            } else return
        }

        if (!config.skipWikiChat) {
            LorenzUtils.clickableChat(
                "Click here to search for $wikiDisplayName §eon the Hypixel Skyblock Fandom Wiki!",
                "wiki $wikiInternalName"
            )
        } else {
            LorenzUtils.chat("Searching the Fandom Wiki for §a$wikiDisplayName")
            val wikiUrlCustom = "${WikiManager.urlSearchPrefix}$wikiInternalName&scope=internal"
            OSUtils.openBrowser(wikiUrlCustom.replace(' ', '+'))
        }
        event.isCanceled = true
    }

    private fun isEnabled() = config.enabled
}
