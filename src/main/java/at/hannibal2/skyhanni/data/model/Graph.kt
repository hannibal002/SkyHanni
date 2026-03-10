package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.features.misc.pathfind.NavigationHelper
import at.hannibal2.skyhanni.test.graph.GraphEditor
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters.registerTypeAdapter
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

// TODO: This class should be disambiguated into a NodePath and a Graph class
@JvmInline
@Suppress("TooManyFunctions")
value class Graph(
    @Expose private val nodes: List<GraphNode>,
) : List<GraphNode> {
    override val size
        get() = nodes.size

    override fun contains(element: GraphNode) = nodes.contains(element)

    override fun containsAll(elements: Collection<GraphNode>) = nodes.containsAll(elements)

    override fun get(index: Int) = nodes[index]

    override fun isEmpty() = nodes.isEmpty()

    override fun indexOf(element: GraphNode) = nodes.indexOf(element)

    override fun iterator(): Iterator<GraphNode> = nodes.iterator()
    override fun listIterator() = nodes.listIterator()

    override fun listIterator(index: Int) = nodes.listIterator(index)

    override fun subList(fromIndex: Int, toIndex: Int) = nodes.subList(fromIndex, toIndex)

    override fun lastIndexOf(element: GraphNode) = nodes.lastIndexOf(element)

    fun getNodesWithTags(vararg tag: GraphNodeTag): List<GraphNode> = nodes.filter { node -> tag.all { node.hasTag(it) } }
    fun getNodesWithName(name: String): List<GraphNode> = nodes.filter { it.name == name }
    fun getNodesWithNameAndTags(name: String, tag: GraphNodeTag): List<GraphNode> = getNodesWithTags(tag).filter { it.name == name }

    fun getClosestNode(nodeName: String, tag: GraphNodeTag): GraphNode? =
        getNodesWithNameAndTags(nodeName, tag).minByOrNull { it.position.distanceToPlayer() }

    fun nodesAround(node: GraphNode, condition: (GraphNode) -> Boolean): Set<GraphNode> {
        val visited = mutableSetOf<GraphNode>()
        val queue = ArrayDeque<GraphNode>()
        queue.add(node)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbour in current.neighbours.keys) {
                if (!condition(neighbour) || neighbour in visited) continue
                visited.add(neighbour)
                queue.add(neighbour)
            }
        }
        return visited
    }

    fun minByActive(selector: (GraphNode) -> Double): GraphNode = nodes.filter { it.enabled }.minBy(selector)

    fun filterByActive(predicate: (GraphNode) -> Boolean): List<GraphNode> = asSequence().filter(predicate).filter { it.enabled }.toList()

    fun getNearestNode(
        location: LorenzVec = GraphUtils.playerPosition,
        condition: (GraphNode) -> Boolean = { true },
    ): GraphNode =
        filterByActive(condition).minBy { it.position.distanceSq(location) }

    constructor() : this(emptyList())

    // todo clean this up
    companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().registerTypeAdapter<Graph>(
            { out, value -> serializeGraph(out, value) },
            { reader -> deserializeGraph(reader) },
        ).create()

        private fun serializeGraph(out: JsonWriter, value: Graph) {
            out.beginObject()
            for (graphNode in value) {
                out.name(graphNode.id.toString()).beginObject()

                out.name("Position").value(with(graphNode.position) { "$x:$y:$z" })

                graphNode.name?.let {
                    out.name("Name").value(it)
                }

                graphNode.tagNames.takeIf { list -> list.isNotEmpty() }?.let {
                    out.name("Tags")
                    out.beginArray()
                    for (tagName in it) {
                        out.value(tagName)
                    }
                    out.endArray()
                }

                if (graphNode.extraWeight != 0) {
                    out.name("ExtraWeight").value(graphNode.extraWeight)
                }

                out.name("Neighbours")
                out.beginObject()
                for ((node, weight) in graphNode.neighbours) {
                    out.name(node.id.toString()).value(weight.roundTo(2))
                }
                out.endObject()

                out.endObject()
            }
            out.endObject()
        }

        private fun deserializeGraph(reader: JsonReader): Graph {
            reader.beginObject()
            val (nodes, neighbourMap) = parseNodes(reader)
            reader.endObject()

            linkNeighbours(nodes, neighbourMap)
            return Graph(nodes)
        }

        private fun parseNodes(reader: JsonReader): Pair<List<GraphNode>, Map<GraphNode, List<Pair<Int, Double>>>> {
            val list = mutableListOf<GraphNode>()
            val neighbourMap = mutableMapOf<GraphNode, List<Pair<Int, Double>>>()

            while (reader.hasNext()) {
                if (reader.peek() != JsonToken.NAME) {
                    reader.skipValue()
                    continue
                }

                val id = reader.nextName().toIntOrNull() ?: run {
                    reader.skipValue()
                    continue
                }

                reader.beginObject()
                val nodeData = parseNodeData(reader)
                reader.endObject()

                nodeData.position?.let { pos ->
                    val node = GraphNode(id, pos, nodeData.name, nodeData.tags, nodeData.extraWeight)
                    list.add(node)
                    neighbourMap[node] = nodeData.neighbors
                }
            }

            return list to neighbourMap
        }

        private data class NodeData(
            var position: LorenzVec? = null,
            var name: String? = null,
            var tags: List<String> = emptyList(),
            val neighbors: MutableList<Pair<Int, Double>> = mutableListOf(),
            var extraWeight: Int = 0,
        )

        private fun parseNodeData(reader: JsonReader): NodeData {
            val data = NodeData()

            while (reader.hasNext()) {
                if (reader.peek() != JsonToken.NAME) {
                    reader.skipValue()
                    continue
                }

                when (reader.nextName()) {
                    "Position" -> {
                        data.position = reader.nextString().split(":").let { parts ->
                            LorenzVec(parts[0].toDouble(), parts[1].toDouble(), parts[2].toDouble())
                        }
                    }

                    "ExtraWeight" -> data.extraWeight = reader.nextInt()
                    "Neighbours" -> parseNeighbours(reader, data.neighbors)
                    "Name" -> data.name = reader.nextString()
                    "Tags" -> data.tags = parseTags(reader)
                }
            }

            return data
        }

        private fun parseNeighbours(reader: JsonReader, neighbors: MutableList<Pair<Int, Double>>) {
            reader.beginObject()
            while (reader.hasNext()) {
                val nId = reader.nextName().toInt()
                val distance = reader.nextDouble()
                neighbors.add(nId to distance)
            }
            reader.endObject()
        }

        private fun parseTags(reader: JsonReader): List<String> {
            val tags = mutableListOf<String>()
            reader.beginArray()
            while (reader.hasNext()) {
                tags.add(reader.nextString())
            }
            reader.endArray()
            return tags
        }

        private fun linkNeighbours(nodes: List<GraphNode>, neighbourMap: Map<GraphNode, List<Pair<Int, Double>>>) {
            val nodeLookup = nodes.associateBy { it.id }

            for ((node, edges) in neighbourMap) {
                node.neighbours = edges.associate { (id, distance) ->
                    val neighbor = nodeLookup[id] ?: error("Node ${node.id} references non-existent neighbor $id")
                    neighbor to distance
                }
            }
        }

        fun fromJson(json: String): Graph = gson.fromJson<Graph>(json)
        fun fromJson(json: JsonElement): Graph = gson.fromJson<Graph>(json)
    }

    fun toPositionsList() = this.map { it.position }

    fun toJson(): String = gson.toJson(this)
}

