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

    fun copy(): GraphEditorState {
        val newState = GraphEditorState()

        newState.id = this.id
        newState.dissolvePossible = this.dissolvePossible
        newState.seeThroughBlocks = this.seeThroughBlocks
        newState.inEditMode = this.inEditMode
        newState.inTutorialMode = this.inTutorialMode
        newState.textBox.textBox = this.textBox.textBox

        val nodeMap = mutableMapOf<GraphingNode, GraphingNode>()

        for (oldNode in this.nodes) {
            val newNode = GraphingNode(
                oldNode.id,
                oldNode.position.copy(),
                oldNode.name,
                ArrayList(oldNode.tags),
                oldNode.extraWeight,
            )
            newState.nodes.add(newNode)
            nodeMap[oldNode] = newNode
        }

        for (oldEdge in this.edges) {
            val n1 = nodeMap[oldEdge.node1]!!
            val n2 = nodeMap[oldEdge.node2]!!
            val newEdge = GraphingEdge(n1, n2, oldEdge.direction)
            newState.edges.add(newEdge)
        }

        newState.activeNode = this.activeNode?.let { nodeMap[it] }
        newState.closestNode = this.closestNode?.let { nodeMap[it] }

        val selectedIndex = this.edges.indexOf(this.selectedEdge)
        if (selectedIndex != -1) {
            newState.selectedEdge = newState.edges[selectedIndex]
        }

        return newState
    }
}
