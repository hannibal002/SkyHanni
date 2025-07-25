package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.waypoints.SkyhanniWaypoint
import at.hannibal2.skyhanni.data.model.waypoints.WaypointFormat
import at.hannibal2.skyhanni.data.model.waypoints.Waypoints
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawEdges
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawLineToEye
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import java.util.Locale
import java.util.ServiceLoader

@SkyHanniModule
object OrderedWaypoints {
    private val config get() = SkyHanniMod.feature.mining.orderedWaypoints

    private var orderedWaypointsList = Waypoints<SkyhanniWaypoint>()
    private val renderWaypoints: MutableList<Int> = mutableListOf()
    private var currentOrderedWaypointIndex = 0
    private var lastCloser = 0

    @HandleEvent(HypixelJoinEvent::class)
    fun onHypixelJoin() {
        if (SkyHanniMod.orderedWaypointsRoutesData.routes == null) {
            SkyHanniMod.orderedWaypointsRoutesData.routes = mutableMapOf()
            saveConfig()
        }
    }

    fun saveConfig() {
        SkyHanniMod.configManager.saveConfig(ConfigFileType.ROUTES, "Save file")
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return

        for (i in renderWaypoints.indices) {
            val wpColor = if (!config.showAll) {
                when (i) {
                    0 -> config.previousWaypointColor
                    1 -> config.currentWaypointColor
                    in 2..(1 + config.nextCount.toInt()) -> config.nextWaypointColor
                    else -> config.setupModeColor
                }
            } else config.showAllWaypointColor

            if (orderedWaypointsList.size <= renderWaypoints[i]) {
                ChatUtils.debug("${renderWaypoints[i]} $i")
                continue
            }

            when (config.fillBlock) {
                true -> event.drawWaypointFilled(
                    orderedWaypointsList[renderWaypoints[i]].location,
                    wpColor.toColor(),
                    true,
                )

                else -> event.drawEdges(
                    orderedWaypointsList[renderWaypoints[i]].location,
                    wpColor.toColor(),
                    config.blockOutlineThickness.toInt(),
                    false,
                )
            }

            if (config.setupMode || config.showAll || i in 0..(1 + config.nextCount.toInt())) {
                // Waypoint name (number)
                event.drawString(
                    orderedWaypointsList[renderWaypoints[i]].location.add(0.5, 2.5, 0.5),
                    "§e${orderedWaypointsList[renderWaypoints[i]].number}",
                    seeThroughBlocks = true,
                )
            }

            if (config.showDistance) {
                // Distance
                event.drawString(
                    orderedWaypointsList[renderWaypoints[i]].location.add(0.5, 2.0, 0.5),
                    "§e${orderedWaypointsList[renderWaypoints[i]].location.distanceToPlayer().roundTo(1).addSeparators()} m",
                    seeThroughBlocks = true,
                )
            }
        }

        if (renderWaypoints.size <= 1) return decideWaypoints()

        val traceWP = if (renderWaypoints.size == 2) orderedWaypointsList[renderWaypoints[0]]
        else orderedWaypointsList[renderWaypoints[2]]
        val traceLineColor = config.traceLineColor
        if (config.traceLine && !config.showAll && !config.setupMode) {
            event.drawLineToEye(
                traceWP.location.add(0.5, 0.25, 0.5),
                traceLineColor,
                config.traceLineThickness.toInt(),
                depth = true,
            )
        }

        val currentWP = orderedWaypointsList[renderWaypoints[1]]
        val setupModeLineColor = config.setupModeLineColor
        if (config.setupMode && !config.showAll) {
            val eyePos = if (config.sneakingDuringRoute) 1.54
            else 1.62
            event.draw3DLine(
                currentWP.location.add(0.5, 1.0 + eyePos, 0.5),
                traceWP.location.add(0.5, 0.5, 0.5),
                setupModeLineColor,
                config.setupModeLineThickness.toInt(),
                depth = true,
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
        event.registerBrigadier("shordered") {
            description = "Ordered Waypoints commands."
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("sho")
            literal("load", "import") {
                description = "Loads ordered waypoints from your clipboard or config."
                arg(
                    "name", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { getRouteNames() },
                ) { name ->
                    callback { load(getArg(name)) }
                }
                simpleCallback { load("") }
            }
            literal("unload", "clear") {
                description = "Unloads the current ordered waypoints."
                simpleCallback { unload() }
            }
            literal("skip") {
                description = "Skips the next waypoint."
                arg("amount", BrigadierArguments.integer()) { amount ->
                    callback { skip(getArg(amount)) }
                }
                simpleCallback { skip(1) }
            }
            literal("skipto") {
                description = "Skips to the waypoint with the inputted number."
                arg("number", BrigadierArguments.integer()) { number ->
                    callback { skipto(getArg(number)) }
                }
                simpleCallback { skipto(1) }
            }
            literal("unskip") {
                description = "Goes back by the number inputted many waypoints."
                arg("amount", BrigadierArguments.integer()) { amount ->
                    callback { unskip(getArg(amount)) }
                }
                simpleCallback { unskip(1) }
            }
            literal("delete", "remove") {
                description = "Deletes the waypoint with the inputted number."
                arg("number", BrigadierArguments.integer()) { number ->
                    callback { delete(getArg(number)) }
                }
            }
            literal("add", "insert") {
                description = "Inserts a waypoint with the specified numbering below the player."
                arg("number", BrigadierArguments.integer()) { number ->
                    callback { add(getArg(number)) }
                }
            }
            literal("export") {
                description = "Exports the loaded ordered waypoints to clipboard."
                arg("format", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { getWaypointFormats() }) { format ->
                    callback { export(getArg(format)) }
                }
                simpleCallback { export("coleweight") }
            }
            literal("save") {
                description = "Saves the loaded ordered waypoints to your config."
                arg("name", BrigadierArguments.string()) { name ->
                    callback { save(getArg(name)) }
                }
            }
            literal("erase", "delete-route") {
                description = "Erases the route with the specified name."
                arg("name", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { getRouteNames() }) { name ->
                    callback { erase(getArg(name)) }
                }
            }
            literal("setupmode") {
                description = "Toggles setup mode."
                argCallback("enable", BrigadierArguments.bool()) { enableSetupMode ->
                    toggleSetupMode(enableSetupMode)
                }
                simpleCallback { toggleSetupMode(!config.setupMode) }
            }
        }
    }

    private fun getRouteNames() = ProfileStorageData.orderedWaypointsRoutes?.routes?.keys.orEmpty()

    private fun load(name: String) {
        SkyHanniMod.launchIOCoroutine {
            val res = if (name == "") {
                loadWaypoints(ClipboardUtils.readFromClipboard().orEmpty())
            } else {
                val routes = ProfileStorageData.orderedWaypointsRoutes?.routes
                routes?.get(name) ?: run {
                    ChatUtils.userError(
                        "Route $name doesn't exist.\n" +
                            "§cSaved Routes: ${routes?.keys?.toList()?.joinToString(", ")}\n" +
                            "§cIf you would like to import a route from your clipboard, leave the route name blank.",
                    )
                    return@launchIOCoroutine
                }
            }

            res?.let {
                orderedWaypointsList = it.deepCopy()
                orderedWaypointsList.sortedBy { waypoint -> waypoint.number }
                currentOrderedWaypointIndex = orderedWaypointsList.minBy { waypoint -> waypoint.location.distanceSqToPlayer() }.number - 1
                renderWaypoints.clear()
                ChatUtils.chat("Loaded ordered waypoints!")
            } ?: run {
                ChatUtils.userError(
                    "There was an error parsing waypoints. " +
                        "Please make sure they are properly formatted and in a supported format.\n" +
                        "§cSupported Formats: ${getWaypointFormats().joinToString(", ")}",
                )
                return@launchIOCoroutine
            }
        }
    }

    private fun unload() {
        orderedWaypointsList.clear()
        renderWaypoints.clear()
        currentOrderedWaypointIndex = 0
        lastCloser = 0
        ChatUtils.chat("Unloaded ordered waypoints.")
    }

    private fun skip(amount: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.userError("There are no waypoints to skip.")
        }

        incrementIndex(amount)
        ChatUtils.chat("Skipped $amount ${StringUtils.pluralize(amount, "waypoint")}.")
    }

    private fun skipto(number: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.chat("There are no waypoints to skip to.")
        }

        val newOrderedWaypointIndex = number - 1
        if (0 <= newOrderedWaypointIndex && newOrderedWaypointIndex < orderedWaypointsList.size) {
            currentOrderedWaypointIndex = newOrderedWaypointIndex
            ChatUtils.chat("Skipped to ${currentOrderedWaypointIndex + 1}.")
        } else {
            ChatUtils.userError("$number is not between 1 and ${orderedWaypointsList.size}.")
        }
    }

