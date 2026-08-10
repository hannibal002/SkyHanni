package at.hannibal2.skyhanni.features.dungeon.replay

import com.google.gson.annotations.Expose
import java.util.UUID

data class DungeonGhostData(
    @Expose val recordedPositions: MutableList<RecordedPositionDelta> = mutableListOf(),
    @Expose val time: Long = Long.MAX_VALUE,
    @Expose val playerUUID: UUID = UUID.fromString("49f4c15d-14e0-4d75-be1b-9c1b85bad53c"),
    @Expose val playerName: String = "martimavocado"
) {
    override fun toString(): String {
        return buildString {
            if (recordedPositions.isNotEmpty()) {
                append(recordedPositions.first().toString())
            }

            if (recordedPositions.size > 1) {
                append("[+${recordedPositions.size-1}]")
            }
            append(";")
            append(time)
            append(";")
            append(playerName)
            append(":")
            append(playerUUID)
        }
    }
}
