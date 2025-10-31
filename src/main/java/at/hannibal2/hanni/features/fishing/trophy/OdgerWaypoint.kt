package at.hannibal2.hanni.features.fishing.trophy

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.events.minecraft.HanniTickEvent
import at.hannibal2.hanni.features.fishing.FishingApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemCategory
import at.hannibal2.hanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled

@HanniModule
object OdgerWaypoint {

    private val config get() = HanniMod.feature.fishing.trophyFishing
    private val odgerLocation = LorenzVec(-373, 207, -808)

    private var trophyFishInInventory = false

    // todo change to onOwnInventoryChange rather than every tick
    @HandleEvent
    fun onTick(event: HanniTickEvent) {
        if (!isEnabled() || !event.isMod(10)) return
        trophyFishInInventory = InventoryUtils.getItemsInOwnInventory()
            .any { it.getItemCategoryOrNull() == ItemCategory.TROPHY_FISH }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return
        if (FishingApi.holdingLavaRod) return
        if (!trophyFishInInventory) return

        event.drawWaypointFilled(odgerLocation, LorenzColor.WHITE.toColor())
        event.drawDynamicText(odgerLocation, "Odger", 1.5)
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "fishing.odgerLocation", "fishing.trophyFishing.odgerLocation")
    }

    fun isEnabled() = IslandType.CRIMSON_ISLE.isCurrent() && config.odgerLocation
}
