package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandGraphs.pathFind
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNode
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.getNearestNode
import at.hannibal2.skyhanni.utils.GraphUtils.getNearestToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import kotlinx.coroutines.runBlocking
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable

@SkyHanniModule
object GraphEditor {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    fun isEnabled(): Boolean = config.enabled

    var id = 0

    val nodes = mutableListOf<GraphingNode>()
    val edges = mutableListOf<GraphingEdge>()

    var activeNode: GraphingNode? = null
        set(value) {
            field = value
            selectedEdge = findEdgeBetweenActiveAndClosest()
            checkDissolve()
        }
    var closestNode: GraphingNode? = null
        set(value) {
            field = value
            selectedEdge = findEdgeBetweenActiveAndClosest()
        }

    var selectedEdge: GraphingEdge? = null

    var seeThroughBlocks = true

    var inEditMode = false
    var inTextMode = false
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

    val textBox = TextInput()

    private val nodesAlreadyFound = mutableListOf<LorenzVec>()
    private val nodesToFind: List<LorenzVec>
        get() = IslandGraphs.currentIslandGraph?.map { it.position }?.filter { it !in nodesAlreadyFound }.orEmpty()
    private var currentNodeToFind: LorenzVec? = null
    var active = false

    var dissolvePossible = false

    fun findEdgeBetweenActiveAndClosest(): GraphingEdge? = getEdgeIndex(activeNode, closestNode)?.let { edges[it] }

    fun checkDissolve() {
        if (activeNode == null) {
            dissolvePossible = false
            return
        }
        dissolvePossible = edges.count { it.isInEdge(activeNode) } == 2
    }

