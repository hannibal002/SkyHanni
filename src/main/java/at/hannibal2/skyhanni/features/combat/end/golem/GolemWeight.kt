package at.hannibal2.skyhanni.features.combat.end.golem

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossFightEndEvent
import at.hannibal2.skyhanni.events.GolemWeightEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlin.time.Duration.Companion.seconds

/**
 * Calculates the weight for a finished End Stone Protector fight. The shared part of the
 * end-of-fight summary is parsed by [at.hannibal2.skyhanni.features.combat.end.dragon.DragonFightAPI];
 * only the golem specific zealot line is read here.
 */
@SkyHanniModule
object GolemWeight {

    private val config get() = SkyHanniMod.feature.combat.endIsland.golem

    private val repoGroup = RepoPattern.group("combat.boss.protector.2.weight")

    /**
     * WRAPPED-REGEX-TEST: "                       Zealots Contributed: 27/100"
     */
    private val zealotsPattern by repoGroup.pattern(
        "chat.end.zealot",
        "\\s+Zealots Contributed: (?<amount>\\d+)/100",
    )

    private var pendingResult: EndBossFightEndEvent? = null

    /**
     * When that result arrived. The zealot line follows within a moment, so anything later belongs
     * to a different fight - without this, a summary whose zealot line never came would be paired
     * with the next fight's line and produce a weight from two different fights.
     */
    private var pendingSince = SimpleTimeMark.farPast()

    private val PENDING_TIMEOUT = 10.seconds

    /** Weight of the last finished protector fight, read by the dry streak tracker. */
    var weight = 0.0
        private set

    private fun getWeightForPlacement(placement: Int) = when (placement) {
        -1 -> 10
        1 -> 200
        2 -> 175
        3 -> 150
        4 -> 125
        5 -> 110
        6, 7, 8 -> 100
        9, 10 -> 90
        11, 12 -> 80
        else -> 70
    }

    fun calculateWeight(zealots: Int, placement: Int, firstDamage: Double, ownDamage: Double): Double {
        val placementWeight = getWeightForPlacement(if (ownDamage == 0.0) -1 else placement)
        val damageRatio = ownDamage / (firstDamage.takeIf { it != 0.0 } ?: 1.0)
        return placementWeight + 50 * damageRatio + if (zealots > 100) 100 else zealots
    }

    @HandleEvent
    private fun onEndBossFightEnd(event: EndBossFightEndEvent) {
        if (event.boss != EndBoss.END_STONE_PROTECTOR) return
        // The zealot line still follows, so the weight can only be finished there.
        pendingResult = event
        pendingSince = SimpleTimeMark.now()
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val result = pendingResult ?: return
        if (pendingSince.passedSince() > PENDING_TIMEOUT) {
            pendingResult = null
            return
        }
        zealotsPattern.matchMatcher(event.cleanMessage) {
            val zealots = group("amount").toInt()
            weight = calculateWeight(zealots, result.place, result.topDamage, result.yourDamage)
            if (config.weightChat) {
                ChatUtils.chat(
                    "§f${" ".repeat(30)}§r§eYour Weight: §r§a${weight.roundTo(0).addSeparators()}",
                    prefix = false,
                )
            }
            pendingResult = null
            GolemWeightEvent(weight).post()
        }
    }

    @HandleEvent
    private fun onIslandChange(event: IslandChangeEvent) {
        pendingResult = null
    }
}
