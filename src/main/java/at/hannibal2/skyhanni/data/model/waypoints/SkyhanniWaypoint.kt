package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose

@KSerializable
@AutoService(AbstractWaypointFormat::class)
data class SkyhanniWaypoint(
    @Expose override val location: LorenzVec,
    @Expose override var number: Int,
    @Expose val options: MutableMap<String, String> = mutableMapOf(),
    @Expose override var name: String = options["name"] ?: number.toString(),
) : AbstractWaypoint<SkyhanniWaypoint>,
    AbstractDescriptiveWaypoint,
    AbstractWaypointFormat<SkyhanniWaypoint> {

    override val formatType = WaypointFormatType.SKYHANNI
    override fun toSkyHanniFormat() = this
}
