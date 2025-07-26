package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose

@KSerializable
@AutoService(AbstractWaypointFormat::class)
data class ColeweightWaypoint(
    @Expose override val x: Int,
    @Expose override val y: Int,
    @Expose override val z: Int,
    @Expose override val r: Float = 0.0f,
    @Expose override val g: Float = 1.0f,
    @Expose override val b: Float = 0.0f,
    @Expose val options: MutableMap<String, String> = mutableMapOf(),
) : AbstractXYZWaypoint(x, y, z),
    AbstractRGBColoredWaypoint,
    AbstractWaypointFormat<ColeweightWaypoint> {

    constructor(skyhanniWaypoint: SkyhanniWaypoint) : this(
        skyhanniWaypoint.location.x.toInt(),
        skyhanniWaypoint.location.y.toInt(),
        skyhanniWaypoint.location.z.toInt(),
        options = skyhanniWaypoint.options,
    )

    override val formatType = WaypointFormatType.COLEWEIGHT
    override fun toSkyHanniFormat() = SkyhanniWaypoint(
        LorenzVec(x, y, z),
        options["name"]?.toIntOrNull() ?: -1,
        options,
    )
}
