package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.data.model.GraphNodeTag
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.playerPosition
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
import kotlin.text.ifEmpty

@SkyHanniModule
object GraphEditorInput {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    private var lastGuiTime = SimpleTimeMark.farPast()

    private val nodes get() = GraphEditor.nodes
    private val edges get() = GraphEditor.edges
    private val textBox get() = GraphEditor.textBox
    private val activeNode get() = GraphEditor.activeNode
    private val closestNode get() = GraphEditor.closestNode
    private val selectedEdge get() = GraphEditor.selectedEdge

    fun input() {
        if (isAnyGuiActive()) return
        if (config.exitKey.isKeyClicked()) {
            if (GraphEditor.inTextMode) {
                GraphEditor.inTextMode = false
                GraphEditor.feedBackInTutorial("Exited Text Mode.")
                GraphEditor.activeNode?.let {
                    handleNameShortcut(it.name)?.let { (tag, name) ->
                        it.tags.add(tag)
                        it.name = name
                    }
                }
                return
            }
            if (GraphEditor.inEditMode) {
                GraphEditor.inEditMode = false
                GraphEditor.feedBackInTutorial("Exited Edit Mode.")
                return
            }
            config.enabled = false
            GraphEditor.chatAtDisable()
        }
        if (GraphEditor.inTextMode) {
            textBox.handle()
            val text = textBox.finalText()
            activeNode?.name = text.ifEmpty { null }
            return
        }
        if (activeNode != null && config.textKey.isKeyClicked()) {
            GraphEditor.inTextMode = true
            GraphEditor.feedBackInTutorial("Entered Text Mode.")
            return
        }
        if (GraphEditor.inEditMode) {
            editModeClicks()
            GraphEditor.inEditMode = false
        }
        if ((activeNode != null) && config.editKey.isKeyHeld()) {
            GraphEditor.inEditMode = true
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
            GraphEditor.clear()
        }
        if (config.placeKey.isKeyClicked()) {
            addNode()
        }
        if (config.selectKey.isKeyClicked()) {
            GraphEditor.activeNode = if (activeNode == closestNode) {
                GraphEditor.feedBackInTutorial("De-selected active node.")
                null
            } else {
                GraphEditor.feedBackInTutorial("Selected new active node.")
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
            GraphEditor.activeNode = minimumNode
        }
        if (activeNode != closestNode && config.connectKey.isKeyClicked()) {
            val edge = GraphEditor.getEdgeIndex(activeNode, closestNode)
            if (edge == null) {
                addEdge(activeNode, closestNode)
                GraphEditor.feedBackInTutorial("Added new edge.")
            } else {
                edges.removeAt(edge)
                GraphEditor.checkDissolve()
                GraphEditor.selectedEdge = GraphEditor.findEdgeBetweenActiveAndClosest()
                GraphEditor.feedBackInTutorial("Removed edge.")
            }
        }
        if (config.throughBlocksKey.isKeyClicked()) {
            GraphEditor.seeThroughBlocks = !GraphEditor.seeThroughBlocks
            GraphEditor.feedBackInTutorial(
                if (GraphEditor.seeThroughBlocks) "Graph is visible though walls." else "Graph is invisible behind walls.",
            )
        }
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
            if (config.splitKey.isKeyClicked()) {
                GraphEditor.feedBackInTutorial("Split Edge into a Node and two edges.")
                val middle = selectedEdge.node1.position.middle(selectedEdge.node2.position).roundToBlock()
                val node = GraphingNode(GraphEditor.id++, middle)
                nodes.add(node)
                edges.remove(selectedEdge)
                addEdge(selectedEdge.node1, node, selectedEdge.direction)
                addEdge(node, selectedEdge.node2, selectedEdge.direction)
                GraphEditor.activeNode = node
            }
            if (config.edgeCycle.isKeyClicked()) {
                selectedEdge.cycleDirection(activeNode)
                GraphEditor.feedBackInTutorial("Cycled Direction to: ${selectedEdge.cycleText(activeNode)}")
            }
        }
        if (GraphEditor.dissolvePossible && config.dissolveKey.isKeyClicked()) {
            GraphEditor.feedBackInTutorial("Dissolved the node, now it is gone.")
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
            GraphEditor.activeNode = null
            addEdge(neighbors1, neighbors2, direction)
        }
    }

    private fun addNode() {
        val closestNode = GraphEditor.closestNode
        if (closestNode != null && closestNode.distanceSqToPlayer() < 9.0) {
            if (closestNode == GraphEditor.activeNode) {
                GraphEditor.feedBackInTutorial("Removed node, since you where closer than 3 blocks from a the active node.")
                GraphEditor.nodes.remove(closestNode)
                GraphEditor.edges.removeIf { it.isInEdge(closestNode) }
                if (closestNode == GraphEditor.activeNode) GraphEditor.activeNode = null
                GraphEditor.closestNode = null
                return
            }
        }

        if (GraphEditor.nodes.any { it.position == playerPosition }) {
            GraphEditor.feedBackInTutorial("Can't create node, here is already another one.")
            return
        }
        val node = GraphingNode(GraphEditor.id++, playerPosition)
        GraphEditor.nodes.add(node)
        GraphEditor.feedBackInTutorial("Added graph node.")
        if (GraphEditor.activeNode == null) return
        addEdge(GraphEditor.activeNode, node)
    }

    private fun addEdge(node1: GraphingNode?, node2: GraphingNode?, direction: EdgeDirection = EdgeDirection.BOTH) =
        if (node1 != null && node2 != null && node1 != node2) {
            val edge = GraphingEdge(node1, node2, direction)
            if (edge.isInEdge(GraphEditor.activeNode)) {
                GraphEditor.checkDissolve()
                GraphEditor.selectedEdge = GraphEditor.findEdgeBetweenActiveAndClosest()
            }
            GraphEditor.edges.add(edge)
        } else false

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

    private fun handleNameShortcut(name: String?): Pair<GraphNodeTag, String>? = when (name) {
        "fsoul" -> GraphNodeTag.FAIRY_SOUL to "Fairy Soul"
        "na" -> GraphNodeTag.AREA to "no_area"
        else -> null
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
            GraphEditor.activeNode?.let {
                it.position += vector
            }
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
