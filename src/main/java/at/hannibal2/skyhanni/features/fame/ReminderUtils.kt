package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.utils.LorenzUtils

object ReminderUtils {
    fun isBusy(): Boolean =
        LorenzUtils.inDungeons || LorenzUtils.inKuudraFight || FarmingContestAPI.inContest
}