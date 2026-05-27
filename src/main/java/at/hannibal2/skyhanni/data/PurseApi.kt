package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.PurseChangeCause
import at.hannibal2.skyhanni.events.PurseChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.million
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object PurseApi {
    private val patternGroup = RepoPattern.group("data.purse")

    /**
     * REGEX-TEST: Piggy: §6423,085,766
     * REGEX-TEST: Purse: §6423,085,776 §e(+5)
     */
    val coinsPattern by patternGroup.pattern(
        "coins",
        "(?:§.)*(?:Piggy|Purse): §6(?<coins>[\\d,.]+)(?: ?(?:§.)*\\([+-](?<earned>[\\d,.]+)\\)?|.*)?$",
    )

    /**
     * REGEX-TEST: Piggy: §6423,085,766
     * REGEX-TEST: §6§fPiggy: §6301,788§j§6,444
     */
    val piggyPattern by patternGroup.pattern(
        "piggy",
        "(?:§.)*Piggy: (?<coins>.*)",
    )

    private var inventoryCloseTime = SimpleTimeMark.farPast()
    var currentPurse = 0.0
        private set

    const val ARCHFIEND_COST = 666_666.0
    const val HIGH_CLASS_COST = 6_666_666.0
    val ARCHFIEND_PROFIT = 15.million
    val HIGH_CLASS_PROFIT = 100.million

    @HandleEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inventoryCloseTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        coinsPattern.firstMatcher(event.added) {
            val newPurse = group("coins").formatDouble()
            val diff = newPurse - currentPurse
            if (diff == 0.0) return
            currentPurse = newPurse

            PurseChangeEvent(diff, currentPurse, getCause(diff)).post()
        }
    }

    // TODO add more causes in the future (e.g. ah/bz/bank)
    private fun getCause(diff: Double): PurseChangeCause {
        if (diff > 0) {
            if (diff == 1.0) {
                return PurseChangeCause.GAIN_TALISMAN_OF_COINS
            }

            // TODO relic of coins support
            if (diff == ARCHFIEND_PROFIT) {
                return PurseChangeCause.GAIN_DICE_ROLL_ARCHFIEND
            }

            if (diff == HIGH_CLASS_PROFIT) {
                return PurseChangeCause.GAIN_DICE_ROLL_HIGH_CLASS
            }

            if (Minecraft.getInstance().screen == null) {
                if (inventoryCloseTime.passedSince() > 2.seconds) {
                    return PurseChangeCause.GAIN_MOB_KILL
                }
            }
            return PurseChangeCause.GAIN_UNKNOWN
        } else {
            if (SlayerApi.questStartTime.passedSince() < 1.5.seconds) {
                return PurseChangeCause.LOSE_SLAYER_QUEST_STARTED
            }

            if (diff == -ARCHFIEND_COST) {
                return PurseChangeCause.LOSE_DICE_ROLL_COST_ARCHFIEND
            }

            if (diff == -HIGH_CLASS_COST) {
                return PurseChangeCause.LOSE_DICE_ROLL_COST_HIGH_CLASS
            }

            return PurseChangeCause.LOSE_UNKNOWN
        }
    }

    fun getPurse(): Double = currentPurse
}
