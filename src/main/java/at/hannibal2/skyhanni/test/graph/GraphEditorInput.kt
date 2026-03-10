package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.Graph
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RaycastUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.fromNow
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object GraphEditorInput {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    private var lastGuiTime = SimpleTimeMark.farPast()

    private val state get() = GraphEditor.state
    private val nodes get() = state.nodes
    private val edges get() = state.edges
    private val textBox get() = state.textBox
    private val closestNode get() = state.closestNode
    private val selectedEdge get() = state.selectedEdge

    @Suppress("ReturnCount")
    fun input() {
        if (handleUndoRedo()) return

        if (isAnyGuiActive()) return
        if (handleExit()) return
        if (handleTextMode()) return
        if (handleText()) return
        if (state.inEditMode) {
            editModeClicks()
            state.inEditMode = false
        }
        if ((state.activeNode != null) && config.editKey.isKeyHeld()) {
            state.inEditMode = true
            return
        }
        if (config.saveKey.isKeyClicked()) {
            GraphEditorIO.save()
            return
        }
        if (handleLoad()) return
        if (handleClear()) return
        if (config.placeKey.isKeyClicked()) {
            GraphEditorNodeOperations.addNode()
            return
        }
        if (handleSelect()) return
        if (handleRayCast()) return
        if (GraphEditorNodeOperations.handleConnect()) return
        if (handleThroughBlocks()) return
        if (config.dijkstraKey.isKeyClicked()) {
            ChatUtils.debug("testDijkstra")
            GraphEditor.feedBackInTutorial("Calculated shortest route and cleared active node.")
            testDijkstra()
            return
        }
        if (config.tutorialKey.isKeyClicked()) {
            state.inTutorialMode = !state.inTutorialMode
            ChatUtils.chat("Tutorial mode is now ${if (state.inTutorialMode) "active" else "inactive"}.")
            return
        }
        val selectedEdge = selectedEdge
        if (selectedEdge != null) {
            handleSplit(selectedEdge)
            handleEdgeCycle(selectedEdge)
        }
        if (GraphEditorNodeOperations.handleDissolve()) return
    }

    private fun handleText(): Boolean {
        if (state.activeNode != null && config.textKey.isKeyClicked()) {
            state.inTextMode = true
            GraphEditor.feedBackInTutorial("Entered Text Mode.")
            return true
        }
        return false
    }

    private fun handleEdgeCycle(selectedEdge: GraphingEdge) {
        if (!config.edgeCycle.isKeyClicked()) return
        GraphEditorHistory.save("cycled direction")
        selectedEdge.cycleDirection(state.activeNode)
        GraphEditor.feedBackInTutorial("Cycled Direction to: ${selectedEdge.cycleText(state.activeNode)}")
    }

    private fun handleSplit(selectedEdge: GraphingEdge) {
        if (!config.splitKey.isKeyClicked()) return
        GraphEditor.feedBackInTutorial("Split Edge into a Node and two edges.")
        val middle = selectedEdge.node1.position.middle(selectedEdge.node2.position).roundToBlock()
        val node = GraphingNode(state.id++, middle)
        GraphEditorHistory.save("split node")
        nodes.add(node)
        edges.remove(selectedEdge)
        GraphEditorNodeOperations.addEdge(selectedEdge.node1, node, selectedEdge.direction)
        GraphEditorNodeOperations.addEdge(node, selectedEdge.node2, selectedEdge.direction)
        state.activeNode = node
    }

    private fun handleThroughBlocks(): Boolean {
        if (!config.throughBlocksKey.isKeyClicked()) return false
        ChatUtils.debug("handleThroughBlocks")
        state.seeThroughBlocks = !state.seeThroughBlocks
        GraphEditor.feedBackInTutorial(
            if (state.seeThroughBlocks) "Graph is visible though walls." else "Graph is invisible behind walls.",
        )

        return true
    }

    private fun handleClear(): Boolean {
        if (!config.clearKey.isKeyClicked()) return false
        val json = GraphEditorIO.compileGraph().toJson()
        OSUtils.copyToClipboard(json)
        ChatUtils.chat("Copied Graph to Clipboard and cleared the graph.")
        GraphEditor.clear()
        return true
    }

    private fun handleSelect(): Boolean {
        if (!config.selectKey.isKeyClicked()) return false
        state.activeNode = if (state.activeNode == closestNode) {
            GraphEditor.feedBackInTutorial("De-selected active node.")
            null
        } else {
            GraphEditor.feedBackInTutorial("Selected new active node.")
            closestNode
        }
        return true
    }

    private fun handleUndoRedo(): Boolean {
        if (Minecraft.getInstance().screen == null) {
            if (KeyboardManager.isControlKeyDown() && GLFW.GLFW_KEY_Y.isKeyClicked()) {
                GraphEditorHistory.undo()
                return true
            }
            if (KeyboardManager.isControlKeyDown() && GLFW.GLFW_KEY_Z.isKeyClicked()) {
                GraphEditorHistory.redo()
                return true
            }
        }
        return false
    }

    private fun handleRayCast(): Boolean {
        if (!config.selectRaycastKey.isKeyClicked()) return false
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
        state.activeNode = minimumNode
        return true
    }

    private fun handleLoad(): Boolean {
        if (!config.loadKey.isKeyClicked()) return false

        val json = OSUtils.readFromClipboard() ?: return true

        SkyHanniMod.launchIOCoroutine("load graph json") {
            try {
                val graph = Graph.fromJson(json)
                val newState = GraphEditorIO.createStateFrom(graph)

                Minecraft.getInstance().execute {
                    GraphEditorHistory.save("load from clipboard")
                    GraphEditor.state = newState
                    GraphEditorNetworks.recalculate()
                    ChatUtils.chat("Loaded Graph from clipboard.")
                }
            } catch (e: Exception) {
                ErrorManager.logErrorWithData(e, "Import failed", "json" to json, ignoreErrorCache = true)
            }
        }
        return true
    }

    private fun handleTextMode(): Boolean {
        if (!state.inTextMode) return false
        if (!textBox.isActive) {
            saveAndExitTextMode()
            return true
        }
        textBox.handle()
        return true
    }

    private fun handleExit(): Boolean {
        if (!config.exitKey.isKeyClicked()) return false
        if (state.inTextMode) {
            saveAndExitTextMode()
            return true
        }
        if (state.inEditMode) {
            state.inEditMode = false
            GraphEditor.feedBackInTutorial("Exited Edit Mode.")
            return true
        }
        config.enabled = false
        GraphEditor.chatAtDisable()
        return true
    }

    private fun saveAndExitTextMode() {
        val newText = textBox.finalText().ifEmpty { null }
        val activeNode = state.activeNode

        if (activeNode != null && activeNode.name != newText) {
            GraphEditorHistory.save("renamed node")
            activeNode.name = newText
        }

        state.inTextMode = false
        GraphEditor.feedBackInTutorial("Exited Text Mode.")
        activeNode?.let {
            GraphEditorNodeOperations.handleNameShortcut(it.name)?.let { (tag, name) ->
                it.tags.add(tag)
                it.name = name
            }
        }
    }

    private var moveToStart: LorenzVec? = null

    @HandleEvent(onlyOnSkyblock = true)
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!event.isLocalPlayer) return
        val location = moveToStart ?: return
        if (!GraphEditor.isEnabled()) {
            moveToStart = null
            return
        }
        if (location.distanceSqToPlayer() < 100) return

        IslandGraphs.pathFind(
            location,
            "Graph Test",
            condition = { GraphEditor.isEnabled() },
        )
        moveToStart = null
    }

    private fun testDijkstra() {
        val location = state.activeNode?.position ?: return
        if (location.distanceToPlayer() < 5) {
            ChatUtils.chat("Graph Test starts once you move away.")
            moveToStart = location
            return
        }

        IslandGraphs.pathFind(
            location,
            "Graph Test",
            condition = { GraphEditor.isEnabled() },
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
        GraphEditorHistory.save("moved node")
        state.activeNode?.let {
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