    private fun feedBackInTutorial(text: String) {
        if (inTutorialMode) {
            ChatUtils.chat(text)
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        input()
        if (event.isMod(5)) {
            updateRender()
        }
        if (nodes.isEmpty()) return
        closestNode = nodes.getNearestNode()
        handleAllNodeFind()
    }

    private fun updateRender() {
        val maxNodeDistance = config.maxNodeDistance * config.maxNodeDistance
        for (node in nodes) {
            node.rendering = node.distanceSqToPlayer() < maxNodeDistance
        }
    }

    private fun handleAllNodeFind() {
        if (!active) return

        if (nodesToFind.isEmpty()) return
        val closest = nodesToFind.getNearestToPlayer()
        if (distanceSqToPlayer(closest) >= 9) return
        nodesAlreadyFound.add(closest)

        if (nodesToFind.isEmpty()) {
            currentNodeToFind = null
            ChatUtils.chat("Found all nodes on this island")
            TitleManager.sendTitle("§eAll Found!")
            active = false
            return
        }

        calculateNewAllNodeFind()
    }

    fun calculateNewAllNodeFind(): LorenzVec {
        val next = GraphUtils.findShortestDistancesOnCurrentIsland(nodesToFind).lastVisitedNode.position

        val max = IslandGraphs.currentIslandGraph?.size ?: -1
        val todo = nodesToFind.size
        val done = max - todo
        val percentage = (done.toDouble() / max.toDouble()) * 100
        val node = GraphUtils.nearestNodeOnCurrentIsland(next)
        node.pathFind(
            "Progress: ${done.addSeparators()}/${max.addSeparators()} (${percentage.roundTo(2)}%)",
            condition = { active },
        )
        currentNodeToFind = next
        return next
    }

    private fun toggleFindAll() {
        active = !active
        if (active) {
            nodesAlreadyFound.clear()
            calculateNewAllNodeFind()
            ChatUtils.chat("Graph navigation over all nodes started.")
        } else {
            ChatUtils.chat("Graph navigation over all nodes stopped.")
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shgraph") {
            description = "Enables the graph editor"
            category = CommandCategory.DEVELOPER_TEST
            callback { toggleFeature() }
        }
        event.register("shgraphfindall") {
            description = "Navigate over the whole graph network"
            category = CommandCategory.DEVELOPER_TEST
            callback { toggleFindAll() }
        }
        event.register("shgraphloadthisisland") {
            description = "Loads the current island data into the graph editor."
            category = CommandCategory.DEVELOPER_TEST
            callback { GraphEditorIO.loadThisIsland() }
        }
    }

    var bypassTempRemoveTimer = SimpleTimeMark.farPast()

    private fun toggleFeature() {
        config.enabled = !config.enabled
        if (config.enabled) {
            ChatUtils.chat("Graph Editor is now active.")
        } else {
            chatAtDisable()
        }
    }

    private fun chatAtDisable() = ChatUtils.clickableChat(
        "Graph Editor is now inactive. §lClick to activate.",
        GraphEditor::toggleFeature,
    )

    private fun input() {
        if (isAnyGuiActive()) return
        if (config.exitKey.isKeyClicked()) {
            if (inTextMode) {
                inTextMode = false
                feedBackInTutorial("Exited Text Mode.")
                activeNode?.let {
                    handleNameShortcut(it.name)?.let { (tag, name) ->
                        it.tags.add(tag)
                        it.name = name
                    }
                }
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
            activeNode?.name = text.ifEmpty { null }
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
        if ((activeNode != null) && config.editKey.isKeyHeld()) {
            inEditMode = true
            return
        }
        if (config.saveKey.isKeyClicked()) {
            GraphEditorIO.save()
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
                    GraphEditorIO.import(it)
                    ChatUtils.chat("Loaded Graph from clip board.")
                }
            }
            return
        }
        if (config.clearKey.isKeyClicked()) {
            val json = GraphEditorIO.compileGraph().toJson()
            OSUtils.copyToClipboard(json)
            ChatUtils.chat("Copied Graph to Clipboard and cleared the graph.")
            clear()
        }
        if (config.placeKey.isKeyClicked()) {
            addNode()
        }
        if (config.selectKey.isKeyClicked()) {
            activeNode = if (activeNode == closestNode) {
                feedBackInTutorial("De-selected active node.")
                null
            } else {
                feedBackInTutorial("Selected new active node.")
                closestNode
            }
        }
        if (config.selectRaycastKey.isKeyClicked()) {
            val playerRay = RaycastUtils.createPlayerLookDirectionRay()
            var minimumDistance = Double.MAX_VALUE
            var minimumNode: GraphingNode? = null
            for (node in nodes) {
                val nodeCenterPosition = node.position.add(0.5, 0.5, 0.5)
                val distance = RaycastUtils.findDistanceToRay(playerRay, nodeCenterPosition)
                if (distance > minimumDistance) {
                    continue
                }
                if (minimumDistance > 1.0) {
                    minimumNode = node
                    minimumDistance = distance
                    continue
                }
                if (minimumNode == null || minimumNode.distanceSqToPlayer() > node.distanceSqToPlayer()) {
                    minimumNode = node
                    minimumDistance = distance
                }
            }
            activeNode = minimumNode
        }
        if (activeNode != closestNode && config.connectKey.isKeyClicked()) {
            val edge = getEdgeIndex(activeNode, closestNode)
            if (edge == null) {
                addEdge(activeNode, closestNode)
                feedBackInTutorial("Added new edge.")
            } else {
                edges.removeAt(edge)
                checkDissolve()
                selectedEdge = findEdgeBetweenActiveAndClosest()
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
        val selectedEdge = selectedEdge
        if (selectedEdge != null) {
            if (config.splitKey.isKeyClicked()) {
                feedBackInTutorial("Split Edge into a Node and two edges.")
                val middle = selectedEdge.node1.position.middle(selectedEdge.node2.position).roundToBlock()
                val node = GraphingNode(id++, middle)
                nodes.add(node)
                edges.remove(selectedEdge)
                addEdge(selectedEdge.node1, node, selectedEdge.direction)
                addEdge(node, selectedEdge.node2, selectedEdge.direction)
                activeNode = node
            }
            if (config.edgeCycle.isKeyClicked()) {
                selectedEdge.cycleDirection(activeNode)
                feedBackInTutorial("Cycled Direction to: ${selectedEdge.cycleText(activeNode)}")
            }
        }
        if (dissolvePossible && config.dissolveKey.isKeyClicked()) {
            feedBackInTutorial("Dissolved the node, now it is gone.")
            val edgePair = edges.filter { it.isInEdge(activeNode) }
            val edge1 = edgePair[0]
            val edge2 = edgePair[1]
            val neighbors1 = if (edge1.node1 == activeNode) edge1.node2 else edge1.node1
            val neighbors2 = if (edge2.node1 == activeNode) edge2.node2 else edge2.node1
            val direction =
                if (edge1.direction == EdgeDirection.BOTH || edge2.direction == EdgeDirection.BOTH) EdgeDirection.BOTH else when {
                    edge1.isValidConnectionFromTo(neighbors1, activeNode) && edge2.isValidConnectionFromTo(
                        activeNode,
                        neighbors2,
                    ) -> EdgeDirection.ONE_TO_TWO

                    edge1.isValidConnectionFromTo(activeNode, neighbors1) && edge2.isValidConnectionFromTo(
                        neighbors2,
                        activeNode,
                    ) -> EdgeDirection.TOW_TO_ONE

                    else -> EdgeDirection.BOTH
                }
            edges.removeAll(edgePair)
            nodes.remove(activeNode)
            activeNode = null
            addEdge(neighbors1, neighbors2, direction)
        }
    }

    private fun handleNameShortcut(name: String?): Pair<GraphNodeTag, String>? = when (name) {
        "fsoul" -> GraphNodeTag.FAIRY_SOUL to "Fairy Soul"
        "na" -> GraphNodeTag.AREA to "no_area"
        else -> null
    }

    private var lastGuiTime = SimpleTimeMark.farPast()

    private fun isAnyGuiActive(): Boolean {
        val gui = Minecraft.getInstance().screen != null
        if (gui) {
            lastGuiTime = 3.ticks.fromNow()
        }
        return !lastGuiTime.isInPast()
    }

    private fun editModeClicks() {
        val vector = LocationUtils.calculatePlayerFacingDirection()
        KeyboardManager.WasdInputMatrix.w.handleEditClicks(vector)
        KeyboardManager.WasdInputMatrix.a.handleEditClicks(vector.rotateXZ(Math.toRadians(90.0)))
        KeyboardManager.WasdInputMatrix.s.handleEditClicks(vector.rotateXZ(Math.toRadians(180.0)))
        KeyboardManager.WasdInputMatrix.d.handleEditClicks(vector.rotateXZ(Math.toRadians(270.0)))

        KeyboardManager.WasdInputMatrix.up.handleEditClicks(LorenzVec(0, 1, 0))
        KeyboardManager.WasdInputMatrix.down.handleEditClicks(LorenzVec(0, -1, 0))
    }

    private fun KeyMapping.handleEditClicks(vector: LorenzVec) {
        if (this.key.value.isKeyClicked()) {
            activeNode?.let {
                it.position += vector
            }
        }
    }

    fun onMinecraftInput(keyBinding: KeyMapping, cir: CallbackInfoReturnable<Boolean>) {
        if (!isEnabled()) return
        if (!inEditMode) return
        if (keyBinding !in KeyboardManager.WasdInputMatrix) return
        cir.returnValue = false
    }

    private fun addNode() {
        val closestNode = closestNode
        if (closestNode != null && closestNode.distanceSqToPlayer() < 9.0) {
            if (closestNode == activeNode) {
                feedBackInTutorial("Removed node, since you where closer than 3 blocks from a the active node.")
                nodes.remove(closestNode)
                edges.removeIf { it.isInEdge(closestNode) }
                if (closestNode == activeNode) activeNode = null
                GraphEditor.closestNode = null
                return
            }
        }

        if (nodes.any { it.position == playerPosition }) {
            feedBackInTutorial("Can't create node, here is already another one.")
            return
        }
        val node = GraphingNode(id++, playerPosition)
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

    private fun addEdge(node1: GraphingNode?, node2: GraphingNode?, direction: EdgeDirection = EdgeDirection.BOTH) =
        if (node1 != null && node2 != null && node1 != node2) {
            val edge = GraphingEdge(node1, node2, direction)
            if (edge.isInEdge(activeNode)) {
                checkDissolve()
                selectedEdge = findEdgeBetweenActiveAndClosest()
            }
            edges.add(edge)
        } else false

    val highlightedNodes = mutableSetOf<GraphingNode>()
    val highlightedEdges = mutableSetOf<GraphingEdge>()

    private fun testDijkstra() {

        val savedCurrent = closestNode ?: return
        val savedActive = activeNode ?: return

        val compiled = GraphEditorIO.compileGraph()
        GraphEditorIO.import(compiled)
        highlightedEdges.clear()
        highlightedNodes.clear()

        val current = compiled.firstOrNull { it.position == savedCurrent.position } ?: return
        val goal = compiled.firstOrNull { it.position == savedActive.position } ?: return

        val path = GraphUtils.findShortestPathAsGraph(current, goal)

        if (path.isEmpty()) {
            ChatUtils.chat("No Path found")
        }

        val inGraph = path.map { nodes[it.id] }
        highlightedNodes.addAll(inGraph)

        highlightedEdges.addAll(
            highlightedNodes.zipWithNext { a, b -> edges.firstOrNull { it.isValidConnectionFromTo(a, b) } }.filterNotNull(),
        )
    }

    fun clear() {
        id = 0
        nodes.clear()
        edges.clear()
        activeNode = null
        closestNode = null
        dissolvePossible = false
    }

    fun enable() {
        if (!config.enabled) {
            config.enabled = true
            ChatUtils.chat("Graph Editor is now active.")
        }
    }
}

