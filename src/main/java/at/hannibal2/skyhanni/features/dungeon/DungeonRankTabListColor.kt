package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.TabListLineRenderEvent
import at.hannibal2.skyhanni.utils.LorenzUtils.groupOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonRankTabListColor {

    private val config get() = SkyHanniMod.feature.dungeon.tabList
    private val patternGroup = RepoPattern.group("dungeon.tablist")

    /**
     * REGEX-TEST: §8[§r§9319§r§8] §r§bEmpa_ §r§7α §r§f(§r§dMage XXXIV§r§f)
     */
    private val pattern by patternGroup.pattern(
        "rank",
        "^(?:§.)*(?<sbLevel>\\[(?:§.)*\\d+(?:§.)*]) (?<rank>(?:§.)*\\[(?:§.)*[^]]+(?:§.)*])? ?(?<playerName>\\S+) (?<symbols>[^(]*)\\((?:§.)*(?<className>\\S+) (?<classLevel>[CLXVI]+)(?:§.)*\\)(?:§.)*$"
    )

    @SubscribeEvent
    fun onTabListText(event: TabListLineRenderEvent) {
        if (!isEnabled()) return

        val (sbLevel, rank, playerName, className, classLevel) = pattern.matchMatcher(event.text.stripHypixelMessage()) {
            listOf(
                group("sbLevel"),
                groupOrNull("rank") ?: "",
                group("playerName"),
                group("className"),
                group("classLevel"),
            )
        } ?: return

        val cleanName = playerName.cleanPlayerName(true)
        val color = DungeonAPI.getColor(classLevel.romanToDecimalIfNecessary())

        event.text = "§8$sbLevel $rank$cleanName §f(§d$className $color$classLevel§f)"
    }

    fun isEnabled() = DungeonAPI.inDungeon() && config.coloredClassLevel
}
