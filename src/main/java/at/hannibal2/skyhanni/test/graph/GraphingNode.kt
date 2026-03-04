package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LorenzVec

// The node object the graph editor is working with
class GraphingNode(
    val id: Int,
    override var position: LorenzVec,
    var name: String? = null,
    var tags: MutableList<GraphNodeTag> = mutableListOf(),
    var extraWeight: Int = 0,
) : GraphUtils.GenericNode {

    var rendering = true
    var enabled = true

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphingNode

        return id == other.id
    }
}
