package at.hannibal2.hanni.features.fishing.trophy

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryDetector
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object OdgerTotalCaught {

    private val config get() = HanniMod.feature.fishing.trophyFishing
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
