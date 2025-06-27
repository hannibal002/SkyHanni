package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.waypoints.SkyhanniWaypoint
import at.hannibal2.skyhanni.data.model.waypoints.WaypointFormat
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
import at.hannibal2.skyhanni.utils.SpecialColor.toSpecialColor
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawEdges
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.ServiceLoader

@SkyHanniModule
object OrderedWaypoints {
    private val config get() = SkyHanniMod.feature.mining.orderedWaypoints

    private var orderedWaypointsList = Waypoints<SkyhanniWaypoint>()
    private val renderWaypoints: MutableList<Int> = mutableListOf()
    private var currentOrderedWaypointIndex = 0
    private var lastCloser = 0

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return

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
        event.registerBrigadier("shorderedload") {
            description = "Loads the ordered waypoints from your clipboard or config."
            category = CommandCategory.USERS_ACTIVE
            arg("name", BrigadierArguments.string()) { name ->
                callback { load(getArg(name)) }
            }
            callback { load("") }
            aliases = generateAliases(listOf("load", "import"))
        }

        event.registerBrigadier("shorderedunload") {
            description = "Unloads the current ordered waypoints."
            category = CommandCategory.USERS_ACTIVE
            callback { unload() }
            aliases = generateAliases(listOf("unload", "clear"))
        }

        event.registerBrigadier("shorderedskip") {
            description = "Skips the next inputted number many waypoints."
            category = CommandCategory.USERS_ACTIVE
            arg("amount", BrigadierArguments.integer()) { amount ->
                callback { skip(getArg(amount)) }
            }
            simpleCallback { skip(1) }
            aliases = generateAliases(listOf("skip"))
        }

        event.registerBrigadier("shorderedskipto") {
            description = "Skips to the waypoint with the inputted number."
            category = CommandCategory.USERS_ACTIVE
            arg("number", BrigadierArguments.integer()) { number ->
                callback { skipto(getArg(number)) }
            }
            aliases = generateAliases(listOf("skipto"))
        }

        event.registerBrigadier("shorderedunskip") {
            description = "Goes back by the number inputted many waypoints."
            category = CommandCategory.USERS_ACTIVE
            arg("amount", BrigadierArguments.integer()) { amount ->
                callback { unskip(getArg(amount)) }
            }
            aliases = generateAliases(listOf("unskip"))
        }

        event.registerBrigadier("shordereddelete") {
            description = "Deletes the waypoint with the inputted number."
            category = CommandCategory.USERS_ACTIVE
            arg("number", BrigadierArguments.integer()) { number ->
                callback { delete(getArg(number)) }
            }
            aliases = generateAliases(listOf("delete", "remove"))
        }

        event.registerBrigadier("shorderedadd") {
            description = "Inserts a waypoint with the specified numbering below the player."
            category = CommandCategory.USERS_ACTIVE
            arg("number", BrigadierArguments.integer()) { number ->
                callback { add(getArg(number)) }
            }
            aliases = generateAliases(listOf("add", "insert"))
        }

        event.registerBrigadier("shorderedexport") {
            description = "Exports the loaded ordered waypoints to clipboard."
            category = CommandCategory.USERS_ACTIVE
            arg("format", BrigadierArguments.string()) { format ->
                callback { export(getArg(format)) }
            }
            simpleCallback { export("coleweight") }
            aliases = generateAliases(listOf("export"))
        }

