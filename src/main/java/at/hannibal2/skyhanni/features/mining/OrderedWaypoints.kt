package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierUtils
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.model.waypoints.AbstractWaypoint
import at.hannibal2.skyhanni.data.model.waypoints.SequencedWaypointSet
import at.hannibal2.skyhanni.data.model.waypoints.SkyhanniWaypoint
import at.hannibal2.skyhanni.data.model.waypoints.AbstractWaypointFormat
import at.hannibal2.skyhanni.data.model.waypoints.WaypointSet
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LocationUtils.distanceSqToPlayer
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import java.util.Locale
import java.util.ServiceLoader

@SkyHanniModule
object OrderedWaypoints {
    private val config get() = SkyHanniMod.feature.mining.orderedWaypoints
    private val profileStorage get() = ProfileStorageData.orderedWaypointsRoutes
    private val modStorage get() = SkyHanniMod.orderedWaypointsRoutesData
    private val services by lazy { ServiceLoader.load(AbstractWaypointFormat::class.java) }

    private var waypointData = SequencedWaypointSet<SkyhanniWaypoint>()
    private var currentWaypointIndex
        get() = waypointData.currentIndex
        set(value) {
            waypointData.currentIndex = value
        }
    private var lastClosestWpIndex = 0

    private val waypointCount: Int get() = waypointData.size
    private val waypoints: List<SkyhanniWaypoint> get() = waypointData.waypoints
    private val orderedWaypoints: Map<Int, SkyhanniWaypoint> get() = waypointData.orderedWaypoints
    private val renderWaypointIndices: MutableList<Int> = mutableListOf()

    fun saveConfig() {
        SkyHanniMod.configManager.saveConfig(ConfigFileType.ROUTES, "Save file")
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return

        for ((waypointNumber, waypoint) in orderedWaypoints) {
            val configNext = config.nextCount + 1
            // todo right now we're discarding the 'color' from the waypoint entirely - seems like we should change that
            val wpColor = if (!config.showAll) when (waypointNumber) {
                0 -> config.previousWaypointColor
                1 -> config.currentWaypointColor
                in 2..configNext -> config.nextWaypointColor
                else -> config.setupModeColor
            } else config.showAllWaypointColor

            with(waypoint) {
                if (config.fillBlock) event.drawFilledSelf(wpColor)
                else event.drawEdgesSelf(wpColor, config.blockOutlineThickness.toInt())

                val inHighlightRange = waypointNumber in 0..configNext
                if (config.setupMode || config.showAll || inHighlightRange) event.drawName()

                if (config.showDistance) event.drawDistanceTo()
            }
        }

        if (waypointCount <= 1) return decideWaypoints()

        val renderIndex = renderWaypointIndices.first().takeIf { renderWaypointIndices.size == 2 } ?: renderWaypointIndices[2]
        val renderWaypoint = orderedWaypoints[renderIndex] ?: return
        with(renderWaypoint) {
            val shouldDrawEyeLine = config.traceLine && !config.showAll && !config.setupMode
            if (shouldDrawEyeLine) event.drawLineToEye(config.traceLineColor, config.traceLineThickness.toInt())
        }

        with(waypoints[1]) {
            with(config) {
                if (setupMode && !showAll) event.draw3DLineToOther(
                    renderWaypoint,
                    setupModeLineColor,
                    setupModeLineThickness.toInt(),
                    sneakingDuringRoute,
                )
            }
        }

        decideWaypoints()
    }

