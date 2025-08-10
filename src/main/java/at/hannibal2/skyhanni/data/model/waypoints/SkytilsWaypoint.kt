package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.json.fromJsonOrNull
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

    constructor() : this(0, 0, 0, "", true, 0, 0L)

    private data class SkytilsWaypointCategoriesSet(
        @Expose val categories: List<SkytilsWaypointCategory>
    )

    private data class SkytilsWaypointCategory(
        @Expose val name: String,
        @Expose val island: String,
        @Expose val waypoints: WaypointSet<SkytilsWaypoint>,
    )

    override fun deserialize(string: String): WaypointSet<SkytilsWaypoint>? = kotlin.runCatching {
        val stringToUse = kotlin.runCatching { StringUtils.decodeBase64(string) }.getOrNull() ?: string
        val asCategorySet = ConfigManager.gson.fromJsonOrNull<SkytilsWaypointCategoriesSet>(stringToUse)
        if (asCategorySet == null) {
            ConfigManager.gson.fromJsonOrNull<SkytilsWaypointCategory>(stringToUse)?.waypoints
                ?: ConfigManager.gson.fromJsonOrNull<WaypointSet<SkytilsWaypoint>>(stringToUse)
        } else {
            asCategorySet.categories.firstOrNull()?.waypoints
        }
    }.getOrNull()

    override val formatType = WaypointFormatType.SKYTILS
    override val color by lazy { Color(colorInt).toChromaColor() }
    override fun toSkyHanniFormat(indexOffer: Int) = SkyhanniWaypoint(
        location,
        name.toIntOrNull() ?: (indexOffer + 1),
    )
}
