package at.hannibal2.skyhanni.features.inventory.experimentationtable.superpairs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ExperimentationSuperpairApi
import at.hannibal2.skyhanni.api.ExperimentationTableApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets

@SkyHanniModule
object SuperPairsItemVisibility {

    private val config get() = SkyHanniMod.feature.inventory.experimentationTable.superpairs.clickedItemsVisible

    @HandleEvent(onlyOnIsland = IslandType.PRIVATE_ISLAND)
    fun onReplaceItem(event: ReplaceItemEvent) {
        if (!config.enabled) return
        if (!ExperimentationTableApi.inSuperpairs) return
        val replacementItem = ExperimentationSuperpairApi.uncoveredItemStacks[event.slot] ?: return
        val originalName = event.originalItem.hoverName.formattedTextCompatLeadingWhiteLessResets()
        if (!ExperimentationSuperpairApi.unknownSuperpairsClickPattern.matches(originalName)) return
        event.replace(replacementItem)
    }
}
