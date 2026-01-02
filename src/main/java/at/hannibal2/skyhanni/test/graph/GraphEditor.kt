package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.GraphUtils.getNearestNode
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.client.KeyMapping
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

    var inTutorialMode = false

    val textBox = TextInput()

    var dissolvePossible = false

    fun findEdgeBetweenActiveAndClosest(): GraphingEdge? = getEdgeIndex(activeNode, closestNode)?.let { edges[it] }

    fun checkDissolve() {
        if (activeNode == null) {
            dissolvePossible = false
            return
        }
        dissolvePossible = edges.count { it.isInEdge(activeNode) } == 2
    }

    fun feedBackInTutorial(text: String) {
        if (inTutorialMode) {
            ChatUtils.chat(text)
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!isEnabled()) return
        GraphEditorInput.input()
        if (event.isMod(5)) {
            updateRender()
        }
        if (nodes.isEmpty()) return
        closestNode = nodes.getNearestNode()
        GraphEditorNodeFinder.handleAllNodeFind()
    }

    private fun updateRender() {
        val maxNodeDistance = config.maxNodeDistance * config.maxNodeDistance
        for (node in nodes) {
            node.rendering = node.distanceSqToPlayer() < maxNodeDistance
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
            callback { GraphEditorNodeFinder.toggleFindAll() }
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

    fun chatAtDisable() = ChatUtils.clickableChat(
        "Graph Editor is now inactive. §lClick to activate.",
        GraphEditor::toggleFeature,
    )

    fun onMinecraftInput(keyBinding: KeyMapping, cir: CallbackInfoReturnable<Boolean>) {
        if (!isEnabled()) return
        if (!inEditMode) return
        if (keyBinding !in KeyboardManager.WasdInputMatrix) return
        cir.returnValue = false
    }

    fun getEdgeIndex(node1: GraphingNode?, node2: GraphingNode?) =
        if (node1 != null && node2 != null && node1 != node2) GraphingEdge(
            node1,
            node2,
        ).let { e -> edges.indexOfFirst { it == e }.takeIf { it != -1 } }
        else null


    val highlightedNodes = mutableSetOf<GraphingNode>()
    val highlightedEdges = mutableSetOf<GraphingEdge>()

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

