package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import net.minecraft.client.Minecraft
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GraphEditorIO {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    private val state get() = GraphEditor.state
    private val nodes get() = state.nodes
    private val edges get() = state.edges

    fun compileGraph(nodeSubset: Set<GraphingNode>? = null): Graph {
        val filteredNodes = nodeSubset ?: nodes
        val filteredEdges = if (nodeSubset != null) {
            edges.filter { it.node1 in nodeSubset && it.node2 in nodeSubset }
        } else {
            edges
        }

        val compiledNodeMap = filteredNodes.associate { node ->
            node.id to GraphNode(
                node.id,
                node.position,
                node.name,
                node.tags.map { it.internalName },
                node.extraWeight,
            )
        }

        val edgesByNode = filteredNodes.associateWith { node ->
            filteredEdges.filter { it.isInEdge(node) && it.isValidDirectionFrom(node) }
        }

        for (node in filteredNodes) {
            val compiledNode = compiledNodeMap[node.id] ?: continue
            val nodeEdges = edgesByNode[node].orEmpty()

            compiledNode.neighbours = nodeEdges.map { edge ->
                val otherNode = edge.getOther(node)
                val compiledOther = compiledNodeMap[otherNode.id]
                    ?: error("Invalid node ID ${otherNode.id} referenced in edge")
                val distance = node.position.distance(otherNode.position)
                val extraWeight = node.extraWeight + otherNode.extraWeight
                compiledOther to distance + extraWeight
            }.sortedBy { it.second }.toMap()
        }

        return Graph(compiledNodeMap.values.toList())
    }

    fun createStateFrom(graph: Graph): GraphEditorState {
        val newState = GraphEditorState()
        val (nodes, edges) = convertToGraphingData(graph) { it.id }
        newState.nodes.addAll(nodes)
        newState.edges.addAll(edges)
        newState.id = newState.nodes.maxOfOrNull { it.id }?.plus(1) ?: 0
        return newState
    }

    fun save() {
        if (nodes.isEmpty()) {
            ChatUtils.chat("Copied nothing since the graph is empty.")
            return
        }
        val compileGraph = compileGraph()
        val json = compileGraph.toJson()
        OSUtils.copyToClipboard(json)
        ChatUtils.chat("Copied Graph to Clipboard.")
        val networkCount = GraphEditorNetworks.recalculate()

        if (config.useAsIslandArea) {
            SkyHanniMod.launchCoroutine("bridge graph networks") {
                GraphEditorNetworks.bridgeNetworks(compileGraph)
                Minecraft.getInstance().execute {
                    IslandGraphs.setNewGraph(compileGraph)
                    GraphEditorBugFinder.runTests()
                    if (GraphEditorNodeFinder.active) {
                        GraphEditorNodeFinder.calculateNewAllNodeFind()
                    }
                }
            }
        }

        if (config.showsStats) {
            val length = edges.sumOf { it.node1.position.distance(it.node2.position) }.toInt().addSeparators()
            val namedNodes = nodes.count { it.name != null }.addSeparators()
            ChatUtils.chat(
                "§lStats\n" +
                    "§eNamed Nodes: $namedNodes\n" +
                    "§eNodes: ${nodes.size.addSeparators()}\n" +
                    "§eEdges: ${edges.size.addSeparators()}\n" +
                    "§eLength: $length",
            )
            if (networkCount > 1) {
                ChatUtils.clickableChat(
                    "§cNetworks: ${networkCount.addSeparators()}",
                    onClick = { GraphEditorNetworks.findNetworks() },
                    hover = "Click to find networks!",
                )
            }
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
        GraphEditorNetworks.recalculate()
        ChatUtils.chat("Graph Editor loaded this island!")
    }

    fun mergeFromClipboard() {
        val json = OSUtils.readFromClipboard()
        if (json == null) {
            ChatUtils.userError("Clipboard is empty!")
            return
        }

        SkyHanniMod.launchIOCoroutine("merge graph json") {
            try {
                val graph = Graph.fromJson(json)

                Minecraft.getInstance().execute {
                    GraphEditorHistory.save("merge from clipboard")

                    var nextId = state.id
                    val (newNodes, newEdges) = convertToGraphingData(graph) { nextId++ }
                    nodes.addAll(newNodes)
                    edges.addAll(newEdges)
                    state.id = nextId

                    GraphEditorNetworks.recalculate()
                    GraphEditor.updateCache()

                    val nodeCount = newNodes.size.addSeparators()
                    val edgeCount = newEdges.size.addSeparators()
                    ChatUtils.chat("Merged $nodeCount nodes and $edgeCount edges from clipboard.")
                }
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Merge failed", "json" to json, ignoreErrorCache = true)
            }
        }
    }

    private fun convertToGraphingData(graph: Graph, idProvider: (GraphNode) -> Int): Pair<List<GraphingNode>, List<GraphingEdge>> {
        val importedNodes = graph.map { graphNode ->
            GraphingNode(
                idProvider(graphNode),
                graphNode.position,
                graphNode.name,
                graphNode.tagNames.mapNotNull { tag -> GraphNodeTag.byId(tag) }.toMutableList(),
                graphNode.extraWeight,
            )
        }
        val translation = graph.zip(importedNodes).toMap()

        val rawEdges = graph.flatMap { node ->
            node.neighbours.mapNotNull { (neighbor, _) ->
                val node1 = translation[node] ?: error("Invalid node in translation: ${node.id}")
                val node2 = translation[neighbor] ?: error("Invalid neighbor in translation: ${neighbor.id}")
                GraphingEdge(node1, node2, EdgeDirection.ONE_TO_TWO)
            }
        }

        val reducedEdges = rawEdges.groupingBy { it }.reduce { _, accumulator, element ->
            if (
                (element.node1 == accumulator.node1 && accumulator.direction != element.direction) ||
                (element.node1 == accumulator.node2 && accumulator.direction == element.direction)
            ) {
                accumulator.direction = EdgeDirection.BOTH
            }
            accumulator
        }

        return importedNodes to reducedEdges.values.toList()
    }

}
