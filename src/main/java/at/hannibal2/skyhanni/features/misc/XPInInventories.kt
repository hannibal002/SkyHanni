package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.events.minecraft.add
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object XPInInventories {
    private val config get() = SkyHanniMod.feature.misc

    /**
     * REGEX-TEST: 0 Exp Levels
     * REGEX-TEST: 10 XP Levels
     * REGEX-TEST: Starting cost: 350 XP Levels
     */
    private val xpLevelsPattern by RepoPattern.list(
        "misc.xp-in-inventory.exp-levels.new",
        "(?<xp>\\d+) (Exp|XP) Levels",
        "Starting cost: (?<xp>\\d+) XP Levels",
    )

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return

        var requiredXP = 0
        val indexOfCost = event.toolTip.indexOfFirst {
            xpLevelsPattern.matchMatchers(it.string) {
                requiredXP = group("xp").toInt()
            } != null
        }
        if (indexOfCost == -1) return

        val playerXP = MinecraftCompat.localPlayer.experienceLevel
        val color = if (playerXP >= requiredXP) "§a" else "§c"
        event.toolTip.add(indexOfCost + 1, "§7Your XP: $color$playerXP")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.xpInInventory
}
