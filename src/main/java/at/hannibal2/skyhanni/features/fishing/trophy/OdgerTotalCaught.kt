package at.hannibal2.skyhanni.features.fishing.trophy

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object OdgerTotalCaught {

    private val config get() = SkyHanniMod.feature.fishing.trophyFishing
    private val patternGroup = RepoPattern.group("fishing.trophy.odger")

    /**
     * REGEX-TEST: §aDiscovered
     */
    private val discoveredPattern by patternGroup.pattern(
        "discovered",
        "§aDiscovered",
    )

    /**
     * REGEX-TEST: §8Bronze §c✖
     * REGEX-TEST: §8Bronze §a✔§7 (4)
     * REGEX-TEST: §5§o§8Bronze §a✔§7 (4)
     */
    private val bronzePattern by patternGroup.pattern(
        "bronze",
        "^(?:§5§o)?§8Bronze.*",
    )

    private val odgerInventory = InventoryDetector { name -> name == "Trophy Fishing" }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onToolTipEvent(event: ToolTipEvent) {
        if (!odgerInventory.isInside()) return
        if (!config.totalFishCaught) return

        if (event.toolTip.none { discoveredPattern.matcher(it).find() }) return

        val trophyFishKey = TrophyFishApi.getInternalName(event.itemStack.displayName)

        val counts = TrophyFishManager.fish?.get(trophyFishKey) ?: return
        val bestFishObtained = counts.filter { it.value > 0 }.keys.maxOrNull() ?: TrophyRarity.BRONZE
        val bronzeLineIndex = event.toolTip.indexOfFirst { bronzePattern.matcher(it).find() }

        if (bronzeLineIndex > 0) {
            event.toolTip.add(bronzeLineIndex + 1, "")
            event.toolTip.add(
                bronzeLineIndex + 2,
                "§7Total: ${bestFishObtained.formatCode}${counts.values.sum().addSeparators()}",
            )
        }
    }
}
