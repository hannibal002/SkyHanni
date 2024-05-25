package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraph
import at.hannibal2.skyhanni.data.model.toJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import kotlinx.coroutines.runBlocking
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

object GraphEditor {

    private val config get() = SkyHanniMod.feature.dev.devTool.graph

    private fun isEnabled() = config != null && config.enabled

    private var id = 0

    private val nodes = mutableListOf<GraphingNode>()
    private val edge = mutableListOf<GraphingEdge>()

    private var activeNode: GraphingNode? = null
    private var closedNode: GraphingNode? = null

    private var seeThroughBlocks = true

    private var inEditMode = false
    private var inTextMode = false
        set(value) {
            field = value
            if (value) {
                activeNode?.name?.let {
                    textBox.textBox = it
                }
                textBox.makeActive()
            } else {
                textBox.clear()
                textBox.disable()
            }
        }

    private val textBox = TextInput()

    private val nodeColor = LorenzColor.BLUE.addOpacity(200)
    private val activeColor = LorenzColor.GREEN.addOpacity(200)
    private val closedColor = LorenzColor.YELLOW.addOpacity(200)
    private val dijkstraColor = LorenzColor.LIGHT_PURPLE.addOpacity(200)

    private val edgeColor = LorenzColor.GOLD.addOpacity(150)
    private val edgeDijkstraColor = LorenzColor.DARK_BLUE.addOpacity(150)

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        nodes.forEach { event.drawNode(it) }
        edge.forEach { event.drawEdge(it) }
    }

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.infoDisplay.renderStrings(
            buildList {
                add("§eExit: §6${KeyboardManager.getKeyName(config.exitKey)}")
                if (!inEditMode && !inTextMode) {
                    add("§ePlace: §6${KeyboardManager.getKeyName(config.placeKey)}")
                    add("§eSelect: §6${KeyboardManager.getKeyName(config.selectKey)}")
                    add("§eConnect: §6${KeyboardManager.getKeyName(config.connectKey)}")
                    add("§eTest: §6${KeyboardManager.getKeyName(config.dijkstraKey)}")
                    add("§eVision: §6${KeyboardManager.getKeyName(config.throughBlocksKey)}")
                    add("§eSave: §6${KeyboardManager.getKeyName(config.saveKey)}")
                    add("§eLoad: §6${KeyboardManager.getKeyName(config.loadKey)}")
                    add("§eClear: §6${KeyboardManager.getKeyName(config.clearKey)}")
                    if (activeNode != null) add("§eText: §6${KeyboardManager.getKeyName(config.textKey)}")
                }
                if (!inTextMode && activeNode != null) {
                    add("§eEdit: §6${KeyboardManager.getKeyName(config.editKey)}")
                }
                if (inEditMode) {
                    add("§ex+ §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.w.keyCode)}")
                    add("§ex- §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.s.keyCode)}")
                    add("§ez+ §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.a.keyCode)}")
                    add("§ez- §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.d.keyCode)}")
                    add("§ey+ §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.up.keyCode)}")
                    add("§ey- §6${KeyboardManager.getKeyName(KeyboardManager.WasdInputMatrix.down.keyCode)}")
                }
                if (inTextMode) {
                    add("§eFormat: ${textBox.finalText()}")
                    add("§eRaw:     ${textBox.editText()}")
                }
            }, posLabel = "Graph Info"
        )
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        input()
        if (nodes.isEmpty()) return
        closedNode = nodes.minBy { it.position.distanceSqToPlayer() }
    }

    private fun LorenzRenderWorldEvent.drawNode(node: GraphingNode) {
        this.drawWaypointFilled(
            node.position,
            node.getNodeColor(),
            seeThroughBlocks = seeThroughBlocks,
            minimumAlpha = 0.2f,
            inverseAlphaScale = true
        )
        if (node.name == null) return
        this.drawString(node.position, node.name!!, seeThroughBlocks)
    }

    private fun LorenzRenderWorldEvent.drawEdge(edge: GraphingEdge) = this.draw3DLine_nea(
        edge.node1.position.add(0.5, 0.5, 0.5),
        edge.node2.position.add(0.5, 0.5, 0.5),
        if (edge !in highlightedEdges) edgeColor else edgeDijkstraColor,
        7,
        !seeThroughBlocks
    )

    private fun GraphingNode.getNodeColor() = when (this) {
        activeNode -> if (this == closedNode) ColorUtils.blendRGB(activeColor, closedColor, 0.5) else activeColor
        closedNode -> closedColor
        in highlightedNodes -> dijkstraColor
        else -> nodeColor
    }

    fun commandIn() {
        config.enabled = !config.enabled
        if (config.enabled) {
            ChatUtils.chat("Graph Editor is now active")
        } else {
            chatAtDisable()
        }
    }

    private fun chatAtDisable() =
        ChatUtils.clickableChat("Graph Editor is now inactive. §lClick to activate", ::commandIn)

    private fun input() {
        if (LorenzUtils.isAnyGuiActive()) return
        if (config.exitKey.isKeyClicked()) {
            if (inTextMode) {
                inTextMode = false
                return
            }
            if (inEditMode) {
                inEditMode = false
                return
            }
            config.enabled = false
            chatAtDisable()
        }
        if (inTextMode) {
            textBox.handle()
            val text = textBox.finalText()
            if (text.isEmpty()) {
                activeNode?.name = null
            } else {
                activeNode?.name = text
            }
            return
        }
        if (activeNode != null && config.textKey.isKeyClicked()) {
            inTextMode = true
            return
        }
        if (inEditMode) {
            editModeClicks()
            inEditMode = false
        }
        if (activeNode != null && config.editKey.isKeyHeld()) {
            inEditMode = true
            return
        }
        if (config.saveKey.isKeyClicked()) {
            if (nodes.isEmpty()) {
                ChatUtils.chat("Copied nothing since the graph is empty")
                return
            }
            OSUtils.copyToClipboard(compileGraph().toJson())
            ChatUtils.chat("Copied Graph to Clipboard")
            return
        }
        if (config.loadKey.isKeyClicked()) {
            runBlocking {
                OSUtils.readFromClipboard()?.let {
                    try {
                        Graph.fromJson(it)
                    } catch (e: Exception) {
                        ErrorManager.logErrorWithData(
                            e,
                            "Import of graph failed",
                            "json" to it,
                            ignoreErrorCache = true
                        )
                        null
                    }
                }?.let { import(it) }
            }
            return
        }
        if (config.clearKey.isKeyClicked()) {
            OSUtils.copyToClipboard(compileGraph().toJson())
            ChatUtils.chat("Copied Graph to Clipboard and cleared the graph")
            clear()
        }
        if (config.placeKey.isKeyClicked()) {
            addNode()
        }
        if (config.selectKey.isKeyClicked()) {
            activeNode = if (activeNode == closedNode) null else closedNode
        }
        if (activeNode != closedNode && config.connectKey.isKeyClicked()) {
            val edge = getEdgeIndex(activeNode, closedNode)
            if (edge == null) {
                addEdge(activeNode, closedNode)
            } else {
                this.edge.removeAt(edge)
            }
        }
        if (config.throughBlocksKey.isKeyClicked()) {
            seeThroughBlocks = !seeThroughBlocks
        }
        if (config.dijkstraKey.isKeyClicked()) {
            testDijkstra()
        }
    }

    private fun editModeClicks() {
        KeyboardManager.WasdInputMatrix.w.handleEditClicks(x = 1)
        KeyboardManager.WasdInputMatrix.s.handleEditClicks(x = -1)
        KeyboardManager.WasdInputMatrix.a.handleEditClicks(z = 1)
        KeyboardManager.WasdInputMatrix.d.handleEditClicks(z = -1)
        KeyboardManager.WasdInputMatrix.up.handleEditClicks(y = 1)
        KeyboardManager.WasdInputMatrix.down.handleEditClicks(y = -1)
    }

    private fun KeyBinding.handleEditClicks(x: Int = 0, y: Int = 0, z: Int = 0) {
        if (this.keyCode.isKeyClicked()) {
            activeNode?.position = activeNode?.position?.add(x, y, z) ?: return
        }
    }

    fun onMinecraftInput(keyBinding: KeyBinding, cir: CallbackInfoReturnable<Boolean>) {
        if (!isEnabled()) return
        if (!inEditMode) return
        if (keyBinding !in KeyboardManager.WasdInputMatrix) return
        cir.returnValue = false
    }

    private fun addNode() {
        val closedNode = closedNode
        if (closedNode != null && closedNode.position.distanceSqToPlayer() < 9.0) {
            nodes.remove(closedNode)
            edge.removeIf { it.isInEdge(closedNode) }
            if (closedNode == activeNode) activeNode = null
            this.closedNode = null
            return
        }
        val position = LocationUtils.playerLocation().round(0)
        val node = GraphingNode(id++, position)
        nodes.add(node)
        if (activeNode == null) return
        addEdge(activeNode, node)
    }

    private fun getEdgeIndex(node1: GraphingNode?, node2: GraphingNode?) =
        if (node1 != null && node2 != null && node1 != node2) GraphingEdge(
            node1,
            node2
        ).let { e -> edge.indexOfFirst { it == e }.takeIf { it != -1 } }
        else null

    private fun addEdge(node1: GraphingNode?, node2: GraphingNode?) =
        if (node1 != null && node2 != null && node1 != node2) edge.add(GraphingEdge(node1, node2)) else false

    /** Has a side effect on the graphing graph, since it runs [prune] on the graphing graph*/
    private fun compileGraph(): Graph {
        prune()
        val indexedTable = nodes.mapIndexed { index, node -> node.id to index }.toMap()
        val nodes = nodes.mapIndexed { index, it -> GraphNode(index, it.position, it.name) }
        val neighbours = this.nodes.map { node ->
            edge.filter { it.isInEdge(node) }.map { edge ->
                val otherNode = if (node == edge.node1) edge.node2 else edge.node1
                nodes[indexedTable[otherNode.id]!!] to node.position.distance(otherNode.position)
            }.sortedBy { it.second }
        }
        nodes.forEachIndexed { index, it -> it.neighbours = neighbours[index].toMap() }
        return Graph(nodes)
    }

    fun import(graph: Graph) {
        clear()
        nodes.addAll(graph.map { GraphingNode(it.id, it.position, it.name) })
        val translation = graph.mapIndexed { index, it -> it to nodes[index] }.toMap()
        edge.addAll(
            graph.map { node ->
                node.neighbours.map { GraphingEdge(translation[node]!!, translation[it.key]!!) }
            }.flatten().distinct()
        )
        id = nodes.lastOrNull()?.id?.plus(1) ?: 0
    }

    private val highlightedNodes = mutableSetOf<GraphingNode>()
    private val highlightedEdges = mutableSetOf<GraphingEdge>()

    private fun testDijkstra() {

        val savedCurrent = closedNode ?: return
        val savedActive = activeNode ?: return

        val compiled = compileGraph()
        import(compiled)
        highlightedEdges.clear()
        highlightedNodes.clear()

        val current = compiled.firstOrNull { it.position == savedCurrent.position } ?: return
        val goal = compiled.firstOrNull { it.position == savedActive.position } ?: return

        val path = compiled.findShortestPathAsGraph(current, goal)

        val inGraph = path.map { nodes[it.id] }
        highlightedNodes.addAll(inGraph)

        val edge = edge.filter { highlightedNodes.contains(it.node1) && highlightedNodes.contains(it.node2) }
        highlightedEdges.addAll(edge)
    }

    private fun clear() {
        id = 0
        nodes.clear()
        edge.clear()
        activeNode = null
        closedNode = null
    }

    private fun prune() { //TODO fix
        val hasNeighbours = nodes.associateWith { false }.toMutableMap()
        edge.forEach {
            hasNeighbours[it.node1] = true
            hasNeighbours[it.node2] = true
        }
        nodes.removeIf { hasNeighbours[it] == false }
    }
}

private class GraphingNode(val id: Int, var position: LorenzVec, var name: String? = null) {

    override fun hashCode(): Int {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphingNode

        if (id != other.id) return false

        return true
    }
}

private class GraphingEdge(val node1: GraphingNode, val node2: GraphingNode) {

    fun isInEdge(node: GraphingNode) = node1 == node || node2 == node

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphingEdge

        return (this.node1 == other.node1 && this.node2 == other.node2) || (this.node1 == other.node2 && this.node2 == other.node1)
    }

    override fun hashCode(): Int {
        val hash1 = node1.hashCode()
        val hash2 = node2.hashCode()

        var result: Int
        if (hash1 <= hash2) {
            result = hash1
            result = 31 * result + hash2
        } else {
            result = hash2
            result = 31 * result + hash1
        }
        return result
    }

}

