package at.hannibal2.hanni.data.model.waypoints

import at.hannibal2.hanni.utils.LorenzVec
import com.google.gson.annotations.Expose

class HanniWaypoint(
    @Expose
    val location: LorenzVec,
    @Expose
    var number: Int,
    @Expose
    val options: MutableMap<String, String> = mutableMapOf(),
) : Copyable<HanniWaypoint> {
    override fun copy() = HanniWaypoint(location, number, options)
}
