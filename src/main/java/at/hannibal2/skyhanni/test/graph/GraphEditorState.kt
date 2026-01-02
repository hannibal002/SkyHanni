package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.SimpleTimeMark

class GraphEditorState {
    var id = 0
    val nodes = mutableListOf<GraphingNode>()
    val edges = mutableListOf<GraphingEdge>()

    var activeNode: GraphingNode? = null
        set(value) {
            field = value
            selectedEdge = findEdgeBetweenActiveAndClosest()
            checkDissolve()
        }

    var closestNode: GraphingNode? = null
        set(value) {
            field = value
            selectedEdge = findEdgeBetweenActiveAndClosest()
        }

    var selectedEdge: GraphingEdge? = null
    var dissolvePossible = false

    var seeThroughBlocks = true
    var inEditMode = false
    var inTutorialMode = false

    val textBox = TextInput()
    var inTextMode = false
        set(value) {
            field = value
            if (value) {
                activeNode?.name?.let { textBox.textBox = it }
                textBox.makeActive()
            } else {
                textBox.clear()
                textBox.disable()
            }
        }

    val highlightedNodes = mutableSetOf<GraphingNode>()
    val highlightedEdges = mutableSetOf<GraphingEdge>()

    var cachedNearbyNodes = listOf<GraphingNode>()
    var lastCacheUpdate = SimpleTimeMark.farPast()

    fun findEdgeBetweenActiveAndClosest(): GraphingEdge? =
        getEdgeIndex(activeNode, closestNode)?.let { edges[it] }

    fun getEdgeIndex(node1: GraphingNode?, node2: GraphingNode?) =
        if (node1 != null && node2 != null && node1 != node2) {
            val search = GraphingEdge(node1, node2)
            edges.indexOfFirst { it == search }.takeIf { it != -1 }
        } else null

    fun checkDissolve() {
        val active = activeNode
        if (active == null) {
            dissolvePossible = false
            return
        }
        dissolvePossible = edges.count { it.isInEdge(active) } == 2
    }
}
