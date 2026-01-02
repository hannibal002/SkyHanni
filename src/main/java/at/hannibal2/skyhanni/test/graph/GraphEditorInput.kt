package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import kotlinx.coroutines.runBlocking
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft

@SkyHanniModule
object GraphEditorInput {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    private var lastGuiTime = SimpleTimeMark.farPast()

    private val nodes get() = GraphEditor.nodes
    private val edges get() = GraphEditor.edges
    private val textBox get() = GraphEditor.textBox
    private val closestNode get() = GraphEditor.closestNode
    private val selectedEdge get() = GraphEditor.selectedEdge

    fun input() {
        if (isAnyGuiActive()) return
        if (handleExit()) return
        if (handleTextMode()) return
        if (handleText()) return
        if (GraphEditor.inEditMode) {
            editModeClicks()
            GraphEditor.inEditMode = false
        }
        if ((GraphEditor.activeNode != null) && config.editKey.isKeyHeld()) {
            GraphEditor.inEditMode = true
            return
        }
        if (config.saveKey.isKeyClicked()) {
            GraphEditorIO.save()
            return
        }
        if (handleLoad()) return
        handleClear()
        if (config.placeKey.isKeyClicked()) {
            GraphEditorNodeOperations.addNode()
        }
        handleSelect()
        handleRayCast()
        GraphEditorNodeOperations.handleConnect()
        handleThroughBlocks()
        if (config.dijkstraKey.isKeyClicked()) {
            GraphEditor.feedBackInTutorial("Calculated shortest route and cleared active node.")
            testDijkstra()
        }
        if (config.tutorialKey.isKeyClicked()) {
            GraphEditor.inTutorialMode = !GraphEditor.inTutorialMode
            ChatUtils.chat("Tutorial mode is now ${if (GraphEditor.inTutorialMode) "active" else "inactive"}.")
        }
        val selectedEdge = selectedEdge
        if (selectedEdge != null) {
            handleSplit(selectedEdge)
            handleEdgeCycle(selectedEdge)
        }
        GraphEditorNodeOperations.handleDissolve()
    }

    private fun handleText(): Boolean {
        if (GraphEditor.activeNode != null && config.textKey.isKeyClicked()) {
            GraphEditor.inTextMode = true
            GraphEditor.feedBackInTutorial("Entered Text Mode.")
            return true
        }
        return false
    }

    private fun handleEdgeCycle(selectedEdge: GraphingEdge) {
        if (!config.edgeCycle.isKeyClicked()) return
        selectedEdge.cycleDirection(GraphEditor.activeNode)
        GraphEditor.feedBackInTutorial("Cycled Direction to: ${selectedEdge.cycleText(GraphEditor.activeNode)}")
    }

    private fun handleSplit(selectedEdge: GraphingEdge) {
        if (!config.splitKey.isKeyClicked()) return
        GraphEditor.feedBackInTutorial("Split Edge into a Node and two edges.")
        val middle = selectedEdge.node1.position.middle(selectedEdge.node2.position).roundToBlock()
        val node = GraphingNode(GraphEditor.id++, middle)
        nodes.add(node)
        edges.remove(selectedEdge)
        GraphEditorNodeOperations.addEdge(selectedEdge.node1, node, selectedEdge.direction)
        GraphEditorNodeOperations.addEdge(node, selectedEdge.node2, selectedEdge.direction)
        GraphEditor.activeNode = node
    }

    private fun handleThroughBlocks() {
        if (!config.throughBlocksKey.isKeyClicked()) return
        GraphEditor.seeThroughBlocks = !GraphEditor.seeThroughBlocks
        GraphEditor.feedBackInTutorial(
            if (GraphEditor.seeThroughBlocks) "Graph is visible though walls." else "Graph is invisible behind walls.",
        )
    }