    @HandleEvent
    fun onWorldChange() {
        currentWaypointIndex = 0
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shordered") {
            description = "Ordered WaypointSet commands."
            category = CommandCategory.USERS_ACTIVE
            aliases = listOf("sho")
            literal("load", "import") {
                description = "Loads ordered waypoints from your clipboard or config."
                arg(
                    "name", BrigadierArguments.string(), BrigadierUtils.dynamicSuggestionProvider { getRouteNames() },
                ) { name ->
                    callback { loadWaypointSet(getArg(name)) }
                }
                simpleCallback { loadWaypointSet("") }
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
                    callback { skipTo(getArg(number)) }
                }
                simpleCallback { skipTo(1) }
            }
            literal("unskip") {
                description = "Goes back by the number inputted many waypoints."
                arg("amount", BrigadierArguments.integer()) { amount ->
                    callback { unSkip(getArg(amount)) }
                }
                simpleCallback { unSkip(1) }
            }
            literal("delete", "remove") {
                description = "Deletes the waypoint with the inputted number."
                arg("number", BrigadierArguments.integer()) { number ->
                    callback { deleteWaypoint(getArg(number)) }
                }
            }
            literal("add", "insert") {
                description = "Inserts a waypoint with the specified numbering below the player."
                arg("number", BrigadierArguments.integer()) { number ->
                    callback { addWaypoint(getArg(number)) }
                }
            }
            literal("export") {
                description = "Exports the loaded ordered waypoints to clipboard."
                arg(
                    "format",
                    BrigadierArguments.string(),
                    BrigadierUtils.dynamicSuggestionProvider { getAvailableWaypointFormats() },
                ) { format ->
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
                simpleCallback(::toggleSetupMode)
            }
        }
    }

    private fun getRouteNames() = profileStorage?.routes?.keys.orEmpty()

    private fun loadWaypointSet(setName: String) {
        val routes = profileStorage?.routes ?: ErrorManager.skyHanniError("profileStorage not initialized for OrderedWaypoints")

        SkyHanniMod.launchIOCoroutine {
            waypointData = when (setName) {
                "" -> loadWaypoints(ClipboardUtils.readFromClipboard().orEmpty())
                else -> routes[setName] ?: return@launchIOCoroutine ChatUtils.userError(
                    "Route $setName doesn't exist.\n" +
                        "§cSaved Routes: ${routes.keys.toList().joinToString(", ")}\n" +
                        "§cIf you would like to import a route from your clipboard, leave the route name blank.",
                )
            } ?: return@launchIOCoroutine ChatUtils.userError(
                "There was an error parsing waypoints. " +
                    "Please make sure they are properly formatted and in a supported format.\n" +
                    "§cSupported Formats: ${getAvailableWaypointFormats().joinToString(", ")}",
            )

            currentWaypointIndex = waypointData.minBy { waypoint -> waypoint.location.distanceSqToPlayer() }.number - 1
            renderWaypointIndices.clear()
            ChatUtils.chat("Loaded ordered waypoints!")
        }
    }

    private fun unload() {
        waypointData.clear()
        renderWaypointIndices.clear()
        currentWaypointIndex = 0
        lastClosestWpIndex = 0
        ChatUtils.chat("Unloaded ordered waypoints.")
    }

    private fun skip(amount: Int) {
        if (waypointData.isEmpty()) return ChatUtils.userError("There are no waypoints to skip.")
        waypointData.incrementIndex(amount)
        ChatUtils.chat("Skipped $amount ${StringUtils.pluralize(amount, "waypoint")}.")
    }

    private fun skipTo(waypointNumber: Int) {
        if (waypointData.isEmpty()) return ChatUtils.chat("There are no waypoints to skip to.")

        val newOrderedWaypointIndex = waypointNumber - 1
        if (0 <= newOrderedWaypointIndex && newOrderedWaypointIndex < waypointData.size) {
            currentWaypointIndex = newOrderedWaypointIndex
            ChatUtils.chat("Skipped to ${currentWaypointIndex + 1}.")
        } else {
            ChatUtils.userError("$waypointNumber is not between 1 and ${waypointData.size}.")
        }
    }

    private fun unSkip(amount: Int) {
        if (waypointData.isEmpty()) return ChatUtils.userError("There are no waypoints to unskip.")
        waypointData.incrementIndex(-amount)
        ChatUtils.chat("Unskipped $amount waypoints.")
    }

    private fun deleteWaypoint(waypointNumberSuggestion: Int) {
        val waypointNumber = waypointNumberSuggestion.takeIf { it in 2..waypointCount }
            ?: return ChatUtils.userError("$waypointNumberSuggestion is not between 1 and $waypointCount.")
        val waypointData = waypointData.takeIfNotEmpty() ?: return ChatUtils.userError("There are no waypoints to delete.")

        waypointData.removeAt(waypointNumber - 1)
        renderWaypointIndices.clear()

        ChatUtils.chat("Removed waypoint $waypointNumber.")
    }

    private fun addWaypoint(waypointNumberSuggestion: Int) {
        val pos = LocationUtils.playerLocation().add(0, -1, 0).roundToBlock()
        val waypointNumber = waypointNumberSuggestion.takeIf { it in 1..waypointCount + 1 }
            ?: return ChatUtils.userError("$waypointNumberSuggestion is not between 1 and ${waypointData.size + 1}.")

        waypointData.addNumbered(SkyhanniWaypoint(pos, number = waypointNumber))
    }

    private fun export(format: String) = SkyHanniMod.launchIOCoroutine {
        val formattedRoute = when (format) {
            // todo our class should probably be most descriptive, encapsulate every other format
            "" -> exportWaypointSet(waypointData, "coleweight")
            else -> exportWaypointSet(waypointData, format.lowercase(Locale.getDefault()))
        } ?: return@launchIOCoroutine ChatUtils.userError(
            "Invalid waypoint format specified.\n" +
                "§cFormats: ${getAvailableWaypointFormats().joinToString { ", " }}",
        )

        ClipboardUtils.copyToClipboard(formattedRoute)
        ChatUtils.chat("Route was copied to clipboard.")
    }

    private fun save(name: String) {
        ProfileStorageData.orderedWaypointsRoutes?.routes?.set(name, waypointData.deepCopy())
        saveConfig()
        ChatUtils.chat("Route saved as $name. Do /sho load $name to import it.")
    }

    private fun erase(name: String) {
        ProfileStorageData.orderedWaypointsRoutes?.routes?.remove(name) ?: return ChatUtils.userError("Route $name doesn't exist.")
        saveConfig()
        ChatUtils.chat("Route $name successfully deleted.")
    }

    private fun toggleSetupMode(forceVal: Boolean? = null) {
        config.setupMode = forceVal ?: !config.setupMode
        ChatUtils.chat("Toggled setup mode to ${config.setupMode}")
    }

    private fun decideWaypoints() {
        renderWaypointIndices.clear()
        if (waypointData.isEmpty()) return
        val currentIndex = currentWaypointIndex
        renderWaypointIndices.addAll(getNewRenderWaypoints(currentIndex))
    }

    private fun getNewRenderWaypoints(currentIndex: Int) = buildList {
        val beforeWaypoint = waypointData.getOrNull(currentIndex - 1) ?: waypointData.last()
        add(beforeWaypoint.number - 1)

        val currentWaypoint = waypointData.getOrNull(currentIndex)
        val distanceToCurrent = currentWaypoint?.location?.distanceToPlayer()?.let {
            add(currentWaypoint.number - 1)
            it
        } ?: Double.POSITIVE_INFINITY

        val nextWaypoint = waypointData.getOrNull(this@OrderedWaypoints.currentWaypointIndex + 1) ?: waypointData.first()

        val distanceToNext = nextWaypoint.location.distanceToPlayer()
        if (nextWaypoint.number - 1 !in this) add(nextWaypoint.number - 1)

        for (it in 1..<config.nextCount) {
            val index = (nextWaypoint.number - 1 + it) % waypointData.size
            if (index !in this) add(index)
        }

        val lastIsCurrent = lastClosestWpIndex == currentIndex
        val currentInRange = distanceToCurrent < config.waypointRange.toDouble()
        val nextInRange = distanceToNext < config.waypointRange.toDouble()
        val rangeChanged = distanceToCurrent > distanceToNext && nextInRange

        if (lastIsCurrent && rangeChanged) return@buildList waypointData.incrementIndex(1)
        if (currentInRange) lastClosestWpIndex = currentIndex
        if (nextInRange) waypointData.incrementIndex(1)

        waypointData.filter {
            val inRange = it.location.distanceToPlayer() < config.setupModeRange
            config.setupMode && (inRange || config.showAll)
        }.forEach { add(it.number - 1) }
    }.toSet()

    private fun loadWaypoints(data: String): SequencedWaypointSet<SkyhanniWaypoint>? = services.firstNotNullOfOrNull { format ->
        val deserialized = format.deserialize(data) ?: return@firstNotNullOfOrNull null
        val shConverted = deserialized.map { it.toSkyHanniFormat() }.toMutableList()
        return@firstNotNullOfOrNull SequencedWaypointSet(shConverted)
    }

    private fun <N : AbstractWaypoint<N>> exportWaypointSet(
        waypoints: WaypointSet<N>,
        name: String,
    ): String? = services.firstOrNull { it.name == name }?.let {
        @Suppress("UNCHECKED_CAST")
        val fmt = it as? AbstractWaypointFormat<N> ?: return null
        fmt.serialize(waypoints)
    }

    private fun getAvailableWaypointFormats(): List<String> = services.map { it.name }
}
