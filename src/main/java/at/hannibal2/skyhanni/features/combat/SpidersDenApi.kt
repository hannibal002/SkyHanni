package at.hannibal2.hanni.features.combat

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.ScoreboardData
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.anyMatches
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object SpidersDenApi {

    private val patternGroup = RepoPattern.group("combat.spidersden")

    /**
     * REGEX-TEST: §4Broodmother§7: §6Soon
     */
    val broodmotherPattern by patternGroup.pattern(
        "broodmother",
        "§4Broodmother§7: §[e64](?:Slain|Dormant|Soon|Awakening|Imminent|Alive!)",
    )

    fun inSpidersDen() = IslandType.SPIDER_DEN.isCurrent()

    fun isAtTopOfNest() = inSpidersDen() && broodmotherPattern.anyMatches(ScoreboardData.sidebarLinesFormatted)
}
