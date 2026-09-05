package at.hannibal2.skyhanni.features.fishing.trophyfrog

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyTotalCaught
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object RiberyTotalCaught : TrophyTotalCaught() {

    private val config get() = SkyHanniMod.feature.fishing.trophyFrogs

    private val patternGroup = RepoPattern.group("fishing.trophy.ribery")

    /**
     * REGEX-TEST: Discovered
     */
    override val discoveredPattern by patternGroup.pattern(
        "discovered",
        "Discovered",
    )

    /**
     * REGEX-TEST: Bronze ✔ (615)
     * REGEX-TEST: Bronze ✖
     */
    override val bronzePattern by patternGroup.pattern(
        "bronze",
        "^Bronze.*",
    )

    override fun isInInventory() = TrophyFrogManager.riberyInventory.isInside()
    override fun isEnabled() = config.totalFrogsCaught
    override fun countsFor(cleanName: String) = TrophyFrogManager.frog?.get(cleanName)

    @HandleEvent(onlyOnSkyblock = true)
    private fun onToolTipEvent(event: ToolTipTextEvent) {
        handleToolTip(event)
    }
}
