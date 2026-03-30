package at.hannibal2.skyhanni.data.model.graph

import at.hannibal2.skyhanni.features.misc.pathfind.NavigationHelper
import at.hannibal2.skyhanni.test.graph.GraphEditor
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LorenzVec

class GraphNode(
    val id: Int,
    override val position: LorenzVec,
    val name: String? = null,
    val tagNames: List<String> = emptyList(),
    val extraWeight: Int = 0,
) : GraphUtils.GenericNode {

    val tags: List<GraphNodeTag> by lazy {
        tagNames.mapNotNull { GraphNodeTag.byId(it) }
    }

    var enabled = true
        set(value) {
            if (value != field) GraphEditor.flagDisabledDirty()
            field = value
        }

    /** Keys are the neighbors and value the edge weight (e.g. distance) */
    lateinit var neighbors: Map<GraphNode, Double>

    fun hasTag(tag: GraphNodeTag): Boolean = tag in tags

    fun sameNameAndTags(other: GraphNode): Boolean = name == other.name && allowedTags == other.allowedTags

    private val allowedTags get() = tags.filter { it in NavigationHelper.allowedTags }

    // Identity is by id alone — two GraphNode references with the same id are the same node regardless
    // of mutable state (neighbors, enabled), which must not participate in equality.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return id == (other as GraphNode).id
    }

    override fun hashCode() = id
}
