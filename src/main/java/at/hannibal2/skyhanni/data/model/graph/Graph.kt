package at.hannibal2.skyhanni.data.model.graph

import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.util.Collection
import java.util.function.IntFunction
import kotlin.collections.iterator

// TODO: This class should be disambiguated into a NodePath and a Graph class
@JvmInline
@Suppress("TooManyFunctions")
value class Graph(
    @Expose private val nodes: List<GraphNode>,
) : List<GraphNode> by nodes {

    constructor() : this(emptyList())

    fun getNodesWithTags(vararg tag: GraphNodeTag): List<GraphNode> =
        nodes.filter { node -> tag.all { node.hasTag(it) } }
    fun getNodesWithName(name: String): List<GraphNode> =
        nodes.filter { it.name == name }
    fun getNodesWithNameAndTags(name: String, tag: GraphNodeTag): List<GraphNode> =
        getNodesWithTags(tag).filter { it.name == name }
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
    fun minByActive(selector: (GraphNode) -> Double): GraphNode =
        nodes.filter { it.enabled }.minBy(selector)
    fun filterByActive(predicate: (GraphNode) -> Boolean): List<GraphNode> =
        asSequence().filter(predicate).filter { it.enabled }.toList()
    fun getNearestNode(
        location: LorenzVec = GraphUtils.playerPosition,
        condition: (GraphNode) -> Boolean = { true },
    ): GraphNode = filterByActive(condition).minBy { it.position.distanceSq(location) }
    fun toPositionsList() = map { it.position }
    fun toJson(): String = gson.toJson(this)

    @Deprecated("See parent deprecation")
    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun <T> toArray(generator: IntFunction<Array<T>>): Array<T> =
        (nodes as Collection<GraphNode>).toArray(generator)

    companion object {
        /** Exposed so [at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters] can register it without
         *  pulling in the full base GsonBuilder. */
        val typeAdapter: TypeAdapter<Graph> = object : TypeAdapter<Graph>() {
            override fun write(out: JsonWriter, value: Graph) = serializeGraph(out, value)
            override fun read(reader: JsonReader) = deserializeGraph(reader)
        }

        // Minimal Gson for graph files — deliberately does not use the base builder
        // (no @Expose filtering, no config adapters).
        val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Graph::class.java, typeAdapter.nullSafe())
            .create()

        fun fromJson(json: String): Graph = gson.fromJson<Graph>(json)
        fun fromJson(json: JsonElement): Graph = gson.fromJson<Graph>(json)

        private fun serializeGraph(out: JsonWriter, value: Graph) {
            out.beginObject()
            for (node in value) {
                out.name(node.id.toString()).beginObject()
                out.name("Position").value(with(node.position) { "$x:$y:$z" })
                node.name?.let { out.name("Name").value(it) }
                node.tagNames.takeIf { it.isNotEmpty() }?.let {
                    out.name("Tags").beginArray()
                    it.forEach(out::value)
                    out.endArray()
                }
                if (node.extraWeight != 0) out.name("ExtraWeight").value(node.extraWeight)
                out.name("Neighbours").beginObject()
                for ((neighbour, weight) in node.neighbours) {
                    out.name(neighbour.id.toString()).value(weight.roundTo(2))
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
                    "Position" -> data.position = reader.nextString().split(":").let {
                        LorenzVec(it[0].toDouble(), it[1].toDouble(), it[2].toDouble())
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
            while (reader.hasNext()) neighbors.add(reader.nextName().toInt() to reader.nextDouble())
            reader.endObject()
        }

        private fun parseTags(reader: JsonReader): List<String> {
            val tags = mutableListOf<String>()
            reader.beginArray()
            while (reader.hasNext()) tags.add(reader.nextString())
            reader.endArray()
            return tags
        }

        private fun linkNeighbours(nodes: List<GraphNode>, neighbourMap: Map<GraphNode, List<Pair<Int, Double>>>) {
            val lookup = nodes.associateBy { it.id }
            for ((node, edges) in neighbourMap) {
                node.neighbours = edges.associate { (id, distance) ->
                    (lookup[id] ?: error("Node ${node.id} references non-existent neighbor $id")) to distance
                }
            }
        }
    }
}
