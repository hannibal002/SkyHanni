package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HotmData.Companion.inInventory
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object OdgerTotalCaught {

    private val config get() = SkyHanniMod.feature.fishing.trophyFishing
    private val patternGroup = RepoPattern.group("fishing.trophy.odger")

    private val inventoryPattern by patternGroup.pattern(
        "inventory",
        "Trophy Fishing"
    )

    private val discoveredPattern by patternGroup.pattern(
        "discovered",
        "§aDiscovered"
    )

    private var inventoryOpened = false

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!config.totalFishCaught) return
        inInventory = inventoryPattern.matches(event.inventoryName)
        if (!inInventory) return
        inventoryOpened = true
    }

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (!inInventory) return
        if (!config.totalFishCaught) return
        inInventory = false
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onToolTipEvent(event: ToolTipEvent) {
        if (!inInventory) return
        if (!config.totalFishCaught) return

        if (event.toolTip.none { discoveredPattern.matcher(it).find() }) return

        val trophyFishKey = event.itemStack.name
            .removeColor()
            .lowercase()
            .replace(" ", "")
            .replace(Regex("^obfuscated(\\d+)$"), "obfuscatedfish$1")

        val totalFishCaught = TrophyFishManager.fish?.get(trophyFishKey)?.values?.sum() ?: return

        event.toolTip[0] = "${event.toolTip[0]} §7($totalFishCaught)"
    }
}
