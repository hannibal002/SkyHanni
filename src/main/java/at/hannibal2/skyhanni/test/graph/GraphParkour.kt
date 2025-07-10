package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.graph.GraphEditor.isEnabled
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.LorenzVec.Companion.toLorenzVec
import at.hannibal2.skyhanni.utils.OSUtils

@SkyHanniModule
object GraphParkour {

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shgraphloadparkour") {
            description = "Loads the current clipboard as parkour into the graph editor."
            category = CommandCategory.DEVELOPER_TEST
            callback {
                SkyHanniMod.launchCoroutine {
                    loadParkour()
                }
            }
        }
        event.register("shgraphexportasparkour") {
            description = "Saves the graph editor as parkour into the clipboard."
            category = CommandCategory.DEVELOPER_TEST
            callback { saveParkour() }
        }
    }

    private fun saveParkour() {
        val graph = GraphEditor.compileGraph()

        val list = graphToList(graph) ?: return

        val resultList = mutableListOf<String>()
        for (location in list) {
            val x = location.x.toString().replace(",", ".")
            val y = location.y.toString().replace(",", ".")
            val z = location.z.toString().replace(",", ".")
            resultList.add("\"$x:$y:$z\"".replace(".0", ""))
        }
        OSUtils.copyToClipboard(resultList.joinToString((",\n")))
        ChatUtils.chat("Saved graph as parkour to clipboard!")
    }

    private fun graphToList(graph: Graph): List<LorenzVec>? {
        val starts = graph.nodes.filter { it.name == "start" }
        if (starts.isEmpty()) {
            ChatUtils.userError("No start node found!")
            return null
        }
        if (starts.size > 1) {
            ChatUtils.userError("More than one start node found!")
            return null
        }
        val ends = graph.nodes.filter { it.name == "end" }
        if (ends.isEmpty()) {
            ChatUtils.userError("No end node found!")
            return null
        }
        if (ends.size > 1) {
            ChatUtils.userError("More than one end node found!")
            return null
        }

        val start = starts.first()
        val startN = start.neighbours.entries
        if (startN.isEmpty()) {
            ChatUtils.userError("Start has no neighbours!")
            return null
        }
        if (startN.size != 1) {
            ChatUtils.userError("Start has more than one neighbours!")
            return null
        }

        val list = mutableListOf<GraphNode>()
        list.add(start)

        var current = startN.first().key

        while (list.size != graph.nodes.size - 1) {
            val neighbours = current.neighbours.filter { it.key !in list }.keys
            if (neighbours.size > 1) {
                ChatUtils.userError("One node has more than two neighbours!")
                showErrorAt(current.position)
                return null
            }
            if (neighbours.isEmpty()) {
                ChatUtils.userError("One node has only one neighbour!")
                showErrorAt(current.position)
                return null
            }
            if (current.name == "end") {
                ChatUtils.userError("End node has two neighbours!")
                showErrorAt(current.position)
                return null
            }
            list.add(current)
            current = neighbours.first()
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
        IslandGraphs.pathFind(
            vec, "Node error",
            LorenzColor.RED.toColor(),
            condition = { isEnabled() },
        )
    }

    private suspend fun loadParkour() {
        val locations = readListFromClipboard() ?: return
        val graph = listToGraph(locations)
        GraphEditor.enable()
        GraphEditor.import(graph)
        IslandGraphs.pathFind(
            locations.first(),
            "Start of parkour",
            condition = { isEnabled() },
        )
        ChatUtils.chat("Graph Editor loaded a parkour from clipboard!")
    }

    private suspend fun readListFromClipboard(): List<LorenzVec>? {
        val clipboard = OSUtils.readFromClipboard() ?: return null
        return clipboard.split("\n").map { line ->
            val raw = line.replace("\"", "").replace(",", "")
            raw.split(":").map { it.toDouble() }.toLorenzVec()
        }
    }

    private fun listToGraph(locations: List<LorenzVec>): Graph {
        val nodes = mutableListOf<GraphNode>()

        for ((index, location) in locations.withIndex()) {
            val name = when (index) {
                0 -> "start"
                locations.size - 1 -> "end"
                else -> null
            }
            nodes.add(
                GraphNode(index, location, name = name).also {
                    it.neighbours = emptyMap()
                },
            )
        }

        for (node in nodes) {
            nodes.getOrNull(node.id - 1)?.let { previous ->
                val distance = previous.position.distance(node.position)
                addNeighbour(node, previous, distance)
                addNeighbour(previous, node, distance)
            }
            nodes.getOrNull(node.id + 1)?.let { next ->
                val distance = next.position.distance(node.position)
                addNeighbour(node, next, distance)
                addNeighbour(next, node, distance)
            }
        }

        return Graph(nodes)
    }

    private fun addNeighbour(a: GraphNode, b: GraphNode, distance: Double) {
        val neighbours = mutableMapOf<GraphNode, Double>()
        neighbours.putAll(a.neighbours)
        neighbours[b] = distance
        a.neighbours = neighbours
    }
}
