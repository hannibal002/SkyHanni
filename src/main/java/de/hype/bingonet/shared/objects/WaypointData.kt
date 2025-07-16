package de.hype.bingonet.shared.objects

import java.awt.Color

open class WaypointData(
    var position: Position,
    var text: String,
    var renderDistance: Int,
    var visible: Boolean,
    var deleteOnServerSwap: Boolean,
    var render: MutableList<RenderInformation> = mutableListOf(),
    var color: Color = Color(1f, 1f, 1f),
    var renderThroughBlocks: Boolean = true,
    var renderBeacon: Boolean = false,
    var doTracer: Boolean = true,
) {
    val waypointId: Int = counter++
    companion object {
        var counter: Int = 1
    }
}
