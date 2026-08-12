package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object MissingCropWarning {

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val cropStorage = GreenhouseCropStorage()

    private var previousScan: Set<CropCategory>? = null
    private var stableScanCount = 0
    private val detectedCropsByVisitedPlot = mutableMapOf<Int, Set<CropCategory>>()
    private var checklistDisplay: Renderable? = null
    private var checklistVisible = true
    private var missingCropWaypointsPlotId: Int? = null
    private var missingCropWaypoints: Map<CropCategory, LorenzVec> = emptyMap()
    private var lastProcessedPlotId: Int? = null
    private var checkerRunCount = 0
    private val replacementPrompts = mutableMapOf<CropCategory, CropReplacement>()

    @HandleEvent(ConfigLoadEvent::class)
    private fun onConfigLoad() {
        cropStorage.syncRuntimeData()
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        clearChecklistSession()
        reset()
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.GARDEN)
    private fun onSecondPassed() {
        if (!config.missingCropWarning || !isInGreenhouse()) {
            reset()
            return
        }
        checkerRunCount++

        cropStorage.syncRuntimeData()

        val plot = GardenPlotApi.getCurrentPlot() ?: return
        preparePlotScan(plot.id)
        missingCropWaypointsPlotId = plot.id
        missingCropWaypoints = emptyMap()
        if (!GreenhouseGridScanner.isLoaded(plot)) return

        val outOfGridDetectedCrops = cropStorage.detectedCropPositionsByPlot()[plot.id].orEmpty()
            .filterValues { !GreenhouseGridScanner.isInsideGrid(plot, it) }
            .keys
        cropStorage.removeDetectedCropPositions(plot.id, outOfGridDetectedCrops)
        val allRememberedPositions = cropStorage.rememberedCropPositions()
        val rawScannedPositions = GreenhouseCropScanner.scanGreenhousePositions(plot)
        promptForCropReplacements(plot.id, rawScannedPositions, allRememberedPositions)

        val scannedPositions = rawScannedPositions.filterKeys { category ->
            allRememberedPositions.none { (rememberedPlotId, positions) ->
                rememberedPlotId != plot.id && category in positions
            }
        }
        val present = scannedPositions.keys
        val spotted = rawScannedPositions.keys

        cropStorage.rememberDetectedCrops(plot.id, scannedPositions)
        if (spotted != previousScan) {
            previousScan = spotted
            stableScanCount = 1
            return
        }
        stableScanCount++
        if (stableScanCount < REQUIRED_STABLE_SCANS) return

        val presentNames = present.mapTo(mutableSetOf()) { it.name }
        cropStorage.updateDetectedCrops(plot.id, presentNames, scannedPositions)
        recordStableScan(plot.id, spotted)

        // Only validate positions belonging to the plot the player is currently inside. Positions from a
        // neighbouring Greenhouse may also be in loaded chunks, but loading must not make them current.
        val rememberedPositions = cropStorage.rememberedCropPositions()
        val missingPositions = rememberedPositions[plot.id].orEmpty().filter { (category, position) ->
            position.isInLoadedChunk() && GreenhouseCropScanner.isMissingCrop(position, category)
        }
        missingCropWaypoints = missingPositions
    }

    private fun preparePlotScan(plotId: Int) {
        if (lastProcessedPlotId == plotId) return
        previousScan = null
        stableScanCount = 0
        lastProcessedPlotId = plotId
    }

    private fun reset() {
        previousScan = null
        stableScanCount = 0
        missingCropWaypointsPlotId = null
        missingCropWaypoints = emptyMap()
        lastProcessedPlotId = null
    }

    private fun recordStableScan(plotId: Int, spotted: Set<CropCategory>) {
        detectedCropsByVisitedPlot[plotId] = spotted
        updateChecklistDisplay()
    }

    private fun updateChecklistDisplay() {
        if (detectedCropsByVisitedPlot.isEmpty()) {
            checklistDisplay = null
            return
        }
        val plotIds = checklistPlotIds()
        val table = buildList {
            add(
                buildList {
                    add(Renderable.text("§7Crop"))
                    plotIds.forEach { plotId ->
                        add(Renderable.text(plotId?.let { "§ePlot $it" } ?: "§8Unvisited"))
                    }
                },
            )
            CropCategory.entries.forEach { category ->
                add(
                    buildList {
                        add(Renderable.text("§f${category.displayName}"))
                        plotIds.forEach { plotId ->
                            val spotted = plotId?.let(detectedCropsByVisitedPlot::get)
                            val marker = when {
                                spotted == null -> "§8—"
                                category in spotted -> "§a✔"
                                else -> "§c✘"
                            }
                            add(Renderable.text(marker))
                        }
                    },
                )
            }
        }
        val content = buildList {
            add(Renderable.text("§6§lGreenhouse Crop Checklist"))
            add(
                Renderable.text(
                    "§7Visited: §e${detectedCropsByVisitedPlot.size}/$EXPECTED_GREENHOUSE_COUNT",
                ),
            )
            add(Renderable.table(table, xSpacing = 7, ySpacing = 1))
            add(Renderable.text(combinedMissingLine()))
        }
        checklistDisplay = Renderable.vertical(content, spacing = 1)
    }

    private fun checklistPlotIds(): List<Int?> {
        val knownIds = GardenPlotApi.plots.asSequence()
            .filter { it.greenhouse }
            .map { it.id }
            .plus(detectedCropsByVisitedPlot.keys.asSequence())
            .distinct()
            .sorted()
            .toMutableList<Int?>()
        while (knownIds.size < EXPECTED_GREENHOUSE_COUNT) knownIds.add(null)
        return knownIds
    }

    private fun combinedMissingLine(): String {
        if (detectedCropsByVisitedPlot.size < EXPECTED_GREENHOUSE_COUNT) {
            return "§7Visit every Greenhouse to calculate missing crops."
        }
        val spotted = detectedCropsByVisitedPlot.values.flatten().toSet()
        val missing = CropCategory.entries.toSet() - spotted
        return if (missing.isEmpty()) {
            "§aAll unique crops spotted across the Greenhouses."
        } else {
            "§cMissing: §e" + missing.joinToString("§7, §e") { it.displayName }
        }
    }

    private fun clearChecklistSession() {
        detectedCropsByVisitedPlot.clear()
        checklistDisplay = null
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.missingCropWarning || !isInGreenhouse()) return
        if (GardenPlotApi.getCurrentPlot()?.id != missingCropWaypointsPlotId) return
        for ((category, position) in missingCropWaypoints) {
            event.drawWaypointFilled(
                position,
                LorenzColor.RED.toColor(),
                seeThroughBlocks = true,
                beacon = true,
            )
            event.drawDynamicText(position.add(y = 1), "§cMissing ${category.displayName}", 1.5)
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class, onlyOnIsland = IslandType.GARDEN)
    private fun onRenderOverlay() {
        if (!config.missingCropWarning || !checklistVisible || !GardenPlotApi.inGreenhouse()) return
        val display = checklistDisplay ?: return
        config.cropChecklistPosition.renderRenderable(display, posLabel = "Greenhouse Crop Checklist")
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shgreenhousecropdebug") {
            description = "Shows the current state of the Greenhouse missing crop checker"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { showDebugState() }
        }
        event.registerBrigadier("shgreenhouseplots") {
            description = "Lists the plots SkyHanni knows are Greenhouses"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { showKnownGreenhousePlots() }
        }
        event.registerBrigadier("shgreenhousechecklist") {
            description = "Toggles the Greenhouse crop checklist display"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { toggleChecklistDisplay() }
        }
        event.registerBrigadier("shresetgreenhousecropdata") {
            description = "Clears all remembered Greenhouse crop locations"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { resetGreenhouseCropData() }
        }
    }

    private fun resetGreenhouseCropData() {
        cropStorage.clearAll()
        replacementPrompts.clear()
        clearChecklistSession()
        reset()
        ChatUtils.chat("§aCleared all remembered Greenhouse crop locations.")
    }

    private fun toggleChecklistDisplay() {
        checklistVisible = !checklistVisible
        ChatUtils.chat("§eGreenhouse crop checklist ${if (checklistVisible) "§ashown" else "§chidden"}§e.")
    }

    private fun showKnownGreenhousePlots() {
        val greenhousePlots = GardenPlotApi.plots.filter { it.greenhouse }
        if (greenhousePlots.isEmpty()) {
            ChatUtils.chat("§cSkyHanni currently knows no Greenhouse plots.")
            return
        }
        ChatUtils.chat(
            buildString {
                appendLine("§6Known Greenhouse Plots")
                greenhousePlots.sortedBy { it.id }.forEach { plot ->
                    val loaded = GreenhouseCropScanner.isCompleteScanAreaLoaded(plot)
                    appendLine(
                        " §7Plot §e${plot.id}§7: " +
                            if (loaded) "§afully loaded" else "§cnot fully loaded",
                    )
                }
                append(" §7Current plot: §e${GardenPlotApi.getCurrentPlot()?.id ?: "none"}")
            },
        )
    }

    private fun showDebugState() {
        val plot = GardenPlotApi.getCurrentPlot()
        val liveCrops = plot?.let(GreenhouseCropScanner::scanGreenhouse).orEmpty()
        val savedByPlot = cropStorage.detectedCropsByPlot()
        val savedCrops = savedByPlot.values.flatten().mapNotNullTo(mutableSetOf(), CropCategory::fromStorageName)
        ChatUtils.debug(
            buildString {
                appendLine("§6Greenhouse Crop Checker Debug")
                appendLine(" §7Enabled: §e${config.missingCropWarning}")
                appendLine(" §7Scoreboard area: §e${SkyBlockUtils.scoreboardArea}")
                appendLine(" §7Scoreboard says Greenhouse: §e${scoreboardShowsGreenhouse()}")
                appendLine(" §7Plot marked as Greenhouse: §e${GardenPlotApi.inGreenhouse()}")
                appendLine(" §7Current plot: §e${plot?.id ?: "none"}")
                appendLine(" §7Checker runs this session: §e$checkerRunCount")
                appendLine(" §7Live scan: §e${liveCrops.namesOrNone()}")
                appendLine(" §7Saved crops: §e${savedCrops.namesOrNone()}")
                append(" §7Missing: §e${(CropCategory.entries.toSet() - savedCrops).namesOrNone()}")
            },
        )
    }

    private fun Collection<CropCategory>.namesOrNone(): String =
        ifEmpty { return "none" }.joinToString(", ") { it.displayName }

    private fun promptForCropReplacements(
        newPlotId: Int,
        scannedPositions: Map<CropCategory, LorenzVec>,
        rememberedPositions: Map<Int, Map<CropCategory, LorenzVec>>,
    ) {
        scannedPositions.forEach { (category, newPosition) ->
            val oldPlotId = rememberedPositions.entries.firstOrNull { (plotId, positions) ->
                plotId != newPlotId && category in positions
            }?.key ?: return@forEach
            val replacement = CropReplacement(category, oldPlotId, newPlotId, newPosition)
            if (category.name in cropStorage.ignoredCropReplacementsByPlot()[newPlotId].orEmpty()) return@forEach
            replacementPrompts[category]?.let { existing ->
                if (existing.oldPlotId == oldPlotId && existing.newPlotId == newPlotId) {
                    // Keep the actionable position fresh without repeating the same prompt.
                    replacementPrompts[category] = replacement
                    return@forEach
                }
            }
            replacementPrompts[category] = replacement

            ChatUtils.chat(
                "§e${category.displayName} was remembered in Greenhouse Plot §6$oldPlotId§e, " +
                    "but was also found in Plot §6$newPlotId§e. Replace its remembered location?",
            )
            ChatUtils.clickableChat(
                "§a[Replace with Plot $newPlotId]",
                onClick = {
                    replacementPrompts[category]
                        ?.takeIf { it.oldPlotId == oldPlotId && it.newPlotId == newPlotId }
                        ?.let(::acceptCropReplacement)
                        ?: ChatUtils.chat("§cThat Greenhouse crop replacement is no longer pending.")
                },
                hover = "§eMove the remembered ${category.displayName} location to Plot $newPlotId.",
            )
            ChatUtils.clickableChat(
                "§c[Keep Plot $oldPlotId]",
                onClick = {
                    val pending = replacementPrompts[category]
                    if (pending?.oldPlotId == oldPlotId && pending.newPlotId == newPlotId) {
                        replacementPrompts.remove(category)
                        cropStorage.rememberIgnoredCropReplacement(newPlotId, category)
                        ChatUtils.chat("§eKept ${category.displayName} in Greenhouse Plot §6$oldPlotId§e.")
                    }
                },
                hover = "§eKeep the existing location and ignore this candidate.",
            )
        }
    }

    private fun acceptCropReplacement(replacement: CropReplacement) {
        if (replacementPrompts[replacement.category] != replacement) {
            ChatUtils.chat("§cThat Greenhouse crop replacement is no longer pending.")
            return
        }
        if (
            !replacement.position.isInLoadedChunk() ||
            GreenhouseCropScanner.isMissingCrop(replacement.position, replacement.category)
        ) {
            replacementPrompts.remove(replacement.category)
            ChatUtils.chat("§cThe replacement ${replacement.category.displayName} is no longer present.")
            return
        }

        cropStorage.moveCrop(replacement.category, replacement.newPlotId, replacement.position)
        replacementPrompts.remove(replacement.category)
        ChatUtils.chat(
            "§aMoved ${replacement.category.displayName} from Greenhouse Plot §e${replacement.oldPlotId} " +
                "§ato Plot §e${replacement.newPlotId}§a.",
        )
    }

    private fun scoreboardShowsGreenhouse(): Boolean = GreenhouseUtils.scoreboardShowsGreenhouse()

    private fun isInGreenhouse(): Boolean = GreenhouseUtils.isInGreenhouse()

    private data class CropReplacement(
        val category: CropCategory,
        val oldPlotId: Int,
        val newPlotId: Int,
        val position: LorenzVec,
    )

    private const val REQUIRED_STABLE_SCANS = 2
    private const val EXPECTED_GREENHOUSE_COUNT = 3
}
