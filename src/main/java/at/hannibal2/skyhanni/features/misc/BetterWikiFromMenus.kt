package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class BetterWikiFromMenus {

    private val config get() = SkyHanniMod.feature.commands.betterWiki

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "fandomWiki", "commands.fandomWiki")
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onSlotClick(event: SlotClickEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!isEnabled()) return

        val urlSearchPrefix = if (config.useFandom) "https://hypixel-skyblock.fandom.com/wiki/Special:Search?query="
        else "https://wiki.hypixel.net/index.php?search="

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
            LorenzUtils.clickableLinkChat("Click here to visit the Hypixel Skyblock Fandom Wiki!", "https://hypixel-skyblock.fandom.com/wiki")
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
                (itemClickedStack.getLore()
                    .let { it.any { line -> line == "§7§eClick to view on the SkyBlock Wiki!" }})
            if (inThirdWikiInventory) {
                wikiDisplayName = itemClickedName.removeColor().replace("✔ ", "").replace("✖ ", "")
                wikiInternalName = wikiDisplayName
            } else return
        }

        if (!config.menuOpenWiki) {
            LorenzUtils.clickableChat(
                "Click here to search for $wikiDisplayName §eon the Hypixel Skyblock Fandom Wiki!",
                "wiki $wikiInternalName"
            )
        } else {
            val wikiUrlCustom = "${urlSearchPrefix}$wikiInternalName&scope=internal"
            LorenzUtils.clickableLinkChat("Click to search the wiki for §a$wikiDisplayName§e!",
                wikiUrlCustom.replace(' ', '+'),config.menuOpenWiki,"Search §a$wikiDisplayName§e on the wiki!")
        }
        event.isCanceled = true
    }

    private fun isEnabled() = config.enabled
}
