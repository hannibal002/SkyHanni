package at.hannibal2.skyhanni.data.hypixel

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.hypixelapi.HypixelLocationApi
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ClientDisconnectEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * This class handles the "am I in SkyBlock" and "what SkyBlock island am I on" checks.
 * For "Am I on hypixel" see [HypixelData].
 */
@SkyHanniModule
object SkyBlockLocationData {

    private val patternGroup = RepoPattern.group("data.skyblockstate")

    /**
     * REGEX-TEST: SKYBLOCK
     * REGEX-TEST: SKYBLOCK GUEST
     * REGEX-TEST: SKYBLOCK CO-OP
     * REGEX-TEST: SKYBLOCK ♲
     * REGEX-TEST: SKYBLOCK ☀
     * REGEX-TEST: SKYBLOCK Ⓑ
     */
    private val scoreboardTitlePattern by patternGroup.pattern(
        "scoreboard.title",
        "SK[YI]BLOCK(?: CO-OP| GUEST)?(?: [♲☀Ⓑ])?",
    )

    val inSkyBlock get() = HypixelLocationApi.inSkyblock
    val currentIsland get() = HypixelLocationApi.island

    private var scoreboardShowsSkyBlock = false
    private var scoreboardTitle: String? = null

    @HandleEvent
    fun onWorldChange() {
        scoreboardTitle = null
        scoreboardShowsSkyBlock = false
    }

    @HandleEvent(ClientDisconnectEvent::class)
    fun onDisconnect() {
        scoreboardTitle = null
        scoreboardShowsSkyBlock = false
    }

    @HandleEvent(ScoreboardUpdateEvent::class)
    fun onScoreboardUpdate() {
        scoreboardTitle = HypixelData.getScoreboardTitle()?.removeColor()
        scoreboardShowsSkyBlock = scoreboardTitle?.let { scoreboardTitlePattern.matches(it) } ?: false
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("SkyBlock Location Data")
        val list = listOf(
            "onHypixel: ${HypixelLocationApi.inHypixel}",
            "scoreboardShowsSkyBlock: $scoreboardShowsSkyBlock",
            "scoreboardTitle: $scoreboardTitle",
            "modApiIsland: ${HypixelLocationApi.island}",
            "inSkyBlock: $inSkyBlock",
        )
        if (inSkyBlock) {
            event.addIrrelevant(list)
        } else {
            event.addData(list)
        }
    }
}
