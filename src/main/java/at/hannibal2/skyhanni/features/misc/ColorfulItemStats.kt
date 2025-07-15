package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.item.ItemHoverEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.replace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object ColorfulItemStats {
    private val config get() = SkyHanniMod.feature.misc.colorfulItemTooltips

    private val group = RepoPattern.group("misc.itemstats")

    /**
     * REGEX-TEST: §7Crit Chance: §c+30%
     * REGEX-TEST: §7Magic Find: §a+54.52
     */
    private val genericStat by group.pattern(
        "generic",
        "§7(?<stat>[a-zA-Z ]+): (?<oldColor>§[0-9a-f])(?<bonus>[-+]?[\\d.,%]+)",
    )

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ItemHoverEvent) {
        if (!config.enabled) return

        for ((index, line) in event.toolTip.withIndex()) {
            event.toolTip[index] = genericStat.replace(line) {

                val stat = group("stat")
                val oldColor = group("oldColor")

                val skyblockStat = SkyblockStat.getValueOrNull(
                    stat.uppercase().replace(" ", "_")
                )

                val bonusGroup = group("bonus")
                val bonus = if (config.replacePercentages && config.statIcons) bonusGroup.removeSuffix("%") else bonusGroup

                buildString {
                    append("§7$stat: ")
                    append(
                        SkyblockStat.getValueOrNull(
                            stat.uppercase().replace(" ", "_")
                        )?.icon?.take(2) ?: oldColor
                    )
                    append(skyblockStat?.icon?.take(2) ?: oldColor)
                    append(bonus)
                    if (config.statIcons) {
                        skyblockStat?.icon?.lastOrNull()?.let { append(it) }
                    }
                    append(oldColor)
                }
            }
        }
    }
}
