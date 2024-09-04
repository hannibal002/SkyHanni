package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeCategory
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.data.model.findShortestPathAsGraph
import at.hannibal2.skyhanni.data.model.toJson
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addString
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine_nea
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import kotlinx.coroutines.runBlocking
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GraphEditor {

    private val config get() = SkyHanniMod.feature.dev.devTool.graph

    private fun isEnabled() = config != null && config.enabled

    private var id = 0

    private val nodes = mutableListOf<GraphingNode>()
    private val edges = mutableListOf<GraphingEdge>()

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

    private var inTutorialMode = false

    private val textBox = TextInput()

    private val nodeColor = LorenzColor.BLUE.addOpacity(200)
    private val activeColor = LorenzColor.GREEN.addOpacity(200)
    private val closedColor = LorenzColor.YELLOW.addOpacity(200)
    private val dijkstraColor = LorenzColor.LIGHT_PURPLE.addOpacity(200)

    private val edgeColor = LorenzColor.GOLD.addOpacity(150)
    private val edgeDijkstraColor = LorenzColor.DARK_BLUE.addOpacity(150)

    val scrollValue = ScrollValue()
    var nodesDisplay = emptyList<Renderable>()
    var lastUpdate = SimpleTimeMark.farPast()

    @SubscribeEvent
    fun onRender(event: LorenzRenderWorldEvent) {
        if (!isEnabled()) return
        nodes.forEach { event.drawNode(it) }
        edges.forEach { event.drawEdge(it) }
    }

    @SubscribeEvent
    fun onOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.infoDisplay.renderStrings(buildDisplay(), posLabel = "Graph Info")
    }

    private fun buildDisplay(): List<String> = buildList {
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
            add("§eTutorial: §6${KeyboardManager.getKeyName(config.tutorialKey)}")
            add(" ")
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
            add("§eRaw:     ${textBox.editText(textColor = LorenzColor.YELLOW)}")
        }
    }

    @SubscribeEvent
    fun onGuiRender(event: GuiRenderEvent) {
        if (!isEnabled()) return
        config.namedNodesList.renderRenderables(
            buildList {
                val list = getNodeNames()
                val size = list.size
                addString("§eGraph Nodes: $size")
                val height = (size * 10).coerceAtMost(150)
                if (list.isNotEmpty()) {
                    add(Renderable.scrollList(list, height, scrollValue = scrollValue, velocity = 10.0))
                }
            },
            posLabel = "Graph Nodes List",
        )
    }

    private fun getNodeNames(): List<Renderable> {
        if (lastUpdate.passedSince() > 250.milliseconds) {
            updateNodeNames()
        }
        return nodesDisplay
    }

    private fun updateNodeNames() {
        lastUpdate = SimpleTimeMark.now()
        nodesDisplay = drawNodeNames()
    }

    private fun updateCategoryView(node: GraphingNode) {
        lastUpdate = SimpleTimeMark.now() + 60.seconds
        nodesDisplay = drawCategoryNames(node)
    }

    private fun drawCategoryNames(node: GraphingNode): List<Renderable> = buildList {
        addString("§eChange category for node '${node.name}§e'")
        addString("")

        for (category in GraphNodeCategory.entries) {
            val state = if (category in node.categories) "§aYES" else "§cNO"
            val name = state + " §r" + category.displayName
            add(createCategoryName(name, category, node))
        }
        addString(".")
        add(
            Renderable.clickAndHover(
                "§cGo Back!",
                tips = listOf("§eClick to go back to the node list!"),
                onClick = {
                    updateNodeNames()
                },
            ),
        )
    }

    private fun createCategoryName(
        name: String,
        category: GraphNodeCategory,
        node: GraphingNode,
    ) = Renderable.clickAndHover(
        name,
        tips = listOf(
            "Category ${category.name}",
            "§7${category.description}",
            "",
            "§eClick to set category for ${node.name} to ${category.name}!",
        ),
        onClick = {
            if (category in node.categories) {
                node.categories.remove(category)
            } else {
                node.categories.add(category)
            }
            updateCategoryView(node)
        },
    )

    private fun drawNodeNames(): List<Renderable> = buildList {
        for ((node, distance: Double) in nodes.map { it to it.position.distanceSqToPlayer() }.sortedBy { it.second }) {
            val name = node.name?.takeIf { !it.isBlank() } ?: continue
            val color = if (node == activeNode) "§a" else "§7"
            val distanceFormat = sqrt(distance).toInt().addSeparators()
            val categoryText = node.categories.let {
                if (it.isEmpty()) {
                    " §cNo tag§r"
                } else {
                    val text = node.categories.map { it.internalName }.joinToString(", ")
                    " §f($text)"
                }
            }

            val text = "${color}Node §r$name$categoryText §7[$distanceFormat]"
            add(createNodeTextLine(text, name, node))
        }
    }

    private fun MutableList<Renderable>.createNodeTextLine(
        text: String,
        name: String,
        node: GraphingNode,
    ): Renderable = Renderable.clickAndHover(
        text,
        tips = buildList {
            add("Node '$name'")
            add("")

            if (node.categories.isNotEmpty()) {
                add("Categories: ")
                for (category in node.categories) {
                    add(" §8- §r${category.displayName}")
                }
                add("")
            }

            add("§eClick to select/deselect this node!")
            add("§eControl-Click to edit the categories for this node!")

        },
        onClick = {
            if (KeyboardManager.isModifierKeyDown()) {
                updateCategoryView(node)
            } else {
                activeNode = node
                updateNodeNames()
            }
        },
    )

    private fun feedBackInTutorial(text: String) {
        if (inTutorialMode) {
            ChatUtils.chat(text)
        }
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
            inverseAlphaScale = true,
        )

        val nodeName = node.name ?: return
        this.drawDynamicText(
            node.position,
            nodeName,
            0.8,
            ignoreBlocks = seeThroughBlocks || node.position.distanceSqToPlayer() < 100,
            smallestDistanceVew = 12.0,
            ignoreY = true,
            yOff = -15f,
            maxDistance = 80,
        )

        val categories = node.categories
        if (categories.isEmpty()) return
        val categoryText = categories.map { it.displayName }.joinToString(" §f+ ")
        this.drawDynamicText(
            node.position,
            categoryText,
            0.8,
            ignoreBlocks = seeThroughBlocks || node.position.distanceSqToPlayer() < 100,
            smallestDistanceVew = 12.0,
            ignoreY = true,
            yOff = 0f,
            maxDistance = 80,
        )
    }

    private fun LorenzRenderWorldEvent.drawEdge(edge: GraphingEdge) = this.draw3DLine_nea(
        edge.node1.position.add(0.5, 0.5, 0.5),
        edge.node2.position.add(0.5, 0.5, 0.5),
        if (edge !in highlightedEdges) edgeColor else edgeDijkstraColor,
        7,
        !seeThroughBlocks,
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
            ChatUtils.chat("Graph Editor is now active.")
        } else {
            chatAtDisable()
        }
    }

    private fun chatAtDisable() =
        ChatUtils.clickableChat("Graph Editor is now inactive. §lClick to activate.", ::commandIn)

    private fun input() {
        if (LorenzUtils.isAnyGuiActive()) return
        if (config.exitKey.isKeyClicked()) {
            if (inTextMode) {
                inTextMode = false
                feedBackInTutorial("Exited Text Mode.")
                return
            }
            if (inEditMode) {
                inEditMode = false
                feedBackInTutorial("Exited Edit Mode.")
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
            feedBackInTutorial("Entered Text Mode.")
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
            save()
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
                            "Import of graph failed.",
                            "json" to it,
                            ignoreErrorCache = true,
                        )
                        null
                    }
                }?.let {
                    import(it)
                    ChatUtils.chat("Loaded Graph from clip board.")
                }
            }
            return
        }
        if (config.clearKey.isKeyClicked()) {
            val json = compileGraph().toJson()
            OSUtils.copyToClipboard(json)
            ChatUtils.chat("Copied Graph to Clipboard and cleared the graph.")
            clear()
        }
        if (config.placeKey.isKeyClicked()) {
            addNode()
        }
        if (config.selectKey.isKeyClicked()) {
            activeNode = if (activeNode == closedNode) {
                feedBackInTutorial("De selected active node.")
                null
            } else {
                feedBackInTutorial("Selected new active node.")
                closedNode
            }
        }
        if (activeNode != closedNode && config.connectKey.isKeyClicked()) {
            val edge = getEdgeIndex(activeNode, closedNode)
            if (edge == null) {
                addEdge(activeNode, closedNode)
                feedBackInTutorial("Added new edge.")
            } else {
                this.edges.removeAt(edge)
                feedBackInTutorial("Removed edge.")
            }
        }
        if (config.throughBlocksKey.isKeyClicked()) {
            seeThroughBlocks = !seeThroughBlocks
            feedBackInTutorial(
                if (seeThroughBlocks) "Graph is visible though walls." else "Graph is invisible behind walls.",
            )
        }
        if (config.dijkstraKey.isKeyClicked()) {
            feedBackInTutorial("Calculated shortest route and cleared active node.")
            testDijkstra()
        }
        if (config.tutorialKey.isKeyClicked()) {
            inTutorialMode = !inTutorialMode
            ChatUtils.chat("Tutorial mode is now ${if (inTutorialMode) "active" else "inactive"}.")
        }
    }

    private fun save() {
        if (nodes.isEmpty()) {
            ChatUtils.chat("Copied nothing since the graph is empty.")
            return
        }
        val json = compileGraph().toJson()
        OSUtils.copyToClipboard(json)
        ChatUtils.chat("Copied Graph to Clipboard.")
        if (config.showsStats) {
            val length = edges.sumOf { it.node1.position.distance(it.node2.position) }.toInt().addSeparators()
            ChatUtils.chat(
                "§lStats\n" +
                    "§eNamed Nodes: ${nodes.filter { it.name != null }.size.addSeparators()}\n" +
                    "§eNodes: ${nodes.size.addSeparators()}\n" +
                    "§eEdges: ${edges.size.addSeparators()}\n" +
                    "§eLength: $length",
            )
        }
    }

    private fun editModeClicks() {
        var vector = LocationUtils.calculatePlayerFacingDirection()
        KeyboardManager.WasdInputMatrix.w.handleEditClicks(vector)
        KeyboardManager.WasdInputMatrix.a.handleEditClicks(vector.rotateXZ(Math.toRadians(90.0)))
        KeyboardManager.WasdInputMatrix.s.handleEditClicks(vector.rotateXZ(Math.toRadians(180.0)))
        KeyboardManager.WasdInputMatrix.d.handleEditClicks(vector.rotateXZ(Math.toRadians(270.0)))

        KeyboardManager.WasdInputMatrix.up.handleEditClicks(LorenzVec(0, 1, 0))
        KeyboardManager.WasdInputMatrix.down.handleEditClicks(LorenzVec(0, -1, 0))
    }

    private fun KeyBinding.handleEditClicks(vector: LorenzVec) {
        if (this.keyCode.isKeyClicked()) {
            activeNode?.position = activeNode?.position?.plus(vector) ?: return
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
            if (closedNode == activeNode) {
                feedBackInTutorial("Removed node, since you where closer than 3 blocks from a the active node.")
                nodes.remove(closedNode)
                edges.removeIf { it.isInEdge(closedNode) }
                if (closedNode == activeNode) activeNode = null
                this.closedNode = null
                return
            }
        }
        val position = LocationUtils.playerEyeLocation().roundLocationToBlock()
        if (nodes.any { it.position == position }) {
            feedBackInTutorial("Can't create node, here is already another one.")
            return
        }
        val node = GraphingNode(id++, position)
        nodes.add(node)
        feedBackInTutorial("Added graph node.")
        if (activeNode == null) return
        addEdge(activeNode, node)
    }

    private fun getEdgeIndex(node1: GraphingNode?, node2: GraphingNode?) =
        if (node1 != null && node2 != null && node1 != node2) GraphingEdge(
            node1,
            node2,
        ).let { e -> edges.indexOfFirst { it == e }.takeIf { it != -1 } }
        else null

    private fun addEdge(node1: GraphingNode?, node2: GraphingNode?) =
        if (node1 != null && node2 != null && node1 != node2) edges.add(GraphingEdge(node1, node2)) else false

    /** Has a side effect on the graphing graph, since it runs [prune] on the graphing graph*/
    private fun compileGraph(): Graph {
        prune()
        val indexedTable = nodes.mapIndexed { index, node -> node.id to index }.toMap()
        val nodes = nodes.mapIndexed { index, it -> GraphNode(index, it.position, it.name, it.categories.mapNotNull { it.internalName }) }
        val neighbours = this.nodes.map { node ->
            edges.filter { it.isInEdge(node) }.map { edge ->
                val otherNode = if (node == edge.node1) edge.node2 else edge.node1
                nodes[indexedTable[otherNode.id]!!] to node.position.distance(otherNode.position)
            }.sortedBy { it.second }
        }
        nodes.forEachIndexed { index, it -> it.neighbours = neighbours[index].toMap() }
        return Graph(nodes)
    }

    fun import(graph: Graph) {
        clear()
        nodes.addAll(
            graph.map {
                GraphingNode(
                    it.id,
                    it.position,
                    it.name,
                    it.categoryNames?.mapNotNull { GraphNodeCategory.byId(it) }?.toMutableList() ?: mutableListOf(),
                )
            },
        )
        val translation = graph.mapIndexed { index, it -> it to nodes[index] }.toMap()
        edges.addAll(
            graph.map { node ->
                node.neighbours.map { GraphingEdge(translation[node]!!, translation[it.key]!!) }
            }.flatten().distinct(),
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

        val edge = edges.filter { highlightedNodes.contains(it.node1) && highlightedNodes.contains(it.node2) }
        highlightedEdges.addAll(edge)
    }

    private fun clear() {
        id = 0
        nodes.clear()
        edges.clear()
        activeNode = null
        closedNode = null
    }

    private fun prune() { //TODO fix
        val hasNeighbours = nodes.associateWith { false }.toMutableMap()
        edges.forEach {
            hasNeighbours[it.node1] = true
            hasNeighbours[it.node2] = true
        }
        nodes.removeIf { hasNeighbours[it] == false }
    }
}

// The node object the graph editor is working with
private class GraphingNode(
    val id: Int,
    var position: LorenzVec,
    var name: String? = null,
    var categories: MutableList<GraphNodeCategory> = mutableListOf(),
) {

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

        return (this.node1 == other.node1 && this.node2 == other.node2) ||
            (this.node1 == other.node2 && this.node2 == other.node1)
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

