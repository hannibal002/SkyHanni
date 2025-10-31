package at.hannibal2.hanni.features.dungeon

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.TabListLineRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.hanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.hanni.utils.RegexUtils.groupOrNull
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.hanni.utils.StringUtils.stripHypixelMessage

@HanniModule
object DungeonRankTabListColor {

    private val config get() = HanniMod.feature.dungeon.tabList

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
