package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GraphEditorIO {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    private val state get() = GraphEditor.state
    private val nodes get() = state.nodes
    private val edges get() = state.edges

    fun compileGraph(): Graph {
        val indexedTable = nodes.mapIndexed { index, node -> node.id to index }.toMap()
        val compiledNodes = nodes.mapIndexed { index, node ->
            GraphNode(
                index,
                node.position,
                node.name,
                node.tags.map { it.internalName },
            )
        }

        val edgesByNode = nodes.associateWith { node ->
            edges.filter { it.isInEdge(node) && it.isValidDirectionFrom(node) }
        }

        val neighbours = nodes.map { node ->
            val nodeEdges = edgesByNode[node].orEmpty()

            nodeEdges.map { edge ->
                val otherNode = edge.getOther(node)
                val index = indexedTable[otherNode.id] ?: error("Invalid node ID ${otherNode.id} referenced in edge")
                compiledNodes[index] to node.position.distance(otherNode.position)
            }.sortedBy { it.second }
        }

        compiledNodes.forEachIndexed { index, node ->
            node.neighbours = neighbours[index].toMap()
        }
        return Graph(compiledNodes)
    }

    fun createStateFrom(graph: Graph): GraphEditorState {
        val newState = GraphEditorState()

        newState.nodes.addAll(
            graph.map {
                GraphingNode(
                    it.id,
                    it.position,
                    it.name,
                    it.tagNames.mapNotNull { tag -> GraphNodeTag.byId(tag) }.toMutableList(),
                )
            },
        )
        val translation = graph.zip(newState.nodes).toMap()

        val neighbors = graph.flatMap { node ->
            node.neighbours.mapNotNull { (neighbor, _) ->
                val node1 = translation[node]
                val node2 = translation[neighbor]
                if (node1 == null || node2 == null) {
                    error("Invalid edge reference: node ${node.id} <-> neighbor ${neighbor.id}")
                }
                GraphingEdge(node1, node2, EdgeDirection.ONE_TO_TWO)
            }
        }

        val reduced = neighbors.groupingBy { it }.reduce { _, accumulator, element ->
            if (
                (element.node1 == accumulator.node1 && accumulator.direction != element.direction) ||
                (element.node1 == accumulator.node2 && accumulator.direction == element.direction)
            ) {
                accumulator.direction = EdgeDirection.BOTH
            }
            accumulator
        }

        newState.edges.addAll(reduced.values)
        newState.id = newState.nodes.lastOrNull()?.id?.plus(1) ?: 0
        return newState
    }

    fun save() {
        if (nodes.isEmpty()) {
            ChatUtils.chat("Copied nothing since the graph is empty.")
            return
        }
        val compileGraph = compileGraph()
        if (config.useAsIslandArea) {
            IslandGraphs.setNewGraph(compileGraph)
            GraphEditorBugFinder.runTests()
            if (GraphEditorNodeFinder.active) {
                GraphEditorNodeFinder.calculateNewAllNodeFind()
            }
        }
        val json = compileGraph.toJson()
        OSUtils.copyToClipboard(json)
        ChatUtils.chat("Copied Graph to Clipboard.")
        if (config.showsStats) {
            val length = edges.sumOf { it.node1.position.distance(it.node2.position) }.toInt().addSeparators()
            ChatUtils.chat(
                "§lStats\n" + "§eNamed Nodes: ${
                    nodes.count { it.name != null }.addSeparators()
                }\n" + "§eNodes: ${nodes.size.addSeparators()}\n" + "§eEdges: ${edges.size.addSeparators()}\n" + "§eLength: $length",
            )
        }
    }

    fun loadThisIsland() {
        val graph = IslandGraphs.currentIslandGraph
        if (graph == null) {
            ChatUtils.userError("This island does not have graph data!")
            return
        }

        IslandGraphs.disabledNodesReason?.let {
            if (GraphEditor.bypassTempRemoveTimer.isInPast()) {
                IslandGraphs.enableAllNodes()
                ChatUtils.chat("Reset temp remove!")
            } else {
                ChatUtils.chat("§cParts of the island graph are currently temp removed: $it")
                ChatUtils.chat("Run this command again in the next 5 seconds to remove the temp remove logic and copy the current island!")
                GraphEditor.bypassTempRemoveTimer = 5.seconds.fromNow()
                return
            }
        }
        GraphEditor.enable()
        GraphEditorHistory.save("load island ${IslandGraphs.lastLoadedIslandType}")
        GraphEditor.state = createStateFrom(graph)
        ChatUtils.chat("Graph Editor loaded this island!")
    }

}
