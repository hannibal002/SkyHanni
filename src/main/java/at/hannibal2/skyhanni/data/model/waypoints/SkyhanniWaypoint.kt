package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose
import java.awt.Color

class SkyhanniWaypoint(
    @Expose
    val location: LorenzVec,
    @Expose
    val color: Color = Color(0f, 1f, 0f, 0.4f),
    @Expose
    var number: Int,
    @Expose
    val options: MutableMap<String, String> = mutableMapOf(),
)
