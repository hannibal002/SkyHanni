package at.hannibal2.skyhanni.features.garden.farming

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.isFishingRod
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import com.google.gson.JsonArray

@SkyHanniModule
object NoBreak {

    private val SPRAYONATOR_ITEM = "SPRAYONATOR".toInternalName()

    private val config get() = GardenApi.config

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType != InteractClickType.LEFT_CLICK) return
        // TODO make this work with CropClickEvent
        if (event.getCropType() == null) return

        val heldItem = InventoryUtils.itemInHandId
        if (config.noBreakItems.any { it.predicate(heldItem) }) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(136, "garden.noRodBreak", "garden.noBreakItems") { element ->
            if (element.asBoolean) {
                ConfigManager.gson.toJsonTree(NoBreakItem.entries)
            } else {
                JsonArray()
            }
        }
    }

    enum class NoBreakItem(
        val displayName: String,
        val predicate: (NeuInternalName) -> Boolean,
    ) {
        FISHING_ROD("Fishing Rod", { it.isFishingRod() }),
        SPRAYONATOR("Sprayonator", { it == SPRAYONATOR_ITEM }),
        ;

        override fun toString() = displayName
    }
}