        event.registerBrigadier("shorderedsave") {
            description = "Saves the loaded ordered waypoints to your config."
            category = CommandCategory.USERS_ACTIVE
            arg("name", BrigadierArguments.string()) { name ->
                callback { save(getArg(name)) }
            }
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

    private fun load(name: String) {
        SkyHanniMod.coroutineScope.launch {
            val res = if (name.isEmpty()) {
                loadWaypoints(ClipboardUtils.readFromClipboard().orEmpty())
            } else {
                ProfileStorageData.playerSpecific?.routes?.get(name) ?: run {
                    ChatUtils.chat("Route $name doesn't exist.")
                    return@launch
                }
            }

            res?.let {
                ChatUtils.chat(it.waypoints.toString())
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

    private fun skip(amount: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to skip!")
        }

        incrementIndex(amount)
        ChatUtils.chat("Skipped $amount ${StringUtils.pluralize(amount, "waypoint")}.")
    }

    private fun skipto(number: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to skip to!")
        }

        val newOrderedWaypointIndex = number - 1
        if (0 <= newOrderedWaypointIndex && newOrderedWaypointIndex < orderedWaypointsList.size) {
            currentOrderedWaypointIndex = newOrderedWaypointIndex
            ChatUtils.chat("Skipped to ${currentOrderedWaypointIndex + 1}.")
        } else {
            ChatUtils.chat("$number is not between 1 and ${orderedWaypointsList.size}.")
        }
    }

    private fun unskip(amount: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to unskip!")
        }

        incrementIndex(-amount)

        ChatUtils.chat("Unskipped $amount waypoints.")
    }

    private fun delete(number: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to delete!")
        }

        if (number < 1 || number > orderedWaypointsList.size) {
            return ChatUtils.chat("Invalid number! Must be between 1 and ${orderedWaypointsList.size}.")
        }

        for (i in number - 1 until orderedWaypointsList.size) {
            orderedWaypointsList[i].options["name"] = orderedWaypointsList[i].number.dec().toString()
            orderedWaypointsList[i].number--
        }
        orderedWaypointsList.removeAt(number - 1)
        renderWaypoints.clear()

        ChatUtils.chat("Removed waypoint $number.")
    }

    private fun add(number: Int) {
        val pos = LocationUtils.playerLocation().add(0, -1, 0).roundLocationToBlock()

        if (number < 1 || number > orderedWaypointsList.size + 1) {
            return ChatUtils.chat("Please enter a number between 1 and ${orderedWaypointsList.size + 1})")
        }

        val newWaypoint = SkyhanniWaypoint(pos, number = number, options = mutableMapOf("name" to number.toString()))
        if (number == orderedWaypointsList.size + 1) {
            orderedWaypointsList.add(newWaypoint)
        } else {
            for (i in number - 1 until orderedWaypointsList.size) {
                orderedWaypointsList[i].options["name"] = orderedWaypointsList[i].number.inc().toString()
                orderedWaypointsList[i].number++
            }
            orderedWaypointsList.add(number - 1, newWaypoint)
        }
        ChatUtils.chat("Inserted waypoint $number at ${pos.toCleanString()}.")
    }

    private fun export(format: String) {
        SkyHanniMod.coroutineScope.launch {
            val route = if (format.isEmpty()) exportWaypoints(orderedWaypointsList, "coleweight")
            else exportWaypoints(orderedWaypointsList, format.lowercase(Locale.getDefault()))

            route?.let {
                ClipboardUtils.copyToClipboard(it)
                ChatUtils.chat("Route was copied to clipboard!")
            } ?: run {
                ChatUtils.chat("Invalid waypoint format specified.")
            }
        }
    }

    private fun save(name: String) {
        ProfileStorageData.playerSpecific?.routes?.put(name, orderedWaypointsList.deepCopy())
        ChatUtils.chat("Route saved as $name. Do /shorderedload $name to import it.")
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

    private fun loadWaypoints(data: String): Waypoints<SkyhanniWaypoint>? {
        return ServiceLoader.load(WaypointFormat::class.java).firstNotNullOfOrNull {
            it.load(data)
        }?.let {
            Waypoints(it.toMutableList())
        }
    }

    private fun exportWaypoints(waypoints: Waypoints<SkyhanniWaypoint>, name: String): String? {
        return ServiceLoader.load(WaypointFormat::class.java).firstOrNull { it.name == name }?.export(waypoints)
    }
}
