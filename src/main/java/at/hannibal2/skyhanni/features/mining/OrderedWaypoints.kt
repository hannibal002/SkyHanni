package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.waypoints.SkyhanniWaypoint
import at.hannibal2.skyhanni.data.model.waypoints.Waypoints
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawEdges
import at.hannibal2.skyhanni.utils.RenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.WaypointParserUtils
import kotlinx.coroutines.launch

@SkyHanniModule
object OrderedWaypoints {
    private val config get() = SkyHanniMod.feature.mining.orderedWaypoints

    private var orderedWaypointsList = Waypoints<SkyhanniWaypoint>()
    private val renderWaypoints: MutableList<Int> = mutableListOf()
    private var currentOrderedWaypointIndex = 0
    private var lastCloser = 0
    private var enabled = true

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!enabled) return

        for (i in renderWaypoints.indices) {
            val wpColor = if (!config.showAll) {
                when (i) {
                    0 -> config.previousWaypointColor.toSpecialColor()
                    1 -> config.currentWaypointColor.toSpecialColor()
                    2 -> config.nextWaypointColor.toSpecialColor()
                    else -> LorenzColor.RED.toColor()
                }
            } else {
                orderedWaypointsList[renderWaypoints[i]].color
            }

            if (orderedWaypointsList.size <= renderWaypoints[i]) {
                ChatUtils.debug("${renderWaypoints[i]} $i")
                continue
            }

            // Outline
            event.drawEdges(
                orderedWaypointsList[renderWaypoints[i]].location,
                wpColor,
                1,
                false
            )

            if (config.showAll || i < 3) {
                // Waypoint name (number)
                event.drawString(
                    orderedWaypointsList[renderWaypoints[i]].location.add(0.5, 2.5, 0.5),
                    "${LorenzColor.YELLOW.getChatColor()}${orderedWaypointsList[renderWaypoints[i]].number}",
                    seeThroughBlocks = true
                )
            }

            if (config.showDistance) {
                // Distance
                event.drawString(
                    orderedWaypointsList[renderWaypoints[i]].location.add(0.5, 2.0, 0.5),
                    "${
                        LorenzColor.YELLOW.getChatColor() + orderedWaypointsList[renderWaypoints[i]].location.distanceToPlayer().roundTo(1).addSeparators()
                    } m",
                    seeThroughBlocks = true
                )
            }
        }

        val traceWP = orderedWaypointsList[renderWaypoints.getOrNull(2) ?: return decideWaypoints()]
        val lineColor = config.traceLineColor.toSpecialColor()
        if (!config.setupMode && config.traceLine && !config.showAll) {
            event.drawLineToEye(
                traceWP.location.add(0.5, 0.25, 0.5),
                lineColor,
                config.traceLineThickness.toInt(),
                false
            )
        }

        val currentWP = orderedWaypointsList[renderWaypoints.getOrNull(1) ?: return decideWaypoints()]
        if (config.setupMode && !config.showAll) {
            event.draw3DLine(
                currentWP.location.add(0.5, 2.65, 0.5),
                traceWP.location.add(0.5, 0.5, 0.5),
                lineColor,
                config.setupModeLineThickness.toInt(),
                depth = false
            )
        }

        decideWaypoints()
    }

    @HandleEvent(WorldChangeEvent::class)
    fun onWorldChange() {
        currentOrderedWaypointIndex = 0
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shorderedload") {
            description = "Loads the ordered waypoints from your clipboard or config."
            category = CommandCategory.USERS_ACTIVE
            callback { load(it) }
            aliases = generateAliases(listOf("load", "import"))
        }

        event.register("shorderedunload") {
            description = "Unloads the current ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { unload() }
            aliases = generateAliases(listOf("unload", "clear"))
        }

        event.register("shorderedskip") {
            description = "Skips the next <argument> number of waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { skip(it) }
            aliases = generateAliases(listOf("skip"))
        }

        event.register("shorderedskipto") {
            description = "Skips to the <argument> waypoint."
            category = CommandCategory.USERS_ACTIVE
            callback { skipto(it) }
            aliases = generateAliases(listOf("skipto"))
        }

        event.register("shorderedunskip") {
            description = "Goes back <argument> waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { unskip(it) }
            aliases = generateAliases(listOf("unskip"))
        }

        event.register("shorderedenable") {
            description = "Enables the ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { enable() }
            aliases = generateAliases(listOf("enable"))
        }

        event.register("shordereddisable") {
            description = "Disables the ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { disable() }
            aliases = generateAliases(listOf("disable"))
        }

        event.register("shordereddelete") {
            description = "Deletes the <argument>-th waypoint."
            category = CommandCategory.USERS_ACTIVE
            callback { delete(it) }
            aliases = generateAliases(listOf("delete", "remove"))
        }

        event.register("shorderedadd") {
            description = "Adds a waypoint."
            category = CommandCategory.USERS_ACTIVE
            callback { add(it) }
            aliases = generateAliases(listOf("add", "insert"))
        }

        event.register("shorderedexport") {
            description = "Exports the loaded ordered waypoints to clipboard."
            category = CommandCategory.USERS_ACTIVE
            callback { export(it) }
            aliases = generateAliases(listOf("export"))
        }

        event.register("shorderedsave") {
            description = "Saves the loaded ordered waypoints to your config."
            category = CommandCategory.USERS_ACTIVE
            callback { save(it) }
            aliases = generateAliases(listOf("save"))
        }
    }

    // Assumes that commands[0] has already been registered as `/shordered$commands[0]`
    private fun generateAliases(commands: List<String>): List<String> {
        val prefixes = listOf("sho")
        return buildList {
            prefixes.forEach { prefix ->
                addAll(commands.map { "$prefix$it" })
            }

            commands.drop(1).forEach {
                add("shordered$it")
            }
        }
    }

    private fun load(args: Array<String>) {
        SkyHanniMod.coroutineScope.launch {
            val res = if (args.isEmpty()) {
                WaypointParserUtils.loadWaypoints(ClipboardUtils.readFromClipboard().orEmpty())
            } else {
                ProfileStorageData.playerSpecific?.routes?.get(args[0]) ?: run {
                    ChatUtils.chat("Route ${args[0]} doesn't exist.")
                    return@launch
                }
            }

            res?.let {
                orderedWaypointsList = it
                currentOrderedWaypointIndex = 0
                renderWaypoints.clear()
                ChatUtils.chat("Loaded ordered waypoints!")
            } ?: run {
                ChatUtils.chat(
                    "There was an error parsing waypoints. " +
                        "Please make sure they are Coleweight formatted waypoints."
                )
                return@launch
            }

            orderedWaypointsList.sortedBy { item -> item.number }
        }
    }

    private fun unload() {
        orderedWaypointsList.clear()
        renderWaypoints.clear()
        currentOrderedWaypointIndex = 0
        lastCloser = 0
        ChatUtils.chat("Unloaded ordered waypoints!")
    }

    private fun skip(args: Array<String>) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to skip!")
        }

        val amountToSkip = if (args.isNotEmpty()) {
            args[0].toIntOrNull() ?: run {
                return ChatUtils.chat("${args[0]} is not an integer!")
            }
        } else 1

        incrementIndex(amountToSkip)
        ChatUtils.chat("Skipped $amountToSkip ${StringUtils.pluralize(amountToSkip, "waypoint")}.")
    }

    private fun skipto(args: Array<String>) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to skip to!")
        }

        val newOrderedWaypointIndex = args.getOrNull(0)?.toIntOrNull()?.minus(1)
            ?: return ChatUtils.chat("Please enter a number between between 1 and ${orderedWaypointsList.size}.")
        if (0 <= newOrderedWaypointIndex && newOrderedWaypointIndex < orderedWaypointsList.size) {
            currentOrderedWaypointIndex = newOrderedWaypointIndex
            ChatUtils.chat("Skipped to ${currentOrderedWaypointIndex + 1}.")
        } else {
            ChatUtils.chat("${newOrderedWaypointIndex + 1} is not between 1 and ${orderedWaypointsList.size}.")
        }
    }

    private fun unskip(args: Array<String>) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to unskip!")
        }

        val decrement = if (args.isNotEmpty()) {
            args[0].toIntOrNull() ?: run {
                return ChatUtils.chat("Argument is not an integer!")
            }
        } else 1

        incrementIndex(-decrement)

        ChatUtils.chat("Unskipped $decrement waypoints.")
    }

    private fun enable() {
        enabled = true
        ChatUtils.chat("Enabled ordered waypoints.")
    }

    private fun disable() {
        enabled = false
        ChatUtils.chat("Disabled ordered waypoints.")
    }

    private fun delete(args: Array<String>) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to delete!")
        }

        val wNum = args.getOrNull(0)?.toIntOrNull() ?: run {
            return ChatUtils.chat("Usage: /shordereddelete (number)")
        }

        if (wNum < 1 || wNum > orderedWaypointsList.size) {
            return ChatUtils.chat("Invalid number! Must be between 1 and ${orderedWaypointsList.size}.")
        }

        for (i in wNum - 1 until orderedWaypointsList.size) {
            orderedWaypointsList[i].options["name"] = orderedWaypointsList[i].number.dec().toString()
            orderedWaypointsList[i].number--
        }
        orderedWaypointsList.removeAt(wNum - 1)
        renderWaypoints.clear()

        ChatUtils.chat("Removed waypoint $wNum.")
    }

    private fun add(args: Array<String>) {
        val pos = LocationUtils.playerLocation().add(0, -1, 0).roundLocationToBlock()
        val wNum = args.getOrNull(0)?.toIntOrNull()

        if (wNum == null || wNum < 1 || wNum > orderedWaypointsList.size + 1) {
            return ChatUtils.chat("Please enter a number between 1 and ${orderedWaypointsList.size + 1})")
        }

        val newWaypoint = SkyhanniWaypoint(pos, number = wNum, options = mutableMapOf("name" to wNum.toString()))
        if (wNum == orderedWaypointsList.size + 1) {
            orderedWaypointsList.add(newWaypoint)
        } else {
            for (i in wNum - 1 until orderedWaypointsList.size) {
                orderedWaypointsList[i].options["name"] = orderedWaypointsList[i].number.inc().toString()
                orderedWaypointsList[i].number++
            }
            orderedWaypointsList.add(wNum - 1, newWaypoint)
        }
        ChatUtils.chat("Inserted waypoint $wNum at ${pos.toCleanString()}.")
    }

    private fun export(args: Array<String>) {
        SkyHanniMod.coroutineScope.launch {
            val route = if (args.isEmpty()) WaypointParserUtils.exportWaypoints(orderedWaypointsList)
            else WaypointParserUtils.exportWaypoints(orderedWaypointsList, args[0])

            route?.let {
                ClipboardUtils.copyToClipboard(it)
                ChatUtils.chat("Route was copied to clipboard!")
            } ?: run {
                ChatUtils.chat("Invalid waypoint format specified.")
            }
        }
    }

    private fun save(args: Array<String>) {
        if (args.isEmpty()) {
            return ChatUtils.chat("Usage: /shorderedsave (name).")
        }

        ProfileStorageData.playerSpecific?.routes?.put(args[0], orderedWaypointsList)
        ChatUtils.chat("Route saved as ${args[0]}. Do /shorderedload ${args[0]} to import it.")
    }

    private fun decideWaypoints() {
        renderWaypoints.clear()
        if (orderedWaypointsList.isEmpty()) return

        val beforeWaypoint = orderedWaypointsList.getOrNull(currentOrderedWaypointIndex - 1)
        if (beforeWaypoint != null) {
            renderWaypoints.add(beforeWaypoint.number - 1)
        } else {
            renderWaypoints.add(orderedWaypointsList[orderedWaypointsList.size - 1].number - 1)
        }

        val currentWaypoint = orderedWaypointsList.getOrNull(currentOrderedWaypointIndex)

        var distanceTo1 = Double.POSITIVE_INFINITY
        if (currentWaypoint != null) {
            distanceTo1 = currentWaypoint.location.distanceToPlayer()
            renderWaypoints.add(currentWaypoint.number - 1)
        }

        val nextWaypoint = orderedWaypointsList.getOrNull(currentOrderedWaypointIndex + 1)
            ?: orderedWaypointsList.getOrNull(0)

        var distanceTo2 = Double.POSITIVE_INFINITY
        if (nextWaypoint != null) {
            distanceTo2 = nextWaypoint.location.distanceToPlayer()

            renderWaypoints.add(nextWaypoint.number - 1)
        }

        if (
            lastCloser == currentOrderedWaypointIndex &&
            distanceTo1 > distanceTo2 &&
            distanceTo2 < config.waypointRange
        ) {
            return incrementIndex(1)
        }

        if (distanceTo1 < config.waypointRange.toDouble()) {
            lastCloser = currentOrderedWaypointIndex
        }

        if (distanceTo2 < config.waypointRange.toDouble()) {
            incrementIndex(1)
        }

        orderedWaypointsList.filter {
            val inSetupRange = config.setupMode &&
                !renderWaypoints.contains(it.number - 1) &&
                it.location.distanceToPlayer() < config.setupModeRange
            inSetupRange || config.showAll
        }.forEach {
                waypoint ->
            renderWaypoints.add(waypoint.number - 1)
        }
    }

    private fun incrementIndex(increment: Int) {
        currentOrderedWaypointIndex = Math.floorMod(currentOrderedWaypointIndex + increment, orderedWaypointsList.size)
    }
}
