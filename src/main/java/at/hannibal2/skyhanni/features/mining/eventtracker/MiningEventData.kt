package at.hannibal2.skyhanni.features.mining.eventtracker

import at.hannibal2.skyhanni.data.IslandType
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class MiningEventData(
    @Expose @SerializedName("server_type") val serverType: IslandType,
    @Expose @SerializedName("server_id") val serverId: String,
    @Expose val event: MiningEvent,
    @Expose @SerializedName("time_left") val timeRemaining: Long,
    @Expose @SerializedName("reporter_uuid") val uuid: String
)
