package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.findMatcher
import at.hannibal2.skyhanni.utils.compat.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ColorfulItemStats {
    private val config get() = SkyHanniMod.feature.misc.colorfulItemTooltips

    private val group = RepoPattern.group("misc.itemstats")

    /**
     * REGEX-TEST: Crit Chance: +30%
     * REGEX-TEST: Magic Find: +54.52
     * REGEX-TEST: Rift Time: +60s
     * REGEX-TEST: Strength: +60 (+20) (+40) (+199.2)
     * REGEX-FAIL: Health: +1000❤
     */
    private val genericStat by group.pattern(
        "generic-stats-no-color",
        "(?<stat>[a-zA-Z ]+): (?<bonus>[-+]?[\\d.,%s]+)(?:\\s|$)",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ToolTipTextEvent) {
        if (!config.statIcons) return

        for ((index, line) in event.toolTip.withIndex()) {
            genericStat.findMatcher(line.string) {
                val stat = group("stat")
                val statId = stat.uppercase().replace(" ", "_")
                val skyblockStatIcon = SkyblockStat.getIconOrNull(statId) ?: return@findMatcher

                val bonusGroup = group("bonus")
                var bonus = when {
                    config.replacePercentages && bonusGroup.endsWith("%") -> bonusGroup.removeSuffix("%")
                    config.replaceRiftSeconds && bonusGroup.endsWith("s") -> bonusGroup.removeSuffix("s")
                    else -> bonusGroup
                }
                bonus += skyblockStatIcon.drop(2)

                val newComp = line.replace(group("bonus"), bonus, true) ?: return@findMatcher
                event.toolTip[index] = newComp
            }
        }
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(122, "misc.colorfulItemTooltips") { element ->
            val oldEnabled = element.asJsonObject.get("enabled").asBoolean
            if (!oldEnabled) {
                element.asJsonObject.remove("statIcons")
                element.asJsonObject.addProperty("statIcons", false)
            }
            element
        }
    }

}
