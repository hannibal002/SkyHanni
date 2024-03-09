package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.features.commands.WikiManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
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

        val chestName = InventoryUtils.openInventoryName()

        if (chestName.isEmpty()) return

        val itemClickedStack = event.slot.stack ?: return
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
            event.isCanceled = true
        }
    }

    private fun isEnabled() = config.enabled
}
