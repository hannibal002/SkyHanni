package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.isFishingRod
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils

@SkyHanniModule
object NoRodBreak {

    private val config get() = GardenApi.config

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        if (!config.noRodBreak) return
        if (event.clickType != ClickType.LEFT_CLICK) return

        if (InventoryUtils.getItemInHand()?.isFishingRod() == true) {
            event.cancel()
        }
    }
}
