package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object BetterWikiFromMenus {

    private val config get() = SkyHanniMod.feature.misc.commands.betterWiki

    /**
     * REGEX-TEST: Click to view on the SkyBlock Wiki!
     */
    private val clickToViewOnWikiPattern by RepoPattern.pattern(
        "misc.better-wiki-from-menus.click-to-view-on-wiki",
        "Click to view on the SkyBlock Wiki!"
    )

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "fandomWiki", "commands.fandomWiki")
        // Apparently the above got changed again at some point but never got a migration
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return

        val chestName = InventoryUtils.openInventoryName()

        if (chestName.isEmpty()) return

        val itemClickedStack = event.slot?.item ?: return
        val itemClickedName = itemClickedStack.hoverName.string

        val isWiki = event.slotId == 11 && itemClickedName.contains("Wiki Command")
        val isWikithis = event.slotId == 15 && itemClickedName.contains("Wikithis Command")
        val inBiblioInventory = chestName == "SkyBlock Wiki" && (isWiki || isWikithis)
        val inSBGuideInventory = clickToViewOnWikiPattern.anyMatches(itemClickedStack.getCleanLore())

        if (inBiblioInventory) {
            if (isWiki) {
                WikiManager.sendWikiMessage(autoOpen = config.menuOpenWiki)
                return
            }

            WikiManager.wikiThisItem(autoOpen = config.menuOpenWiki)
            return
        }

        if (inSBGuideInventory && config.skyblockGuide) {
            val wikiSearch = itemClickedName.removeColor().replace("✔ ", "").replace("✖ ", "")
            WikiManager.sendWikiMessage(wikiSearch, autoOpen = config.menuOpenWiki)
            event.cancel()
        }
    }

    private fun isEnabled() = config.enabled
}
