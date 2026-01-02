package at.hannibal2.skyhanni.features.misc.navigation

import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.features.misc.IslandAreas.getAreaTag

// A GraphNode that has an area tag, representing a area border.
data class AreaNode(
    val node: GraphNode,
    val name: String,
    val tag: GraphNodeTag,
    val distance: Double,
) {
    private val configVisibleTag: GraphNodeTag? get() = node.getAreaTag(useConfig = true)

    val isConfigVisible: Boolean get() = configVisibleTag != null
    val isNoArea: Boolean get() = name == "no_area"
}
