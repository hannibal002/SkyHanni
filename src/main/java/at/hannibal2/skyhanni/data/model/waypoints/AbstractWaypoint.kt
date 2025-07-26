package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawEdges
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import io.github.notenoughupdates.moulconfig.ChromaColour

sealed interface Waypoint {
    val location: LorenzVec
}

sealed interface AbstractWaypoint : Waypoint {
    fun toSkyHanniFormat(indexOffer: Int): SkyhanniWaypoint

    fun SkyHanniRenderWorldEvent.drawFilledSelf(color: ChromaColour) = this.drawWaypointFilled(
        location,
        color.toColor(),
        true,
    )

    fun SkyHanniRenderWorldEvent.drawEdgesSelf(color: ChromaColour, thickness: Int) = this.drawEdges(
        location,
        color.toColor(),
        thickness,
        false,
    )

    fun SkyHanniRenderWorldEvent.drawDistanceTo() = this.drawString(
        location.add(0.5, 2.0, 0.5),
        "§e${location.distanceToPlayer().roundTo(1).addSeparators()} m",
        seeThroughBlocks = true,
    )

    fun SkyHanniRenderWorldEvent.drawLineToEye(color: ChromaColour, thickness: Int) = this.drawLineToEye(
        location.add(0.5, 0.25, 0.5),
        color.toColor(),
        thickness,
        depth = true,
    )

    fun SkyHanniRenderWorldEvent.draw3DLineToOther(
        other: AbstractWaypoint,
        color: ChromaColour,
        thickness: Int,
        accountForSneak: Boolean,
    ) {
        val eyePos = if (accountForSneak) 1.54 else 1.62
        this.draw3DLine(
            location.add(0.5, 1.0 + eyePos, 0.5),
            other.location.add(0.5, 0.5, 0.5),
            color,
            thickness,
            depth = true,
        )
    }
}

sealed class AbstractXYZWaypoint(
    open val x: Int,
    open val y: Int,
    open val z: Int,
) : AbstractWaypoint {
    override val location by lazy { LorenzVec(x, y, z) }
}

sealed interface AbstractDescriptiveWaypoint : AbstractSequencedWaypoint, AbstractNamedWaypoint {
    fun hasCustomName() = name != number.toString()
}

sealed interface AbstractSequencedWaypoint : AbstractWaypoint {
    var number: Int
}

sealed interface AbstractNamedWaypoint : Waypoint {
    var name: String

    fun refreshOrRetainName(number: Int) {
        if (name != number.toString()) return
        name = number.toString()
    }

    fun SkyHanniRenderWorldEvent.drawName() = this.drawString(
        location.add(0.5, 2.5, 0.5),
        "§e$name",
        seeThroughBlocks = true,
    )
}

sealed interface AbstractColoredWaypoint {
    val color: ChromaColour
}

sealed interface AbstractRGBColoredWaypoint : AbstractColoredWaypoint {
    val r: Number
    val g: Number
    val b: Number

    override val color: ChromaColour
        get() {
            val shouldScale = listOf(r, g, b).none { it.toInt() > 1 }
            val (sr, sg, sb) = listOf(r, g, b).map {
                if (shouldScale) it.toFloat() * 255.0f else it.toFloat()
            }.map { it.toInt() }
            return ChromaColour.fromStaticRGB(sr, sg, sb, a = 255)
        }
}

sealed interface AbstractToggleableWaypoint {
    var enabled: Boolean
}
