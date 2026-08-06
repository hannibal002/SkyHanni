package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.utils.KeyboardManager
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption
import org.lwjgl.glfw.GLFW

class GraphEditorConfig {
    @Expose
    @ConfigOption(name = "Enabled", desc = "Enable the Graph Editor. Can also be toggled via /shgraph")
    @ConfigEditorBoolean
    var enabled: Boolean = false

    @Expose
    @ConfigOption(
        name = "Place Key",
        desc = "Place a new node at the current position. If a node is active automatically connects. " +
            "Deletes a node if you are only 3 blocks away instead of placing a new one."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_F)
    var placeKey: Int = GLFW.GLFW_KEY_F

    @Expose
    @ConfigOption(name = "Auto Select Node", desc = "Automatically select a node after placing it.")
    @ConfigEditorBoolean
    var autoSelectNode: Boolean = false

    // TODO rename to selectNearestKey
    @Expose
    @ConfigOption(name = "Select Nearest Key", desc = "Select the nearest node to be active. Double press to unselect.")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.MIDDLE_MOUSE)
    var selectKey: Int = KeyboardManager.MIDDLE_MOUSE

    // TODO rename to selectLookingAtNodeKey
    @Expose
    @ConfigOption(name = "Select looking at Node", desc = "Select the node you are pointing your cursor at.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var selectRaycastKey: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Connect Key",
        desc = "Connect the nearest node with the active node. If the nodes are already connected removes the connection."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_C)
    var connectKey: Int = GLFW.GLFW_KEY_C

    @Expose
    @ConfigOption(name = "Exit Key", desc = "Exit out of text edit mode. If not in text edit mode, disables the graph editor.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_HOME)
    var exitKey: Int = GLFW.GLFW_KEY_ENTER

    // TODO rename to nodeMoveKey
    @Expose
    @ConfigOption(
        name = "Node Move Key",
        desc = "While holding the Key, edit the position of the active node or the selection block with the minecraft movement controls."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_TAB)
    var editKey: Int = GLFW.GLFW_KEY_TAB

    @Expose
    @ConfigOption(name = "Text Key", desc = "Start text mode, which allows editing a name of a node.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_Y)
    var textKey: Int = GLFW.GLFW_KEY_Y

    // TODO rename to navigateToNodeKey
    @Expose
    @ConfigOption(
        name = "Navigate to Node",
        desc = "On key press, show the shortest path to the active node."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_G)
    var dijkstraKey: Int = GLFW.GLFW_KEY_G

    @Expose
    @ConfigOption(name = "Save Key", desc = "Save the current graph to the clipboard.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_O)
    var saveKey: Int = GLFW.GLFW_KEY_O

    @Expose
    @ConfigOption(name = "Load Key", desc = "Load a graph from clipboard, if valid.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_I)
    var loadKey: Int = GLFW.GLFW_KEY_I

    @Expose
    @ConfigOption(
        name = "Clear Key",
        desc = "Clear the graph. Also saves the graph to the clipboard, in case of a misclick."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_P)
    var clearKey: Int = GLFW.GLFW_KEY_P

    @Expose
    @ConfigOption(name = "Vision Key", desc = "Toggle if the graph should render trough blocks.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_M)
    var throughBlocksKey: Int = GLFW.GLFW_KEY_M

    // TODO rename to feedbackKey
    @Expose
    @ConfigOption(
        name = "Feedback Key",
        desc = "Toggle the feedback mode. In this mode, you will get a chat message explaining on everything you do in the Graph Editor."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_K)
    var tutorialKey: Int = GLFW.GLFW_KEY_K

    @Expose
    @ConfigOption(
        name = "Split Key",
        desc = "Key for splitting an edge that is between the active and the closest node."
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var splitKey: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Dissolve Key", desc = "Dissolve the active node into one edge if it only has two edges.")
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_UNKNOWN)
    var dissolveKey: Int = GLFW.GLFW_KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Edge Cycle",
        desc = "Cycles the direction of the edge that is between the active and the closest node. (Used to make one-directional ways)"
    )
    @ConfigEditorKeybind(defaultKey = GLFW.GLFW_KEY_H)
    var edgeCycle: Int = GLFW.GLFW_KEY_H

    @Expose
    @ConfigLink(owner = GraphEditorConfig::class, field = "enabled")
    val infoDisplay: Position = Position(20, 20)

    @Expose
    @ConfigLink(owner = GraphEditorConfig::class, field = "enabled")
    val namedNodesList: Position = Position(20, 20)

    @Expose
    @ConfigOption(name = "Max Node Distance", desc = "Only render nodes below this distance to the player.")
    @ConfigEditorSlider(minValue = 10f, maxValue = 500f, minStep = 10f)
    var maxNodeDistance: Int = 50

    @Expose
    @ConfigOption(name = "Shows Stats", desc = "Show funny extra statistics on save. May lag the game a bit.")
    @ConfigEditorBoolean
    var showsStats: Boolean = true

    @Expose
    @ConfigOption(
        name = "Use as Island Area",
        desc = "When saving, use the current edited graph as temporary island area for the current island."
    )
    @ConfigEditorBoolean
    var useAsIslandArea: Boolean = false
}
