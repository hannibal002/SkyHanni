package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland

object ReminderUtils {

    // TODO: add arachne fight, add slayer boss spawned, add dragon fight
    fun isBusy(): Boolean =
        DungeonAPI.inDungeon() || LorenzUtils.inKuudraFight || FarmingContestAPI.inContest ||
            RiftAPI.inRift() || IslandType.DARK_AUCTION.isInIsland()
}
