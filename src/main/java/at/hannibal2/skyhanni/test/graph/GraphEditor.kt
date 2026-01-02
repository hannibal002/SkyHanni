package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.dev.GraphConfig
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.GraphUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import net.minecraft.client.KeyMapping
import net.minecraft.client.player.LocalPlayer
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GraphEditor {

    val config: GraphConfig get() = SkyHanniMod.feature.dev.devTool.graph

    var state = GraphEditorState()
        set(value) {
            field = value
            updateRender()
            GraphNodeEditor.updateNodeNames()
        }

    fun isEnabled(): Boolean = config.enabled

    private val nodes get() = state.nodes
    private val inTutorialMode get() = state.inTutorialMode
    private val inEditMode get() = state.inEditMode

    private val undoRedoMessageId = ChatUtils.getUniqueMessageId()

    private data class HistoryEntry(
        val state: GraphEditorState,
        val label: String,
        val playerPos: LorenzVec,
    )

    private val undoStack = java.util.Stack<HistoryEntry>()
    private val redoStack = java.util.Stack<HistoryEntry>()

    fun saveState(label: String) {
        val currentPos = GraphUtils.playerPosition
        undoStack.push(HistoryEntry(state.copy(), label, currentPos))

        redoStack.clear()

        if (undoStack.size > 50) undoStack.removeAt(0)
    }

    fun undo() {
        if (undoStack.isEmpty()) {
            sendUndoRedoMessage("§cNothing to undo.")
            return
        }

        val entry = undoStack.pop()

        redoStack.push(HistoryEntry(state.copy(), entry.label, GraphUtils.playerPosition))

        state = entry.state

        restoreContext(entry, "Undo")
    }

    private fun sendUndoRedoMessage(message: String) {
        "§e[SH Graph Editor] $message".asComponent().send(undoRedoMessageId)
    }

    fun redo() {
        if (redoStack.isEmpty()) {
            sendUndoRedoMessage("§cNothing to redo.")
            return
        }

        val entry = redoStack.pop()

        undoStack.push(HistoryEntry(state.copy(), entry.label, GraphUtils.playerPosition))

        state = entry.state

        restoreContext(entry, "Redo")
    }

    fun addUndoRedo(strings: MutableList<String>) {
        if (undoStack.isNotEmpty()) {
            val peek = undoStack.peek().label
            strings.add(" ")
            strings.add("§eUndo: §6Ctrl + ${KeyboardManager.getKeyName(GLFW.GLFW_KEY_Y)}")
            strings.add("§7(next undo: $peek)")
        }
        if (redoStack.isNotEmpty()) {
            val peek = redoStack.peek().label
            strings.add(" ")
            strings.add("§eRedo: §6Ctrl + ${KeyboardManager.getKeyName(GLFW.GLFW_KEY_Z)}")
            strings.add("§7(next redo: $peek)")
        }
    }

    private fun restoreContext(entry: HistoryEntry, type: String) {
        val stackSize = if (type == "Undo") undoStack.size else redoStack.size
        sendUndoRedoMessage("§a$type: ${entry.label} §7($stackSize left)")

        if (entry.playerPos.distance(GraphUtils.playerPosition) <= 5.0) return
        IslandGraphs.pathFind(
            entry.playerPos,
            "$type: ${entry.label}",
            java.awt.Color.ORANGE,
            condition = { isEnabled() },
        )
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

        // Update cache every second for normal movement
        if (state.lastCacheUpdate.passedSince() > 1.seconds) {
            updateCache()
        }

        state.closestNode = state.cachedNearbyNodes.minByOrNull { it.distanceSqToPlayer() }

        GraphEditorNodeFinder.handleAllNodeFind()
    }

    @HandleEvent
    fun onPlayerMove(event: EntityMoveEvent<LocalPlayer>) {
        if (!isEnabled()) return
        if (!event.isLocalPlayer) return

        if (event.distance > 20) {
            updateCache()
        }
    }

    fun updateCache() {
        state.cachedNearbyNodes = nodes.sortedBy { it.distanceSqToPlayer() }.take(20)
        state.lastCacheUpdate = SimpleTimeMark.now()
    }

    fun updateRender() {
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

    fun clear() {
        saveState("clear graph")
        state = GraphEditorState()
    }

    fun enable() {
        if (!config.enabled) {
            config.enabled = true
            ChatUtils.chat("Graph Editor is now active.")
        }
    }
}

