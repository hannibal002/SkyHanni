package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.LorenzUtils

object ReminderUtils {

    // TODO: add arachne fight, add slayer boss spawned
    fun isBusy(): Boolean =
        DungeonAPI.inDungeon() || LorenzUtils.inKuudraFight || FarmingContestAPI.inContest || RiftAPI.inRift()
}
