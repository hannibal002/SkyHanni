package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.test.graph.GraphEditor
import at.hannibal2.skyhanni.utils.KeyboardManager
import com.google.gson.annotations.Expose
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

class GraphEditorConfig {

    @ConfigOption(name = "Open Tutorial", desc = "Open the Graph Network and Graph Editor tutorial in your browser.")
    @ConfigEditorButton(buttonText = "Open")
    val openTutorial: Runnable = Runnable { GraphEditor.openTutorial() }

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
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_F)
    var placeKey: Int = InputConstants.KEY_F

    @Expose
    @ConfigOption(name = "Auto Select Node", desc = "Automatically select a node after placing it.")
    @ConfigEditorBoolean
    var autoSelectNode: Boolean = false

    // TODO rename to selectNearestNodeKey
    @Expose
    @ConfigOption(name = "Select Nearest Node", desc = "Select the nearest node to be active. Double press to unselect.")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.LEFT_MOUSE)
    var selectKey: Int = KeyboardManager.LEFT_MOUSE

    // TODO rename to selectNodeByLookingKey
    @Expose
    @ConfigOption(name = "Select Node by Looking", desc = "Select the node you are pointing your cursor at.")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var selectRaycastKey: Int = KeyboardManager.KEY_UNKNOWN

    @Expose
    @ConfigOption(
        name = "Connect Key",
        desc = "Connect the nearest node with the active node. If the nodes are already connected removes the connection."
    )
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_C)
    var connectKey: Int = InputConstants.KEY_C

    @Expose
    @ConfigOption(name = "Exit Key", desc = "Exit out of text edit mode. If not in text edit mode, disables the graph editor.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_HOME)
    var exitKey: Int = InputConstants.KEY_HOME

    // TODO rename to nodeMoveKey
    @Expose
    @ConfigOption(
        name = "Node Move Key",
        desc = "While holding the Key, edit the position of the active node or the selection block with the minecraft movement controls."
    )
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_TAB)
    var editKey: Int = InputConstants.KEY_TAB

    @Expose
    @ConfigOption(name = "Text Key", desc = "Start text mode, which allows editing a name of a node.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_Y)
    var textKey: Int = InputConstants.KEY_Y

    // TODO rename to navigateToNodeKey
    @Expose
    @ConfigOption(
        name = "Navigate to Node",
        desc = "On key press, show the shortest path to the active node."
    )
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_G)
    var dijkstraKey: Int = InputConstants.KEY_G

    @Expose
    @ConfigOption(name = "Save Key", desc = "Save the current graph to the clipboard.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_O)
    var saveKey: Int = InputConstants.KEY_O

    @Expose
    @ConfigOption(name = "Load Key", desc = "Load a graph from clipboard, if valid.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_I)
    var loadKey: Int = InputConstants.KEY_I

    @Expose
    @ConfigOption(
        name = "Clear Key",
        desc = "Clear the graph. Also saves the graph to the clipboard, in case of a misclick."
    )
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var clearKey: Int = KeyboardManager.KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Vision Key", desc = "Toggle if the graph should render trough blocks.")
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_M)
    var throughBlocksKey: Int = InputConstants.KEY_M

    // TODO rename to feedbackKey
    @Expose
    @ConfigOption(
        name = "Feedback Key",
        desc = "Toggle the feedback mode. In this mode, you will get a chat message explaining on everything you do in the Graph Editor."
    )
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_K)
    var tutorialKey: Int = InputConstants.KEY_K

    @Expose
    @ConfigOption(
        name = "Split Key",
        desc = "Key for splitting an edge that is between the active and the closest node."
    )
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var splitKey: Int = KeyboardManager.KEY_UNKNOWN

    @Expose
    @ConfigOption(name = "Dissolve Key", desc = "Dissolve the active node into one edge if it only has two edges.")
    @ConfigEditorKeybind(defaultKey = KeyboardManager.KEY_UNKNOWN)
    var dissolveKey: Int = KeyboardManager.KEY_UNKNOWN

    // TODO rename to oneDirectionalKey
    @Expose
    @ConfigOption(
        name = "One Directional Key",
        desc = "Cycles the direction of the edge that is between the active and the closest node. (Used to make one-directional ways)"
    )
    @ConfigEditorKeybind(defaultKey = InputConstants.KEY_H)
    var edgeCycle: Int = InputConstants.KEY_H

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