    private fun unskip(amount: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.userError("There are no waypoints to unskip.")
        }

        incrementIndex(-amount)

        ChatUtils.chat("Unskipped $amount waypoints.")
    }

    private fun delete(number: Int) {
        if (orderedWaypointsList.isEmpty()) {
            return ChatUtils.userError("There are no waypoints to delete.")
        }

        if (number < 1 || number > orderedWaypointsList.size) {
            return ChatUtils.userError("$number is not between 1 and ${orderedWaypointsList.size}.")
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
        val pos = LocationUtils.playerLocation().add(0, -1, 0).roundToBlock()

        if (number < 1 || number > orderedWaypointsList.size + 1) {
            return ChatUtils.userError("$number is not between 1 and ${orderedWaypointsList.size + 1}.")
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
        SkyHanniMod.launchIOCoroutine {
            val route = if (format.isEmpty()) exportWaypoints(orderedWaypointsList, "coleweight")
            else exportWaypoints(orderedWaypointsList, format.lowercase(Locale.getDefault()))

            route?.let {
                ClipboardUtils.copyToClipboard(it)
                ChatUtils.chat("Route was copied to clipboard.")
            } ?: run {
                ChatUtils.userError(
                    "Invalid waypoint format specified.\n" +
                        "§cFormats: ${getWaypointFormats().joinToString { ", " }}",
                )
            }
        }
    }

    private fun save(name: String) {
        ProfileStorageData.orderedWaypointsRoutes?.routes?.set(name, orderedWaypointsList.deepCopy())
        saveConfig()
        ChatUtils.chat("Route saved as $name. Do /sho load $name to import it.")
    }

    private fun erase(name: String) {
        ProfileStorageData.orderedWaypointsRoutes?.routes?.remove(name) ?: run {
            ChatUtils.userError("Route $name doesn't exist.")
            return
        }
        saveConfig()
        ChatUtils.chat("Route $name successfully deleted.")
    }

    private fun toggleSetupMode(value: Boolean) {
        config.setupMode = value
        ChatUtils.chat("Toggled setup mode to $value")
    }

    private fun decideWaypoints() {
        renderWaypoints.clear()
        if (orderedWaypointsList.isEmpty()) return

        val beforeWaypoint = orderedWaypointsList.getOrNull(currentOrderedWaypointIndex - 1)
            ?: orderedWaypointsList.last()
        renderWaypoints.add(beforeWaypoint.number - 1)

        val currentWaypoint = orderedWaypointsList.getOrNull(currentOrderedWaypointIndex)

        var distanceTo1 = Double.POSITIVE_INFINITY
        if (currentWaypoint != null) {
            distanceTo1 = currentWaypoint.location.distanceToPlayer()
            renderWaypoints.add(currentWaypoint.number - 1)
        }

        val nextWaypoint = orderedWaypointsList.getOrNull(currentOrderedWaypointIndex + 1)
            ?: orderedWaypointsList.first()

        val distanceTo2 = nextWaypoint.location.distanceToPlayer()
        if (nextWaypoint.number - 1 !in renderWaypoints) renderWaypoints.add(nextWaypoint.number - 1)
        for (it in 1..config.nextCount.toInt().dec()) {
            val index = (nextWaypoint.number - 1 + it) % orderedWaypointsList.size
            if (index !in renderWaypoints) renderWaypoints.add(index)
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
        }.forEach { waypoint ->
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

    private fun getWaypointFormats(): List<String> {
        return ServiceLoader.load(WaypointFormat::class.java).map { it.name }
    }
}
