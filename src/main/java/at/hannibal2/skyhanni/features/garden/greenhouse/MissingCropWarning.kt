package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.BlockUtils.getTargetedBlock
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.container.table.TableRenderable.Companion.table
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import net.minecraft.client.Minecraft

@SkyHanniModule
object MissingCropWarning {

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val cropStorage = GreenhouseCropStorage()

    private var previousScan: Set<CropCategory>? = null
    private var stableScanCount = 0
    private val detectedCropsByVisitedPlot = mutableMapOf<Int, Set<CropCategory>>()
    private var checklistDisplay: Renderable? = null
    private var checklistVisible = true
    private var pendingDiagnostic: Pair<CropCategory, LorenzVec>? = null
    private var lastTargetedCrop: Pair<CropCategory, LorenzVec>? = null
    private var lastTargetedPosition: LorenzVec? = null
    private var missingCropWaypointsPlotId: Int? = null
    private var missingCropWaypoints: Map<CropCategory, LorenzVec> = emptyMap()
    private var lastProcessedPlotId: Int? = null
    private var checkerRunCount = 0
    private val replacementPrompts = mutableMapOf<CropCategory, CropReplacement>()

    @HandleEvent(ConfigLoadEvent::class)
    private fun onConfigLoad() {
        // Diagnostics can be used before the tab-list profile name has initialized. Flush anything
        // collected in that window as soon as profile-specific storage becomes available.
        cropStorage.syncRuntimeData()
    }

