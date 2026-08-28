package at.hannibal2.skyhanni.features.fishing.trophyfrog

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object RiberyTotalCaught {

    private val config get() = SkyHanniMod.feature.fishing.trophyFrogs

    private val patternGroup = RepoPattern.group("fishing.trophy.ribery")

    /**
     * REGEX-TEST: Discovered
     */
    private val discoveredPattern by patternGroup.pattern(
        "discovered",
        "Discovered",
    )

    /**
     * REGEX-TEST: Bronze ✔ (615)
     * REGEX-TEST: Bronze ✖
     */
    private val bronzePattern by patternGroup.pattern(
        "bronze",
        "^Bronze.*",
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onToolTipEvent(event: ToolTipTextEvent) {
        if (!TrophyFrogManager.riberyInventory.isInside()) return
        if (!config.totalFrogsCaught) return

        if (event.toolTip.none { discoveredPattern.matcher(it.string).find() }) return

        val counts = TrophyFrogManager.frog?.get(event.itemStack.cleanName) ?: return
        val bestObtained = counts.filter { it.value > 0 }.keys.maxOrNull() ?: TrophyRarity.BRONZE
        val bronzeLineIndex = event.toolTip.indexOfFirst { bronzePattern.matcher(it.string).find() }

        if (bronzeLineIndex > 0) {
            event.toolTip.add(bronzeLineIndex + 1, "")
            event.toolTip.add(
                bronzeLineIndex + 2,
                "§7Total: ${bestObtained.formatCode}${counts.values.sum().addSeparators()}",
            )
        }
    }
}
