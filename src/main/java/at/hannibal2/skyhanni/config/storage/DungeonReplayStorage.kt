package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.features.dungeon.replay.DungeonGhostData
import com.google.gson.annotations.Expose

class DungeonReplayStorage {
    @Expose
    var manual: DungeonGhostData = DungeonGhostData()

    @Expose
    var floor3: DungeonGhostData = DungeonGhostData()

    @Expose
    var floor7: DungeonGhostData = DungeonGhostData()

    @Expose
    var floorMaster7: DungeonGhostData = DungeonGhostData()
}