    private fun handleClear() {
        if (!config.clearKey.isKeyClicked()) return
        val json = GraphEditorIO.compileGraph().toJson()
        OSUtils.copyToClipboard(json)
        ChatUtils.chat("Copied Graph to Clipboard and cleared the graph.")
        GraphEditor.clear()
    }

    private fun handleSelect() {
        if (!config.selectKey.isKeyClicked()) return
        GraphEditor.activeNode = if (GraphEditor.activeNode == closestNode) {
            GraphEditor.feedBackInTutorial("De-selected active node.")
            null
        } else {
            GraphEditor.feedBackInTutorial("Selected new active node.")
            closestNode
        }
    }

    private fun handleRayCast() {
        if (!config.selectRaycastKey.isKeyClicked()) return
        val playerRay = RaycastUtils.createPlayerLookDirectionRay()
        var minimumDistance = Double.MAX_VALUE
        var minimumNode: GraphingNode? = null
        for (node in nodes.filter { it.rendering }) {
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
        GraphEditor.activeNode = minimumNode
    }

    private fun handleLoad(): Boolean {
        if (!config.loadKey.isKeyClicked()) return false
        runBlocking {
            val json = OSUtils.readFromClipboard() ?: return@runBlocking
            try {
                val graph = Graph.fromJson(json)
                GraphEditorIO.import(graph)
                ChatUtils.chat("Loaded Graph from clip board.")
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(
                    e,
                    "Import of graph failed: ${e.message}",
                    "json" to json,
                    ignoreErrorCache = true,
                )
            }
        }
        return true
    }

    private fun handleTextMode(): Boolean {
        if (!GraphEditor.inTextMode) return false
        textBox.handle()
        val text = textBox.finalText()
        GraphEditor.activeNode?.name = text.ifEmpty { null }
        return true
    }

    private fun handleExit(): Boolean {
        if (!config.exitKey.isKeyClicked()) return false
        if (GraphEditor.inTextMode) {
            GraphEditor.inTextMode = false
            GraphEditor.feedBackInTutorial("Exited Text Mode.")
            GraphEditor.activeNode?.let {
                GraphEditorNodeOperations.handleNameShortcut(it.name)?.let { (tag, name) ->
                    it.tags.add(tag)
                    it.name = name
                }
            }
            return true
        }
        if (GraphEditor.inEditMode) {
            GraphEditor.inEditMode = false
            GraphEditor.feedBackInTutorial("Exited Edit Mode.")
            return true
        }
        config.enabled = false
        GraphEditor.chatAtDisable()
        return false
    }

    private fun testDijkstra() {

        val savedCurrent = GraphEditor.closestNode ?: return
        val savedActive = GraphEditor.activeNode ?: return

        val compiled = GraphEditorIO.compileGraph()
        GraphEditorIO.import(compiled)
        GraphEditor.highlightedEdges.clear()
        GraphEditor.highlightedNodes.clear()

        val current = compiled.firstOrNull { it.position == savedCurrent.position } ?: return
        val goal = compiled.firstOrNull { it.position == savedActive.position } ?: return

        val path = GraphUtils.findShortestPathAsGraph(current, goal)

        if (path.isEmpty()) {
            ChatUtils.chat("No Path found")
        }

        val inGraph = path.map { GraphEditor.nodes[it.id] }
        GraphEditor.highlightedNodes.addAll(inGraph)

        GraphEditor.highlightedEdges.addAll(
            GraphEditor.highlightedNodes.zipWithNext { a, b -> GraphEditor.edges.firstOrNull { it.isValidConnectionFromTo(a, b) } }
                .filterNotNull(),
        )
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
        if (!this.key.value.isKeyClicked()) return
        GraphEditor.activeNode?.let {
            it.position += vector
        }
    }

    private fun isAnyGuiActive(): Boolean {
        val gui = Minecraft.getInstance().screen != null
        if (gui) {
            lastGuiTime = 3.ticks.fromNow()
        }
        return !lastGuiTime.isInPast()
    }
}
