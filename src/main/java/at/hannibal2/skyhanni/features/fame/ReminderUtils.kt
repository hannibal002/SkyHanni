package at.hannibal2.skyhanni.features.fame

import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
//#if TODO
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestApi
//#endif
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
//#if TODO
import at.hannibal2.skyhanni.features.rift.RiftApi
//#endif

object ReminderUtils {

    /**
     * TODO:
     *  add arachne fight
     *  add slayer boss spawned
     *  add dragon fight
     *  add experimentation solver
     *  add matriach lair
     *  add server restart
     */
    fun isBusy(ignoreFarmingContest: Boolean = false): Boolean =
        (DungeonApi.inDungeon() && !DungeonApi.completed) ||
            KuudraApi.inKuudra ||
            //#if TODO
            (FarmingContestApi.inContest && !ignoreFarmingContest) ||
            RiftApi.inRift() ||
            //#endif
            IslandTypeTags.BUSY.inAny()
}
