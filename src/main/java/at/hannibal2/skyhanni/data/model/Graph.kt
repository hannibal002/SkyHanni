package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.LorenzVec
import java.util.PriorityQueue

typealias Graph = List<GraphNode>

class GraphNode(val id: Int, val position: LorenzVec, val name: String? = null) {

    /** Keys are the neighbours and value the edge weight (e.g. Distance) */
    lateinit var neighbours: Map<GraphNode, Double>

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphNode

        if (id != other.id) return false

        return true
    }
}

fun Graph.findShortestPathAsGraph(start: GraphNode, end: GraphNode): Graph {
    val distances = mutableMapOf<GraphNode, Double>()
    val previous = mutableMapOf<GraphNode, GraphNode>()
    val visited = mutableSetOf<GraphNode>()
    val queue = PriorityQueue<GraphNode>(compareBy { distances.getOrDefault(it, Double.MAX_VALUE) })

    distances[start] = 0.0
    queue.add(start)

    while (queue.isNotEmpty()) {
        val current = queue.poll()
        if (current == end) break

        visited.add(current)

        current.neighbours.forEach { (neighbour, weight) ->
            if (neighbour !in visited) {
                val newDistance = distances.getValue(current) + weight
                if (newDistance < distances.getOrDefault(neighbour, Double.MAX_VALUE)) {
                    distances[neighbour] = newDistance
                    previous[neighbour] = current
                    queue.add(neighbour)
                }
            }
        }
    }

    return buildList {
        var current = end
        while (current != start) {
            add(current)
            current = previous[current] ?: return emptyList()
        }
        add(start)
    }.reversed()
}

fun Graph.findShortestPath(start: GraphNode, end: GraphNode): List<LorenzVec> =
    this.findShortestPathAsGraph(start, end).toPositionsList()

fun Graph.toPositionsList() = this.map { it.position }