// The node object that gets parsed from/to JSON
class GraphNode(
    val id: Int,
    override val position: LorenzVec,
    val name: String? = null,
    val tagNames: List<String> = emptyList(),
    val extraWeight: Int = 0,
) :
    GraphUtils.GenericNode {

    val tags: List<GraphNodeTag> by lazy {
        tagNames.mapNotNull { GraphNodeTag.byId(it) }
    }

    var enabled = true
        set(value) {
            if (value != field) {
                GraphEditor.flagDisabledDirty()
            }
            field = value
        }

    /** Keys are the neighbours and value the edge weight (e.g. Distance) */
    lateinit var neighbours: Map<GraphNode, Double>

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphNode

        return id == other.id
    }

    fun sameNameAndTags(other: GraphNode): Boolean = name == other.name && allowedTags == other.allowedTags

    fun hasTag(tag: GraphNodeTag): Boolean = tag in tags

    private val allowedTags get() = tags.filter { it in NavigationHelper.allowedTags }
}

data class DijkstraTree(
    val origin: GraphNode,
    /**
     * A map of distances between the [origin] and each node in a graph. This distance map is only accurate for nodes closer to the
     * origin than the [lastVisitedNode]. In case there is no early bailout, this map will be accurate for all nodes in the graph.
     */
    val distances: Map<GraphNode, Double>,
    /**
     * A map of nodes to the neighbouring node that is the quickest path towards the origin (the neighbouring node that has the lowest value
     * in [distances])
     */
    val towardsOrigin: Map<GraphNode, GraphNode>,
    /**
     * This is either the furthest away node in the graph, or the node that was bailed out on early because it fulfilled the search
     * condition. In case the search condition matches nothing, this will *still* be the furthest away node, so an additional check might be
     * necessary.
     */
    val lastVisitedNode: GraphNode,
)

@Suppress("MapGetWithNotNullAssertionOperator")
fun DijkstraTree.findPathToDestination(end: GraphNode): Pair<Graph, Double> {
    val distances = this
    val reversePath = buildList {
        var current = end
        while (true) {
            add(current)
            if (current == distances.origin) break
            current = distances.towardsOrigin[current] ?: return Graph() to 0.0
        }
    }
    return Graph(reversePath.reversed()) to distances.distances[end]!!
}
