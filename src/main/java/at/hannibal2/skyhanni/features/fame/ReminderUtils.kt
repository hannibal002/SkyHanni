package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland

object ReminderUtils {

    // TODO: add arachne fight, add slayer boss spawned
    fun isBusy(): Boolean =
        IslandType.CATACOMBS.isInIsland() || LorenzUtils.inKuudraFight || FarmingContestAPI.inContest || RiftAPI.inRift()
}
