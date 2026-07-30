package at.hannibal2.skyhanni.features.inventory.museum

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BitsApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object MuseumCategoryHighlighter {

    private val museumDetector = InventoryDetector { BitsApi.museumGuiNamePattern }

    private val allItemsDonatedPattern by RepoPattern.pattern(
        "museum.all-items-donated",
        "Items Donated: 100%"
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SkyHanniMod.feature.inventory.museumCategoryHighlight) return
        if (!museumDetector.isInside()) return
        for (slot in event.container.slots) {
            val lore = slot.item.getCleanLore()
            allItemsDonatedPattern.firstMatcher(lore) {
                slot.highlight(LorenzColor.GREEN)
            }
        }
    }
}