    @HandleEvent(WorldChangeEvent::class)
    private fun onWorldChange() {
        clearChecklistSession()
        reset()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!config.missingCropWarning || !config.useDiagnosticCropPositionFinder || !isInGreenhouse()) return
        if (Minecraft.getInstance().screen != null) return
        val position = getTargetedBlock() ?: return
        lastTargetedPosition = position
        CropCategory.fromBlock(position.getBlockStateAt().block)?.let {
            lastTargetedCrop = it to position
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onBlockClick(event: BlockClickEvent) {
        if (!config.missingCropWarning || !config.useDiagnosticCropPositionFinder || !isInGreenhouse()) return
        if (event.clickType != InteractClickType.RIGHT_CLICK) return
        lastTargetedPosition = event.position
        val category = CropCategory.fromBlock(event.blockState.block) ?: return
        pendingDiagnostic = category to event.position
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Diagnostics") return
        if (!config.useDiagnosticCropPositionFinder) {
            clearTargetedCrop()
            return
        }
        val plotId = GardenPlotApi.getCurrentPlot()?.id ?: return
        val targeted = pendingDiagnostic ?: lastTargetedCrop
        if (
            event.inventoryItems.values.any { item ->
                item.getLore().any { it.removeColor().trim() == MUTATION_CROP_LORE }
            }
        ) {
            val mutationCategory = targeted?.first
                ?: lastTargetedPosition?.getBlockStateAt()?.block?.let(CropCategory::fromBlock)
            if (mutationCategory != null) {
                ChatUtils.chat(
                    "§eIgnored decorative ${mutationCategory.displayName} blocks in this mutation. " +
                        "§7Detected mutation footprints are excluded from the 10x10 crop scan.",
                )
            }
            clearTargetedCrop()
            return
        }
        val category = event.inventoryItems.values.firstNotNullOfOrNull {
            CropCategory.fromDisplayName(it.hoverName.string.removeColor())
        } ?: targeted?.first
        val targetPosition = targeted?.second ?: lastTargetedPosition ?: getTargetedBlock()
        if (category == null || targetPosition == null) {
            ChatUtils.chat(
                "§cCould not save diagnosed crop. §7Menu crop: §e${category?.displayName ?: "unknown"}§7, " +
                    "target position: §e${targetPosition ?: "unknown"}§7.",
            )
            return
        }

        val nearbyPosition = findNearbyCropPosition(category, targetPosition)
        val targetedCategory = CropCategory.fromBlock(targetPosition.getBlockStateAt().block)
        if (nearbyPosition == null && targetedCategory != null && targetedCategory != category) {
            cropStorage.removeDiagnosedPosition(plotId, category.name)
            clearTargetedCrop()
            ChatUtils.chat(
                "§cCould not save ${category.displayName}: its position resolved to " +
                    "§e${targetedCategory.displayName}§c instead. The stale position was cleared.",
            )
            return
        }

        val position = nearbyPosition ?: if (targetedCategory == null) targetPosition.add(y = 1) else targetPosition
        clearTargetedCrop()
        val persisted = cropStorage.saveDiagnosedPosition(plotId, category, position)
        replacementPrompts.remove(category)
        ChatUtils.chat(
            if (persisted) {
                "§aSaved diagnosed ${category.displayName} in Greenhouse plot §e$plotId§a at " +
                    "§e${position.x.toInt()}, ${position.y.toInt()}, ${position.z.toInt()}§a."
            } else {
                "§eRemembered diagnosed ${category.displayName} for this session, but your SkyBlock profile " +
                    "has not loaded yet. §7It will be saved automatically when profile data becomes available."
            },
        )
    }

    private fun clearTargetedCrop() {
        pendingDiagnostic = null
        lastTargetedCrop = null
        lastTargetedPosition = null
    }

    private fun findNearbyCropPosition(category: CropCategory, center: LorenzVec): LorenzVec? =
        GreenhouseCropScanner.findNearbyCropPosition(category, center)

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
        val present = scannedPositions.keys - missingDiagnosedCropsOnPlot(plot.id)
        val spotted = rawScannedPositions.keys - missingDiagnosedCropsOnPlot(plot.id)

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
        clearTargetedCrop()
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
        checklistDisplay = Renderable.drawInsideDarkRect(Renderable.vertical(content, spacing = 1), padding = 4)
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

    private fun diagnosedPresentCrops(currentPlotId: Int?): Set<CropCategory> =
        cropStorage.diagnosedPositionsByPlot().flatMap { (plotId, positions) ->
            positions.map { (name, position) -> Triple(plotId, name, position) }
        }.mapNotNullTo(mutableSetOf()) { (plotId, name, position) ->
            val category = CropCategory.fromStorageName(name) ?: return@mapNotNullTo null
            category.takeIf {
                plotId != currentPlotId ||
                    !position.isInLoadedChunk() ||
                    !GreenhouseCropScanner.isMissingCrop(position, category)
            }
        }

    private fun missingDiagnosedCropsOnPlot(plotId: Int): Set<CropCategory> =
        missingDiagnosedCropPositionsOnPlot(plotId).keys

    private fun missingDiagnosedCropPositionsOnPlot(plotId: Int): Map<CropCategory, LorenzVec> =
        cropStorage.diagnosedPositionsByPlot()[plotId].orEmpty().entries.mapNotNull { (name, position) ->
            val category = CropCategory.fromStorageName(name) ?: return@mapNotNull null
            (category to position).takeIf {
                position.isInLoadedChunk() && GreenhouseCropScanner.isMissingCrop(position, category)
            }
        }.toMap()

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
        if (!config.missingCropWarning || !checklistVisible) return
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
        event.registerBrigadier("shuniquedetect") {
            description = "Toggles saving unique crop positions from Crop Diagnostics"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { toggleDiagnosticCropPositionFinder() }
        }
        event.registerBrigadier("shclearuniquediagnostics") {
            description = "Clears all saved unique crop diagnostic positions"
            category = CommandCategory.USERS_ACTIVE
            simpleCallback { clearDiagnosticCropPositions() }
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

    private fun clearDiagnosticCropPositions() {
        val clearedPositions = cropStorage.clearDiagnostics()
        clearTargetedCrop()
        missingCropWaypoints = emptyMap()
        ChatUtils.chat(
            if (clearedPositions == 0) "§eNo saved Crop Diagnostics positions were found."
            else "§aCleared saved Crop Diagnostics positions for §e$clearedPositions §acrops.",
        )
    }

    private fun toggleDiagnosticCropPositionFinder() {
        config.useDiagnosticCropPositionFinder = !config.useDiagnosticCropPositionFinder
        clearTargetedCrop()
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "toggle-unique-crop-detection")
        val state = if (config.useDiagnosticCropPositionFinder) "enabled" else "disabled"
        ChatUtils.chat(
            "§eSaving Crop Diagnostics positions is §${if (config.useDiagnosticCropPositionFinder) "a" else "c"}$state§e. " +
                "Previously saved crops are still used.",
        )
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
        val diagnosedPositions = cropStorage.diagnosedPositionsByPlot()[plot?.id].orEmpty()
        ChatUtils.chat(
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
                val allPresent = savedCrops + diagnosedPresentCrops(plot?.id)
                appendLine(" §7Missing: §e${(CropCategory.entries.toSet() - allPresent).namesOrNone()}")
                append(" §7Diagnosed positions: §e")
                append(
                    if (diagnosedPositions.isEmpty()) "none"
                    else diagnosedPositions.entries.joinToString("§7, §e") {
                        val category = CropCategory.fromStorageName(it.key)
                        val block = it.value.getBlockStateAt().block
                        "${category?.displayName ?: it.key}=${it.value} ($block)"
                    },
                )
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
        val diagnosedPositions = cropStorage.diagnosedPositionsByPlot()
        val diagnosedCategories = diagnosedPositions.values
            .flatMapTo(mutableSetOf()) { it.keys }
        scannedPositions.forEach { (category, newPosition) ->
            // A Crop Diagnostics result is an explicit player choice. Ignore every automatic sighting
            // of that crop, regardless of which stale detected entry happens to be visited first.
            if (category.name in diagnosedCategories) return@forEach
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
    private const val MUTATION_CROP_LORE = "Mutation Crop"
}
