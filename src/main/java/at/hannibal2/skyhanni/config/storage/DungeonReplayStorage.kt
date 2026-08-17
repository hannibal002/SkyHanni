package at.hannibal2.skyhanni.config.storage

import at.hannibal2.skyhanni.config.features.dungeon.replay.DungeonBossReplayConfig
import at.hannibal2.skyhanni.features.dungeon.replay.DungeonReplay
import com.google.gson.annotations.Expose

class DungeonReplayStorage {
    @Expose
    var replays: MutableMap<DungeonBossReplayConfig.DungeonFloorWithBoss, DungeonReplay.DungeonGhostData> = mutableMapOf()
}
