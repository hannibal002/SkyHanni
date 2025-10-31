package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.GuiContainerEvent
import at.hannibal2.hanni.features.commands.WikiManager
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object BetterWikiFromMenus {

    private val config get() = HanniMod.feature.misc.commands.betterWiki

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(6, "fandomWiki", "commands.fandomWiki")
    }

    @HandleEvent(priority = HandleEvent.HIGH, onlyOnSkyblock = true)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return

        val chestName = InventoryUtils.openInventoryName()

        if (chestName.isEmpty()) return

        val itemClickedStack = event.slot?.stack ?: return
        val itemClickedName = itemClickedStack.displayName

        val isWiki = event.slotId == 11 && itemClickedName.contains("Wiki Command")
        val isWikithis = event.slotId == 15 && itemClickedName.contains("Wikithis Command")
        val inBiblioInventory = chestName == "SkyBlock Wiki" && (isWiki || isWikithis)
        val inSBGuideInventory =
            (itemClickedStack.getLore().let { it.any { line -> line == "§7§eClick to view on the SkyBlock Wiki!" } })

        if (inBiblioInventory) {
            if (isWiki) {
                WikiManager.sendWikiMessage(useFandom = true)
                return
            }

            if (isWikithis) {
                WikiManager.otherWikiCommands(arrayOf(""), true, true)
                return
            }
        }

        if (inSBGuideInventory && config.sbGuide) {
            val wikiSearch = itemClickedName.removeColor().replace("✔ ", "").replace("✖ ", "")
            WikiManager.sendWikiMessage(wikiSearch, autoOpen = config.menuOpenWiki)
            event.cancel()
        }
    }

    private fun isEnabled() = config.enabled
}
