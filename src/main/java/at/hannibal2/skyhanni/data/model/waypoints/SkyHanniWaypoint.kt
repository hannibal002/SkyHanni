package at.hannibal2.skyhanni.data.model.waypoints

import com.google.gson.annotations.Expose
import net.minecraft.world.phys.Vec3

class SkyHanniWaypoint(
    @Expose
    val location: Vec3,
    @Expose
    var number: Int,
    @Expose
    val options: MutableMap<String, String> = mutableMapOf(),
) : Copyable<SkyHanniWaypoint> {
    override fun copy() = SkyHanniWaypoint(location, number, options)
}
