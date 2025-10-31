package at.hannibal2.hanni.features.fame

import at.hannibal2.hanni.data.IslandTypeTags
import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.features.garden.contest.FarmingContestApi
import at.hannibal2.hanni.features.nether.kuudra.KuudraApi
import at.hannibal2.hanni.features.rift.RiftApi

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
            (FarmingContestApi.inContest && !ignoreFarmingContest) ||
            RiftApi.inRift() ||
            IslandTypeTags.BUSY.inAny()
}
