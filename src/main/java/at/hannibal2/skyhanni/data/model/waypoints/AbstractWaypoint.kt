package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.utils.LorenzVec
import io.github.notenoughupdates.moulconfig.ChromaColour

sealed class AbstractWaypoint<T: AbstractWaypoint<T>> {
    abstract val location: LorenzVec
    abstract fun duplicate(): T
}

sealed class AbstractXYZWaypoint(
    open val x: Int,
    open val y: Int,
    open val z: Int
) : AbstractWaypoint<AbstractXYZWaypoint>() {
    override val location by lazy { LorenzVec(x, y, z) }
}

sealed interface AbstractSequencedWaypoint {
    val number: Int
}

sealed interface AbstractNamedWaypoint {
    val name: String
}

sealed interface AbstractColoredWaypoint {
    val color: ChromaColour
}

sealed interface AbstractToggleableWaypoint {
    var enabled: Boolean
}
