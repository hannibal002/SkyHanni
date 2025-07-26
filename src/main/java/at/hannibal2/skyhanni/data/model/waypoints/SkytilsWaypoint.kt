package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.KSerializable
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.awt.Color

@KSerializable
@AutoService(AbstractWaypointFormat::class)
data class SkytilsWaypoint(
    @Expose override val x: Int,
    @Expose override val y: Int,
    @Expose override val z: Int,
    @Expose override var name: String,
    @Expose override var enabled: Boolean,
    @Expose @SerializedName("color") var colorInt: Int,
    @Expose val addedAt: Long = System.currentTimeMillis(),
) : AbstractXYZWaypoint(x, y, z),
    AbstractToggleableWaypoint,
    AbstractColoredWaypoint,
    AbstractNamedWaypoint,
    AbstractWaypointFormat<SkytilsWaypoint> {

    override val formatType = WaypointFormatType.SKYTILS
    override val color by lazy { Color(colorInt).toChromaColor() }
    override fun toSkyHanniFormat(indexOffer: Int) = SkyhanniWaypoint(
        location,
        name.toIntOrNull() ?: (indexOffer + 1),
    )
}
