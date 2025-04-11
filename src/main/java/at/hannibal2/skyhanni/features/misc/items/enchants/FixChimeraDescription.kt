package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FixChimeraDescription {
    private val patternGroup = RepoPattern.group("data.collection.api")

    /**
     * REGEX-TEST: Copies §a60% §7of your active
     * REGEX-TEST: Copies §a80% §7of your active
     */
    private val percentagePattern by patternGroup.pattern(
        "fix.chimera-description",
        ".*Copies §a(?<percentage>.*)% §7of your active.*",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ItemHoverEvent) {
        // We don't need to always fix this
        if (!LorenzUtils.isAprilFoolsDay) return

        for ((index, line) in event.toolTip.withIndex()) {
            // hypixel doesn't show the 100% for chimera 5
            if (line.contains("Copies your active pet's stats.")) {
                event.toolTip[index] = line.replace("Copies your active pet's stats.", "Copies §a75% §7of your active pet's stats.")
            }
            percentagePattern.matchMatcher(line) {
                group("percentage").formatIntOrNull()?.let { old ->
                    val new = newChimeraValue(old)
                    event.toolTip[index] = line.replace("$old", "$new")
                }
            }
        }
    }

    private fun newChimeraValue(old: Int): Int = when (old) {
        100 -> 75
        80 -> 60
        60 -> 45
        40 -> 25
        20 -> 10
        else -> old
    }
}
