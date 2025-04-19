package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.TabListLineRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage

@SkyHanniModule
object DungeonRankTabListColor {

    private val config get() = SkyHanniMod.feature.dungeon.tabList

    @HandleEvent
    fun onTabListText(event: TabListLineRenderEvent) {
        if (!isEnabled()) return

        DungeonApi.playerDungeonTeamPattern.matchMatcher(event.text.stripHypixelMessage()) {
            val className = groupOrNull("className") ?: return
            val classLevel = groupOrNull("classLevel") ?: return

            val sbLevel = group("sbLevel")
            val rank = groupOrEmpty("rank")
            val playerName = group("playerName")
            // val symbols = group("symbols")
            val cleanName = playerName.cleanPlayerName(true)
            val color = DungeonApi.getColor(classLevel.romanToDecimalIfNecessary())

            event.text = "§8$sbLevel $rank$cleanName §f(§d$className $color$classLevel§f)"
        }
    }

    fun isEnabled() = DungeonApi.inDungeon() && config.coloredClassLevel
}
