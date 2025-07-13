package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

class SkyhanniWaypoint(
    @Expose
    val location: LorenzVec,
    @Expose
    var number: Int,
    @Expose
    val options: MutableMap<String, String> = mutableMapOf(),
) : Copyable<SkyhanniWaypoint> {
    override fun copy() = SkyhanniWaypoint(location, number, options)
}
