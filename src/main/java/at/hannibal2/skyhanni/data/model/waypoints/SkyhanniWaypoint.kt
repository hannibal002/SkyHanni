package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

@KSerializable
data class SkyhanniWaypoint(
    @Expose override val location: LorenzVec,
    @Expose override var number: Int,
    @Expose override val name: String = "",
    @Expose private val options: MutableMap<String, String> = mutableMapOf(),
) : AbstractWaypoint<SkyhanniWaypoint>(),
    AbstractNamedWaypoint,
    AbstractSequencedWaypoint {
    override fun duplicate(): SkyhanniWaypoint = copy()
}

// todo need a migration to convert all `options` maps into name instead
//  previously was @Expose val options: MutableMap<String, String> = mutableMapOf(),
