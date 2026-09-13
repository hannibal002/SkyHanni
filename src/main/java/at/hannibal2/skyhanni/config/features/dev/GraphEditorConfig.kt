package at.hannibal2.skyhanni.config.features.dev

import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.test.graph.GraphEditor
import at.hannibal2.skyhanni.utils.InputCode
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorButton
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
    @ConfigEditorKeyMapping(defaultKey = KEY_F)
    var placeKey = InputCode.KEY_F

    @Expose
    @ConfigOption(name = "Auto Select Node", desc = "Automatically select a node after placing it.")
    @ConfigEditorBoolean
    var autoSelectNode: Boolean = false

    // TODO rename to selectNearestNodeKey
    @Expose
    @ConfigOption(name = "Select Nearest Node", desc = "Select the nearest node to be active. Double press to unselect.")
    @ConfigEditorKeyMapping(defaultKey = LEFT_MOUSE)
    var selectKey = InputCode.LEFT_MOUSE

    // TODO rename to selectNodeByLookingKey
    @Expose
    @ConfigOption(name = "Select Node by Looking", desc = "Select the node you are pointing your cursor at.")
    @ConfigEditorKeyMapping
    var selectRaycastKey = InputCode.UNKNOWN

    @Expose
    @ConfigOption(
        name = "Connect Key",
        desc = "Connect the nearest node with the active node. If the nodes are already connected removes the connection."
    )
    @ConfigEditorKeyMapping(defaultKey = KEY_C)
    var connectKey = InputCode.KEY_C

    @Expose
    @ConfigOption(name = "Exit Key", desc = "Exit out of text edit mode. If not in text edit mode, disables the graph editor.")
    @ConfigEditorKeyMapping(defaultKey = KEY_RETURN)
    var exitKey = InputCode.KEY_RETURN

    // TODO rename to nodeMoveKey
    @Expose
    @ConfigOption(
        name = "Node Move Key",
        desc = "While holding the Key, edit the position of the active node or the selection block with the minecraft movement controls."
    )
    @ConfigEditorKeyMapping(defaultKey = KEY_TAB)
    var editKey = InputCode.KEY_TAB

    @Expose
    @ConfigOption(name = "Text Key", desc = "Start text mode, which allows editing a name of a node.")
    @ConfigEditorKeyMapping(defaultKey = KEY_Y)
    var textKey = InputCode.KEY_Y

    // TODO rename to navigateToNodeKey
    @Expose
    @ConfigOption(
        name = "Navigate to Node",
        desc = "On key press, show the shortest path to the active node."
    )
    @ConfigEditorKeyMapping(defaultKey = KEY_G)
    var dijkstraKey = InputCode.KEY_G

    @Expose
    @ConfigOption(name = "Save Key", desc = "Save the current graph to the clipboard.")
    @ConfigEditorKeyMapping(defaultKey = KEY_O)
    var saveKey = InputCode.KEY_O

    @Expose
    @ConfigOption(name = "Load Key", desc = "Load a graph from clipboard, if valid.")
    @ConfigEditorKeyMapping(defaultKey = KEY_I)
    var loadKey = InputCode.KEY_I

    @Expose
    @ConfigOption(
        name = "Clear Key",
        desc = "Clear the graph. Also saves the graph to the clipboard, in case of a misclick."
    )
    @ConfigEditorKeyMapping
    var clearKey = InputCode.UNKNOWN

    @Expose
    @ConfigOption(name = "Vision Key", desc = "Toggle if the graph should render trough blocks.")
    @ConfigEditorKeyMapping(defaultKey = KEY_M)
    var throughBlocksKey = InputCode.KEY_M

    // TODO rename to feedbackKey
    @Expose
    @ConfigOption(
        name = "Feedback Key",
        desc = "Toggle the feedback mode. In this mode, you will get a chat message explaining on everything you do in the Graph Editor."
    )
    @ConfigEditorKeyMapping(defaultKey = KEY_K)
    var tutorialKey = InputCode.KEY_K

    @Expose
    @ConfigOption(
        name = "Split Key",
        desc = "Key for splitting an edge that is between the active and the closest node."
    )
    @ConfigEditorKeyMapping
    var splitKey = InputCode.UNKNOWN

    @Expose
    @ConfigOption(name = "Dissolve Key", desc = "Dissolve the active node into one edge if it only has two edges.")
    @ConfigEditorKeyMapping
    var dissolveKey = InputCode.UNKNOWN

    // TODO rename to oneDirectionalKey
    @Expose
    @ConfigOption(
        name = "One Directional Key",
        desc = "Cycles the direction of the edge that is between the active and the closest node. (Used to make one-directional ways)"
    )
    @ConfigEditorKeyMapping(defaultKey = KEY_H)
    var edgeCycle = InputCode.KEY_H

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
