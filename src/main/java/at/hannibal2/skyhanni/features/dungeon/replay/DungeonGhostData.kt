package at.hannibal2.skyhanni.features.dungeon.replay

import com.google.gson.annotations.Expose
import java.util.UUID

data class DungeonGhostData(
    @Expose val recordedPositions: List<RecordedPositionDelta> = listOf(),
    @Expose val time: Long = Long.MAX_VALUE,
    @Expose val playerUUID: UUID = UUID.fromString("49f4c15d-14e0-4d75-be1b-9c1b85bad53c"),
    @Expose val playerName: String = "martimavocado"
)
