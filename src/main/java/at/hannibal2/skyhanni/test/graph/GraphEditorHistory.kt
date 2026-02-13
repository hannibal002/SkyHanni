package at.hannibal2.skyhanni.test.graph

import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.test.graph.GraphEditor.isEnabled
import at.hannibal2.skyhanni.test.graph.GraphEditor.state
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.GraphUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import org.lwjgl.glfw.GLFW

object GraphEditorHistory {

    private val undoRedoMessageId = ChatUtils.getUniqueMessageId()

    private data class HistoryEntry(
        val state: GraphEditorState,
        val label: String,
        val playerPos: LorenzVec,
    )

    private val undoStack = java.util.Stack<HistoryEntry>()
    private val redoStack = java.util.Stack<HistoryEntry>()

    fun save(label: String) {
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

    fun addDisplayLines(strings: MutableList<String>) {
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
}
