package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchMatchers
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object XPInInventories {
    private val config get() = HanniMod.feature.misc

    /**
     * REGEX-TEST: §310 Exp Levels
     * REGEX-TEST: §310 XP Levels
     * REGEX-TEST:§7Starting cost: §b350 XP Levels
     */
    private val xpLevelsPattern by RepoPattern.list(
        "misc.xp-in-inventory.exp-levels",
        "(?:§.)*(?<xp>\\d+) (Exp|XP) Levels",
        "(?:§.)*Starting cost: §b(?<xp>\\d+) XP Levels",
    )

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        var requiredXP = 0
        val indexOfCost = event.toolTip.indexOfFirst {
            xpLevelsPattern.matchMatchers(it) {
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
