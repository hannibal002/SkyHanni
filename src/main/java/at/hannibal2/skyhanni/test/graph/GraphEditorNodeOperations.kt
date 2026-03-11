package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.features.misc.pathfind.AreaNode
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition

object GraphEditorNodeOperations {

    private val state get() = GraphEditor.state
    private val nodes get() = state.nodes
    private val edges get() = state.edges

    fun addNode() {
        val closestNode = state.closestNode
        if (closestNode != null && closestNode.distanceSqToPlayer() < 9.0 && closestNode == state.activeNode) {
            GraphEditor.feedBackInTutorial("Removed node, since you where closer than 3 blocks from a the active node.")
            GraphEditorHistory.save("removed node")
            nodes.remove(closestNode)
            edges.removeIf { it.isInEdge(closestNode) }
            GraphEditor.updateCache()
            if (closestNode == state.activeNode) state.activeNode = null
            state.closestNode = null
            return
        }

        if (nodes.any { it.position == playerPosition }) {
            GraphEditor.feedBackInTutorial("Can't create node, here is already another one.")
            return
        }
        val node = GraphingNode(state.id++, playerPosition)
        GraphEditorHistory.save("added node")
        nodes.add(node)
        GraphEditor.feedBackInTutorial("Added graph node.")
        state.activeNode?.let {
            addEdge(it, node)
        }
        GraphEditor.updateCache()
    }

    fun addEdge(node1: GraphingNode?, node2: GraphingNode?, direction: EdgeDirection = EdgeDirection.BOTH): Boolean {
        if (node1 == null || node2 == null || node1 == node2) return false
        val edge = GraphingEdge(node1, node2, direction)
        if (edge.isInEdge(state.activeNode)) {
            state.checkDissolve()
            state.selectedEdge = GraphEditor.state.findEdgeBetweenActiveAndClosest()
        }
        return edges.add(edge)
    }

    fun handleDissolve() {
        if (!state.dissolvePossible) return
        val activeNode = state.activeNode ?: return

        GraphEditor.feedBackInTutorial("Dissolved the node, now it is gone.")
        val edgePair = edges.filter { it.isInEdge(activeNode) }
        val edge1 = edgePair[0]
        val edge2 = edgePair[1]

        val neighbors1 = edge1.getOther(activeNode)
        val neighbors2 = edge2.getOther(activeNode)

        val direction = getDirection(edge1, edge2, neighbors1, activeNode, neighbors2)
        GraphEditorHistory.save("dissolved node")
        edges.removeAll(edgePair)
        nodes.remove(activeNode)
        state.activeNode = null
        addEdge(neighbors1, neighbors2, direction)
    }

    private fun getDirection(
        edge1: GraphingEdge,
        edge2: GraphingEdge,
        neighbors1: GraphingNode,
        activeNode: GraphingNode,
        neighbors2: GraphingNode,
    ): EdgeDirection {
        if (edge1.direction == EdgeDirection.BOTH || edge2.direction == EdgeDirection.BOTH) return EdgeDirection.BOTH
        return when {
            edge1.isValidConnectionFromTo(neighbors1, activeNode) && edge2.isValidConnectionFromTo(activeNode, neighbors2) ->
                EdgeDirection.ONE_TO_TWO

            edge1.isValidConnectionFromTo(activeNode, neighbors1) && edge2.isValidConnectionFromTo(neighbors2, activeNode) ->
                EdgeDirection.TWO_TO_ONE

            else -> EdgeDirection.BOTH
        }
    }

    fun handleConnect() {
        if (state.activeNode == state.closestNode) return
        val edge = GraphEditor.state.getEdgeIndex(state.activeNode, state.closestNode)
        if (edge == null) {
            GraphEditorHistory.save("added edge")
            addEdge(state.activeNode, state.closestNode)
            GraphEditor.feedBackInTutorial("Added new edge.")
        } else {
            GraphEditorHistory.save("removed edge")
            edges.removeAt(edge)
            state.checkDissolve()
            state.selectedEdge = GraphEditor.state.findEdgeBetweenActiveAndClosest()
            GraphEditor.feedBackInTutorial("Removed edge.")
            GraphEditor.updateRender()
        }
    }

    fun handleNameShortcut(name: String?): Pair<GraphNodeTag, String>? = when (name) {
        "fsoul" -> GraphNodeTag.FAIRY_SOUL to "Fairy Soul"
        "na" -> GraphNodeTag.AREA to AreaNode.NO_AREA
        else -> null
    }
}
