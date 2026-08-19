package at.hannibal2.skyhanni.features.misc.pathfind

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.arguments.EnumArgumentType
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.model.graph.Graph
import at.hannibal2.skyhanni.data.model.graph.GraphNode
import at.hannibal2.skyhanni.data.model.waypoints.WaypointFormats
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object NavigateClipboardCommand {

    private const val CLIPBOARD_TARGET_NAME = "Clipboard Waypoint"
    private const val MAX_SNAP_DISTANCE = 20

    private val defaultClipboardWaitTime = 5.seconds

    private enum class ClipboardRouteMode {
        ORDERED,
        OPTIMIZED,
    }

    private class ParsedLocations(val locations: List<LorenzVec>, val unreadableLines: Int, val format: String?)

    private class SnappedLocations(val byNode: Map<GraphNode, LorenzVec>, val tooFar: Int, val merged: Int)

    /**
     * Navigate to all locations read from the clipboard.
     *
     * Every location is mapped to the closest node of the island graph, since only those can be routed to.
     * Locations further than `MAX_SNAP_DISTANCE` blocks from any node are dropped, and locations sharing a node
     * are merged into the first one.
     * The navigation still targets the original location, the node is only used to order the route and to tell
     * apart "the path finder is done" from "the location itself is reached".
     *
     * @param mode Whether the locations are reordered into the shortest route or visited as they were pasted.
     * @param waitTime How long the player has to stand at a waypoint before the next one is targeted.
     */
    private fun navigateAllClipboardCommand(mode: ClipboardRouteMode, waitTime: Duration) {
        val graph = IslandGraphs.currentIslandGraph
        if (graph == null || graph.none { it.enabled }) {
            ChatUtils.userError("There is no path finder network on this island.")
            return
        }

        val clipboard = ClipboardUtils.readFromClipboard().orEmpty()
        if (clipboard.isBlank()) {
            ChatUtils.userError("Your clipboard is empty. Copy one §ex:y:z §clocation per line first.")
            return
        }

        val parsed = readLocations(clipboard)
        if (parsed.locations.isEmpty()) {
            ChatUtils.userError("Could not read any location from your clipboard. Expected one §ex:y:z §cper line.")
            return
        }

        val snapped = snapToNodes(graph, parsed.locations)
        val nodes = snapped.byNode.keys.toList()
        if (nodes.isEmpty()) {
            ChatUtils.userError("No location is within $MAX_SNAP_DISTANCE blocks of the path finder network on this island.")
            return
        }

        val notes = buildList {
            parsed.format?.let { add("§e$it §7format") }
            if (parsed.unreadableLines > 0) add("${parsed.unreadableLines} unreadable")
            if (snapped.tooFar > 0) add("${snapped.tooFar} too far away")
            if (snapped.merged > 0) add("${snapped.merged} mapped to the same node")
        }

        val noteSuffix = if (notes.isEmpty()) "" else " §7(${notes.joinToString("§7, ")}§7)"
        ChatUtils.chat("Read ${StringUtils.pluralize(nodes.size, "location", withNumber = true)} from the clipboard.$noteSuffix")

        var arrivedAt = SimpleTimeMark.farPast()

        NavigateAllApi.navigateAll(
            nodes,
            CLIPBOARD_TARGET_NAME,
            LorenzColor.AQUA.toColor(),
            onFound = { arrivedAt = SimpleTimeMark.now() },
            onFinish = {
                ChatUtils.chat("Reached all ${StringUtils.pluralize(nodes.size, CLIPBOARD_TARGET_NAME, withNumber = true)}§e.")
            },
            continueNavigationCondition = NavigationCondition.SecondPassed { arrivedAt.passedSince() >= waitTime },
            condition = { true },
            optimizeRoute = mode == ClipboardRouteMode.OPTIMIZED,
            renderNumber = true,
            warnWhenUnreachable = true,
            targetLocation = { snapped.byNode[it] ?: it.position },
            skipCommand = "/shnavclipboard skip",
        )
    }

    // TODO add a Skyblocker waypoint format
    private fun readLocations(clipboard: String): ParsedLocations {
        WaypointFormats.load(clipboard)?.let { (waypoints, format) ->
            return ParsedLocations(waypoints.map { it.location }, unreadableLines = 0, format = format)
        }

        val lines = clipboard.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val locations = lines.mapNotNull { parseLocationOrNull(it) }
        return ParsedLocations(locations, unreadableLines = lines.size - locations.size, format = null)
    }

    private fun parseLocationOrNull(line: String): LorenzVec? {
        val parts = line.split(":")
        if (parts.size != 3) return null
        val (x, y, z) = parts.map { it.trim().toDoubleOrNull() ?: return null }
        return LorenzVec(x, y, z)
    }

    private fun snapToNodes(graph: Graph, locations: List<LorenzVec>): SnappedLocations {
        val snapped = LinkedHashMap<GraphNode, LorenzVec>()
        var tooFar = 0
        var merged = 0

        for (location in locations) {
            val node = graph.getNearestNode(location)
            if (node.position.distance(location) > MAX_SNAP_DISTANCE) {
                tooFar++
                continue
            }
            if (snapped.putIfAbsent(node, location) != null) merged++
        }

        return SnappedLocations(snapped, tooFar, merged)
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shnavclipboard") {
            description = "Use the path finder to go to all locations read from the clipboard"
            category = CommandCategory.DEVELOPER_TEST

            arg("mode", EnumArgumentType.lowercase<ClipboardRouteMode>()) { mode ->
                argCallback("seconds", BrigadierArguments.integer(min = 0)) { seconds ->
                    navigateAllClipboardCommand(getArg(mode), seconds.seconds)
                }
                callback {
                    navigateAllClipboardCommand(getArg(mode), defaultClipboardWaitTime)
                }
            }
            literalCallback("skip") {
                NavigateAllApi.handleSkip()
            }
            literalCallback("stop") {
                NavigateAllApi.handleStop(manual = true)
            }
            simpleCallback {
                navigateAllClipboardCommand(ClipboardRouteMode.OPTIMIZED, defaultClipboardWaitTime)
            }
        }
    }
}
