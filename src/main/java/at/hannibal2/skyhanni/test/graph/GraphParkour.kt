package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.graph.GraphEditor.isEnabled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.LorenzVec.Companion.toLorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig

@SkyHanniModule
object GraphParkour {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shgraphloadparkour") {
            description = "Loads the current clipboard as parkour into the graph editor."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                CoroutineConfig("shgraphloadparkour command").launchCoroutine {
                    loadParkour()
                }
            }
        }
        event.registerBrigadier("shgraphexportasparkour") {
            description = "Saves the graph editor as parkour into the clipboard."
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { saveParkour() }
        }
    }

    private fun saveParkour() {
        val graph = GraphEditorIO.compileGraph()
        val list = graphToList(graph) ?: return

        val resultList = list.map { location ->
            val x = location.x.toString().replace(",", ".")
            val y = location.y.toString().replace(",", ".")
            val z = location.z.toString().replace(",", ".")
            "\"$x:$y:$z\"".replace(".0", "")
        }

        OSUtils.copyToClipboard(resultList.joinToString(",\n"))
        ChatUtils.chat("Saved graph as parkour to clipboard!")
    }

    private fun graphToList(graph: Graph): List<LorenzVec>? {
        val start = validateStartNode(graph) ?: return null
        validateEndNode(graph) ?: return null
        return validatePath(graph, start)
    }

    fun validateStartNode(graph: Graph): GraphNode? {
        val starts = graph.getNodesWithName("start")
        if (starts.isEmpty()) {
            ChatUtils.userError("No start node found!")
            return null
        }
        if (starts.size > 1) {
            ChatUtils.userError("More than one start node found!")
            return null
        }

        val start = starts.first()
        val neighbors = start.neighbors.entries
        if (neighbors.isEmpty()) {
            ChatUtils.userError("Start has no neighbors!")
            return null
        }
        if (neighbors.size != 1) {
            ChatUtils.userError("Start has more than one neighbors!")
            return null
        }

        return start
    }

    fun validateEndNode(graph: Graph): GraphNode? {
        val ends = graph.getNodesWithName("end")
        if (ends.isEmpty()) {
            ChatUtils.userError("No end node found!")
            return null
        }
        if (ends.size > 1) {
            ChatUtils.userError("More than one end node found!")
            return null
        }
        return ends.first()
    }

    fun validatePath(graph: Graph, start: GraphNode): List<LorenzVec>? {
        val startNeighbors = start.neighbors.entries.first()
        val list = mutableListOf<GraphNode>()
        list.add(start)

        var current = startNeighbors.key

        while (list.size != graph.size - 1) {
            val neighbors = current.neighbors.filter { it.key !in list }.keys

            if (neighbors.size > 1) {
                ChatUtils.userError("One node has more than two neighbors!")
                showErrorAt(current.position)
                return null
            }
            if (neighbors.isEmpty()) {
                ChatUtils.userError("One node has only one neighbor!")
                showErrorAt(current.position)
                return null
            }
            if (current.name == "end") {
                ChatUtils.userError("End node has two neighbors!")
                showErrorAt(current.position)
                return null
            }

            list.add(current)
            current = neighbors.first()
        }

        if (current.name != "end") {
            ChatUtils.userError("Last node does not have the name end!")
            showErrorAt(current.position)
            return null
        }

        list.add(current)
        return list.map { it.position }
    }

    private fun showErrorAt(vec: LorenzVec) {
        IslandGraphs.pathFind(vec, "Node error", LorenzColor.RED.toColor(), condition = ::isEnabled)
    }

    private fun loadParkour() {
        val locations = readListFromClipboard() ?: return
        val graph = listToGraph(locations)
        GraphEditor.enable()
        GraphEditorHistory.save("load parkour")
        GraphEditor.state = GraphEditorIO.createStateFrom(graph)
        IslandGraphs.pathFind(
            locations.first(),
            "Start of parkour",
            condition = ::isEnabled,
        )
        ChatUtils.chat("Graph Editor loaded a parkour from clipboard!")
    }

    private fun readListFromClipboard(): List<LorenzVec>? {
        val clipboard = OSUtils.readFromClipboard() ?: return null
        return clipboard.split("\n").map { line ->
            val raw = line.replace("\"", "").replace(",", "")
            raw.split(":").map { it.toDouble() }.toLorenzVec()
        }
    }

    private fun listToGraph(locations: List<LorenzVec>): Graph {
        val nodes = locations.mapIndexed { index, location ->
            val name = when (index) {
                0 -> "start"
                locations.size - 1 -> "end"
                else -> null
            }
            GraphNode(index, location, name = name).also {
                it.neighbors = emptyMap()
            }
        }

        for (node in nodes) {
            nodes.getOrNull(node.id - 1)?.let { previous ->
                val distance = previous.position.distance(node.position)
                addNeighbor(node, previous, distance)
                addNeighbor(previous, node, distance)
            }
            nodes.getOrNull(node.id + 1)?.let { next ->
                val distance = next.position.distance(node.position)
                addNeighbor(node, next, distance)
                addNeighbor(next, node, distance)
            }
        }

        return Graph(nodes)
    }

    private fun addNeighbor(a: GraphNode, b: GraphNode, distance: Double) {
        val neighbors = a.neighbors.toMutableMap()
        neighbors[b] = distance
        a.neighbors = neighbors
    }
}
