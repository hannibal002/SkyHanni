package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.features.misc.pathfind.IslandAreaBackend.getAreaTag
import at.hannibal2.skyhanni.utils.SkyBlockUtils

// A GraphNode that has an area tag, representing a area border.
data class AreaNode(
    val node: GraphNode,
    val name: String,
    val tag: GraphNodeTag,
    val distance: Double,
) {

    companion object {
        const val NO_AREA = "no_area"
    }

    private val configVisibleTag: GraphNodeTag? get() = node.getAreaTag(useConfig = true)

    val coloredName: String = tag.color.getChatColor() + name
    val isConfigVisible: Boolean get() = configVisibleTag != null
    val isNoArea: Boolean get() = name == NO_AREA
    val isInside: Boolean get() = name == SkyBlockUtils.graphArea
}
