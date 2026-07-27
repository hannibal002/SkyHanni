package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigFileType
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.BlockUtils.getTargetedBlock
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesInBoundingBox
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesInBox
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getHandItem
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getStandHelmet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.itemType
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

@SkyHanniModule
object MissingCropWarning {

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val storage get() = ProfileStorageData.profileSpecific?.garden?.greenhouse
    private val fallbackStorage get() = ProfileStorageData.playerSpecific?.greenhouseDiagnosedCropPositionsByPlot
    private val fallbackDetectedStorage get() = ProfileStorageData.playerSpecific?.greenhouseDetectedCropPositionsByPlot

    private var previousScan: Set<CropCategory>? = null
    private var stableScanCount = 0
    private var lastReportedMissing: Set<CropCategory>? = null
    private var pendingDiagnostic: Pair<CropCategory, LorenzVec>? = null
    private var lastTargetedCrop: Pair<CropCategory, LorenzVec>? = null
    private var lastTargetedPosition: LorenzVec? = null
    private val runtimeDetectedCropsByPlot = mutableMapOf<Int, MutableSet<String>>()
    private val runtimeDetectedCropPositionsByPlot = mutableMapOf<Int, MutableMap<String, LorenzVec>>()
    private val runtimeDiagnosedPositionsByPlot = mutableMapOf<Int, MutableMap<String, LorenzVec>>()
    private var pendingPersistentSave = false
    private var missingCropWaypointsPlotId: Int? = null
    private var missingCropWaypoints: Map<CropCategory, LorenzVec> = emptyMap()
    private val previousRememberedMissingByPlot = mutableMapOf<Int, Set<CropCategory>>()
    private val stableRememberedChecksByPlot = mutableMapOf<Int, Int>()
    private val lastReportedRememberedMissingByPlot = mutableMapOf<Int, Set<CropCategory>>()
    private val announcedCompletePlots = mutableSetOf<Int>()
    private var lastProcessedPlotId: Int? = null
    private var checkerRunCount = 0

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.missingCropWarning || !config.useDiagnosticCropPositionFinder || !isInGreenhouse()) return
        if (Minecraft.getInstance().screen != null) return
        val position = getTargetedBlock() ?: return
        lastTargetedPosition = position
        CropCategory.fromBlock(position.getBlockStateAt().block)?.let {
            lastTargetedCrop = it to position
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        if (!config.missingCropWarning || !config.useDiagnosticCropPositionFinder || !isInGreenhouse()) return
        if (event.clickType != InteractClickType.RIGHT_CLICK) return
        lastTargetedPosition = event.position
        val category = CropCategory.fromBlock(event.blockState.block) ?: return
        pendingDiagnostic = category to event.position
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Diagnostics") return
        if (!config.useDiagnosticCropPositionFinder) {
            clearTargetedCrop()
            return
        }
        val plotId = GardenPlotApi.getCurrentPlot()?.id ?: return
        val targeted = pendingDiagnostic ?: lastTargetedCrop
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
            runtimeDiagnosedPositionsByPlot[plotId]?.remove(category.name)
            storage?.diagnosedCropPositionsByPlot?.get(plotId)?.remove(category.name)
            fallbackStorage?.get(plotId)?.remove(category.name)
            pendingPersistentSave = true
            savePendingData()
            clearTargetedCrop()
            ChatUtils.chat(
                "§cCould not save ${category.displayName}: its position resolved to " +
                    "§e${targetedCategory.displayName}§c instead. The stale position was cleared.",
            )
            return
        }

        val position = nearbyPosition ?: if (targetedCategory == null) targetPosition.add(y = 1) else targetPosition
        clearTargetedCrop()
        removeRememberedCategoryFromOtherPlots(category.name, plotId, diagnosed = true)
        runtimeDiagnosedPositionsByPlot.getOrPut(plotId) { mutableMapOf() }[category.name] = position
        storage?.diagnosedCropPositionsByPlot?.getOrPut(plotId) { mutableMapOf() }?.set(category.name, position)
        fallbackStorage?.getOrPut(plotId) { mutableMapOf() }?.set(category.name, position)
        pendingPersistentSave = true
        savePendingData()
        ChatUtils.chat(
            "§aSaved diagnosed ${category.displayName} in Greenhouse plot §e$plotId§a at " +
                "§e${position.x.toInt()}, ${position.y.toInt()}, ${position.z.toInt()}§a.",
        )
    }

    private fun clearTargetedCrop() {
        pendingDiagnostic = null
        lastTargetedCrop = null
        lastTargetedPosition = null
    }

    private fun findNearbyCropPosition(category: CropCategory, center: LorenzVec): LorenzVec? {
        val world = MinecraftCompat.localWorldOrNull ?: return null
        val centerPos = center.toBlockPos()
        val from = centerPos.offset(-DIAGNOSTIC_SEARCH_RADIUS, -DIAGNOSTIC_SEARCH_RADIUS, -DIAGNOSTIC_SEARCH_RADIUS)
        val to = centerPos.offset(DIAGNOSTIC_SEARCH_RADIUS, DIAGNOSTIC_SEARCH_RADIUS, DIAGNOSTIC_SEARCH_RADIUS)
        val nearbyPositions = BlockPos.betweenClosed(from, to).toList()
        val matchingCrop = nearbyPositions
            .filter { CropCategory.fromBlock(world.getBlockState(it).block) == category }
            .minByOrNull { it.distSqr(centerPos) }
        if (matchingCrop != null) return matchingCrop.toLorenzVec()

        if (category !in diagnosticOnlyCrops) return null
        return nearbyPositions
            .filter { world.getBlockState(it).block in greenhouseStemBlocks }
            .minByOrNull { it.distSqr(centerPos) }
            ?.toLorenzVec()
    }

    @HandleEvent(SecondPassedEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed() {
        if (!config.missingCropWarning || !isInGreenhouse()) {
            reset()
            return
        }
        checkerRunCount++

        storage?.diagnosedCropPositionsByPlot?.let { saved ->
            runtimeDiagnosedPositionsByPlot.forEach { (plotId, positions) ->
                saved.getOrPut(plotId) { mutableMapOf() }.putAll(positions)
            }
        }
        fallbackStorage?.let { saved ->
            runtimeDiagnosedPositionsByPlot.forEach { (plotId, positions) ->
                saved.getOrPut(plotId) { mutableMapOf() }.putAll(positions)
            }
        }
        storage?.detectedCropPositionsByPlot?.let { saved ->
            runtimeDetectedCropPositionsByPlot.forEach { (plotId, positions) ->
                saved.getOrPut(plotId) { mutableMapOf() }.putAll(positions)
            }
        }
        fallbackDetectedStorage?.let { saved ->
            runtimeDetectedCropPositionsByPlot.forEach { (plotId, positions) ->
                saved.getOrPut(plotId) { mutableMapOf() }.putAll(positions)
            }
        }
        savePendingData()

        val plot = GardenPlotApi.getCurrentPlot() ?: return
        if (lastProcessedPlotId != plot.id) {
            announcedCompletePlots.remove(plot.id)
            previousRememberedMissingByPlot.remove(plot.id)
            stableRememberedChecksByPlot.remove(plot.id)
            lastReportedRememberedMissingByPlot.remove(plot.id)
            lastProcessedPlotId = plot.id
        }
        missingCropWaypointsPlotId = plot.id
        missingCropWaypoints = emptyMap()
        val scannedPositions = scanGreenhousePositions(plot)
        val present = scannedPositions.keys - missingDiagnosedCropsOnPlot(plot.id)

        // Positive sightings do not need stabilization. Floating display entities can fluctuate between
        // consecutive scans, which previously prevented a complete live scan from ever being remembered.
        if (scannedPositions.isNotEmpty()) {
            val seenNames = scannedPositions.keys.mapTo(mutableSetOf()) { it.name }
            runtimeDetectedCropsByPlot.getOrPut(plot.id) { mutableSetOf() }.addAll(seenNames)
            storage?.detectedCropsByPlot?.getOrPut(plot.id) { mutableSetOf() }?.addAll(seenNames)
            saveDetectedCropPositions(plot.id, scannedPositions)
            pendingPersistentSave = true
            savePendingData()
        }
        if (present.size >= CropCategory.entries.size) {
            missingCropWaypoints = emptyMap()
            lastReportedMissing = emptySet()
            if (announcedCompletePlots.add(plot.id)) {
                ChatUtils.chat("§aAll 12 unique Greenhouse crops are planted in Plot §e${plot.id}§a!")
            }
            return
        }

        val allRememberedPositions = rememberedCropPositions()
        val allRememberedCategories = allRememberedPositions.values.flatMapTo(mutableSetOf()) { it.keys }
        if (allRememberedCategories.size >= CropCategory.entries.size) {
            validateRememberedCropsInPlot(plot.id, allRememberedPositions[plot.id].orEmpty())
            return
        }

        if (present != previousScan) {
            previousScan = present
            stableScanCount = 1
            return
        }
        stableScanCount++
        if (stableScanCount < REQUIRED_STABLE_SCANS) return

        val presentNames = present.mapTo(mutableSetOf()) { it.name }
        if (runtimeDetectedCropsByPlot[plot.id] != presentNames) {
            runtimeDetectedCropsByPlot[plot.id] = presentNames
            pendingPersistentSave = true
        }
        storage?.detectedCropsByPlot?.apply {
            putAll(runtimeDetectedCropsByPlot)
            this[plot.id] = presentNames
        }
        saveDetectedCropPositions(plot.id, scannedPositions)
        savePendingData()

        // Only crops that have actually been seen are expected. Their remembered XYZ remains authoritative
        // when the player visits another Greenhouse plot that never contained a unique crop.
        val rememberedPositions = rememberedCropPositions()
        val missingByPlot = rememberedPositions.mapValues { (_, positions) ->
            positions.filter { (category, position) ->
                position.isInLoadedChunk() && position.isMissingCrop(category)
            }
        }
        val missing = missingByPlot.values.flatMapTo(mutableSetOf()) { it.keys }
        missingCropWaypoints = missingByPlot[plot.id].orEmpty()
        if (missing == lastReportedMissing) return
        lastReportedMissing = missing

        if (missing.isEmpty() && rememberedPositions.values.sumOf { it.size } >= CropCategory.entries.size) {
            ChatUtils.chat("§aAll 12 unique Greenhouse crops are planted!")
        } else if (missing.isNotEmpty()) {
            ChatUtils.chat(
                "§cMissing Greenhouse ${if (missing.size == 1) "crop" else "crops"}: §e" +
                    missing.joinToString("§7, §e") { it.displayName },
            )
        }
    }

    private fun scanGreenhouse(plot: GardenPlot): Set<CropCategory> =
        scanGreenhousePositions(plot).keys

    private fun scanGreenhousePositions(plot: GardenPlot): Map<CropCategory, LorenzVec> {
        val world = MinecraftCompat.localWorldOrNull ?: return emptyMap()
        val middle = plot.middle.toBlockPos()
        val from = BlockPos(middle.x - SCAN_RADIUS, MIN_GARDEN_Y, middle.z - SCAN_RADIUS)
        val to = BlockPos(middle.x + SCAN_RADIUS, MAX_GARDEN_Y, middle.z + SCAN_RADIUS)
        return buildMap {
            for (pos in BlockPos.betweenClosed(from, to)) {
                val block = world.getBlockState(pos).block
                CropCategory.fromBlock(block)?.let {
                    putIfAbsent(it, pos.toLorenzVec())
                }
                if (size == CropCategory.entries.size) return@buildMap
            }
            scanFloatingCropHeads(
                AABB(
                    from.x.toDouble(),
                    from.y.toDouble(),
                    from.z.toDouble(),
                    (to.x + 1).toDouble(),
                    (to.y + 1).toDouble(),
                    (to.z + 1).toDouble(),
                ),
            )
        }
    }

    private fun MutableMap<CropCategory, LorenzVec>.scanFloatingCropHeads(scanArea: AABB) {
        getEntitiesInBoundingBox<ArmorStand>(scanArea).forEach { stand ->
            listOf(stand.getStandHelmet(), stand.getHandItem())
                .firstNotNullOfOrNull { it.floatingCropHeadCategory() }
                ?.let { this[it] = stand.getLorenzVec() }
        }
        getEntitiesInBoundingBox<Display.ItemDisplay>(scanArea).forEach { display ->
            display.itemStack.floatingCropHeadCategory()?.let {
                this[it] = display.getLorenzVec()
            }
        }
    }

    private fun isCompleteScanAreaLoaded(plot: GardenPlot): Boolean {
        val middle = plot.middle
        return listOf(
            middle.add(x = -SCAN_RADIUS, z = -SCAN_RADIUS),
            middle.add(x = -SCAN_RADIUS, z = SCAN_RADIUS),
            middle.add(x = SCAN_RADIUS, z = -SCAN_RADIUS),
            middle.add(x = SCAN_RADIUS, z = SCAN_RADIUS),
        ).all { it.isInLoadedChunk() }
    }

    private fun reset() {
        previousScan = null
        stableScanCount = 0
        lastReportedMissing = null
        missingCropWaypointsPlotId = null
        missingCropWaypoints = emptyMap()
        previousRememberedMissingByPlot.clear()
        stableRememberedChecksByPlot.clear()
        lastReportedRememberedMissingByPlot.clear()
        announcedCompletePlots.clear()
        lastProcessedPlotId = null
        clearTargetedCrop()
    }

    private fun validateRememberedCropsInPlot(
        plotId: Int,
        rememberedPositions: Map<CropCategory, LorenzVec>,
    ) {
        // Once every unique has a known home, never sweep unrelated plots again. Only validate the
        // remembered XYZ entries belonging to the plot the player is currently standing in.
        if (rememberedPositions.isEmpty()) return
        val initiallyMissing = rememberedPositions.filter { (category, position) ->
            position.isInLoadedChunk() && position.isMissingCrop(category)
        }
        val diagnosedCategories = diagnosedPositionsByPlot()[plotId].orEmpty().keys
            .mapNotNullTo(mutableSetOf(), CropCategory::fromStorageName)
        val replacements = if (initiallyMissing.isEmpty()) emptyMap() else {
            GardenPlotApi.plots.firstOrNull { it.id == plotId }
                ?.let(::scanGreenhousePositions)
                .orEmpty()
                .filterKeys { it in initiallyMissing && it !in diagnosedCategories }
        }
        replacements.forEach { (category, position) ->
            saveDetectedCropPositions(plotId, mapOf(category to position))
        }
        if (replacements.isNotEmpty()) savePendingData()
        val missingPositions = initiallyMissing - replacements.keys
        val missing = missingPositions.keys
        if (previousRememberedMissingByPlot[plotId] != missing) {
            previousRememberedMissingByPlot[plotId] = missing
            stableRememberedChecksByPlot[plotId] = 1
            return
        }
        val stableChecks = stableRememberedChecksByPlot.getOrDefault(plotId, 0) + 1
        stableRememberedChecksByPlot[plotId] = stableChecks
        if (stableChecks < REQUIRED_STABLE_REMEMBERED_CHECKS) return

        missingCropWaypoints = missingPositions
        val previouslyReported = lastReportedRememberedMissingByPlot[plotId]
        if (previouslyReported == missing) return
        lastReportedRememberedMissingByPlot[plotId] = missing

        if (missing.isEmpty()) {
            if (rememberedPositions.size >= CropCategory.entries.size) {
                if (announcedCompletePlots.add(plotId)) {
                    ChatUtils.chat("§aAll 12 unique Greenhouse crops are planted in Plot §e$plotId§a!")
                }
            } else if (!previouslyReported.isNullOrEmpty()) {
                ChatUtils.chat("§aAll remembered unique crops in Greenhouse Plot §e$plotId §aare planted!")
            }
            return
        }
        announcedCompletePlots.remove(plotId)
        ChatUtils.chat(
            "§c${missingPositions.entries.joinToString("§7, §c") { (category, position) ->
                "${category.displayName} §7(${position.x.toInt()}, ${position.y.toInt()}, ${position.z.toInt()})§c"
            }} ${if (missing.size == 1) "is" else "are"} supposed to be at Greenhouse Plot §e$plotId§c.",
        )
    }

    private fun diagnosedPresentCrops(currentPlotId: Int?): Set<CropCategory> =
        diagnosedPositionsByPlot().flatMap { (plotId, positions) ->
            positions.map { (name, position) -> Triple(plotId, name, position) }
        }.mapNotNullTo(mutableSetOf()) { (plotId, name, position) ->
            val category = CropCategory.fromStorageName(name) ?: return@mapNotNullTo null
            category.takeIf {
                plotId != currentPlotId || !position.isInLoadedChunk() || !position.isMissingCrop(category)
            }
        }

    private fun missingDiagnosedCropsOnPlot(plotId: Int): Set<CropCategory> =
        missingDiagnosedCropPositionsOnPlot(plotId).keys

    private fun missingDiagnosedCropPositionsOnPlot(plotId: Int): Map<CropCategory, LorenzVec> =
        diagnosedPositionsByPlot()[plotId].orEmpty().entries.mapNotNull { (name, position) ->
            val category = CropCategory.fromStorageName(name) ?: return@mapNotNull null
            (category to position).takeIf {
                position.isInLoadedChunk() && position.isMissingCrop(category)
            }
        }.toMap()

    private fun missingRememberedCropPositionsOnPlot(plotId: Int): Map<CropCategory, LorenzVec> = buildMap {
        val diagnosedCategories = diagnosedPositionsByPlot()[plotId].orEmpty().keys
            .mapNotNullTo(mutableSetOf(), CropCategory::fromStorageName)
        detectedCropPositionsByPlot()[plotId].orEmpty().forEach { (name, position) ->
            val category = CropCategory.fromStorageName(name) ?: return@forEach
            if (category !in diagnosedCategories && position.isInLoadedChunk() && position.isMissingCrop(category)) {
                put(category, position)
            }
        }
        putAll(missingDiagnosedCropPositionsOnPlot(plotId))
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
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

    private fun LorenzVec.isMissingCrop(category: CropCategory): Boolean {
        val state = getBlockStateAt()
        return when {
            state.block in deadCropBlocks -> true

            // Crop Diagnostics supplies the identity for crops that can use custom backing blocks.
            category in diagnosticOnlyCrops -> {
                val hasNearbyCrop = findNearbyCropPosition(category, this) != null
                val hasFloatingHead = category in floatingHeadCrops && hasFloatingHeadAtCropPosition(category)
                !hasNearbyCrop && !hasFloatingHead
            }

            category in floatingHeadCrops && hasFloatingHeadAtCropPosition(category) -> false

            category in variableHeightCrops -> {
                (-VARIABLE_HEIGHT_SEARCH_RADIUS..VARIABLE_HEIGHT_SEARCH_RADIUS).none { yOffset ->
                    CropCategory.fromBlock(add(y = yOffset).getBlockStateAt().block) == category
                }
            }

            else -> CropCategory.fromBlock(state.block) != category
        }
    }

    /**
     * Hypixel sometimes renders Greenhouse crops such as cactus and moonflower as floating player heads.
     * Armor stand positions are at their feet, so use a taller Y search while keeping X/Z tight enough
     * that a decorative head belonging to a neighbouring crop cannot satisfy this position.
     */
    private fun LorenzVec.hasFloatingHeadAtCropPosition(category: CropCategory): Boolean {
        fun net.minecraft.world.entity.Entity.isInCropColumn(): Boolean =
            kotlin.math.abs(x - this@hasFloatingHeadAtCropPosition.x) <= FLOATING_HEAD_HORIZONTAL_RADIUS &&
                kotlin.math.abs(z - this@hasFloatingHeadAtCropPosition.z) <= FLOATING_HEAD_HORIZONTAL_RADIUS

        val armorStandHead = getEntitiesInBox<ArmorStand>(this, FLOATING_HEAD_SEARCH_RADIUS) {
            it.isInCropColumn() && listOf(it.getStandHelmet(), it.getHandItem()).any { stack ->
                stack.isFloatingCropHead(category)
            }
        }.isNotEmpty()
        if (armorStandHead) return true

        return getEntitiesInBox<Display.ItemDisplay>(this, FLOATING_HEAD_SEARCH_RADIUS) {
            it.isInCropColumn() && it.itemStack.isFloatingCropHead(category)
        }.isNotEmpty() || getEntitiesInBox<Display.BlockDisplay>(this, FLOATING_HEAD_SEARCH_RADIUS) {
            // Block displays do not expose an item name, so only use them for cactus where this was observed.
            category == CropCategory.CACTUS && it.isInCropColumn() && it.blockState.block in playerHeadBlocks
        }.isNotEmpty()
    }

    private fun SafeItemStack?.isFloatingCropHead(category: CropCategory): Boolean {
        return floatingCropHeadCategory() == category
    }

    private fun SafeItemStack?.floatingCropHeadCategory(): CropCategory? {
        if (this?.itemType != Items.PLAYER_HEAD) return null
        val name = cleanName.lowercase()
        return when {
            name.startsWith("cactus") -> CropCategory.CACTUS
            name.startsWith("sunflower") || name.startsWith("moonflower") -> CropCategory.SUNFLOWER
            name.startsWith("coco") -> CropCategory.COCOA_BEANS
            name.startsWith("pumpkin") -> CropCategory.PUMPKIN
            name.removePrefix("melon").toIntOrNull() != null -> CropCategory.MELON
            else -> null
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
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
    }

    private fun clearDiagnosticCropPositions() {
        val clearedPositions = buildSet {
            runtimeDiagnosedPositionsByPlot.values.forEach { addAll(it.keys) }
            storage?.diagnosedCropPositionsByPlot?.values.orEmpty().forEach { addAll(it.keys) }
            fallbackStorage?.values.orEmpty().forEach { addAll(it.keys) }
        }.size
        runtimeDiagnosedPositionsByPlot.clear()
        storage?.diagnosedCropPositionsByPlot?.clear()
        fallbackStorage?.clear()
        clearTargetedCrop()
        missingCropWaypoints = emptyMap()
        pendingPersistentSave = true
        savePendingData()
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
                    val loaded = isCompleteScanAreaLoaded(plot)
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
        val liveCrops = plot?.let(::scanGreenhouse).orEmpty()
        val savedByPlot = detectedCropsByPlot()
        val savedCrops = savedByPlot.values.flatten().mapNotNullTo(mutableSetOf(), CropCategory::fromStorageName)
        val diagnosedPositions = diagnosedPositionsByPlot()[plot?.id].orEmpty()
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

    private fun diagnosedPositionsByPlot(): Map<Int, Map<String, LorenzVec>> {
        return buildMap {
            storage?.diagnosedCropPositionsByPlot.orEmpty().forEach { (plotId, positions) ->
                put(plotId, positions.toMutableMap())
            }
            fallbackStorage.orEmpty().forEach { (plotId, positions) ->
                put(plotId, get(plotId).orEmpty() + positions)
            }
            runtimeDiagnosedPositionsByPlot.forEach { (plotId, positions) ->
                put(plotId, get(plotId).orEmpty() + positions)
            }
        }
    }

    private fun detectedCropsByPlot(): Map<Int, Set<String>> =
        storage?.detectedCropsByPlot.orEmpty() + runtimeDetectedCropsByPlot

    private fun saveDetectedCropPositions(plotId: Int, positions: Map<CropCategory, LorenzVec>) {
        val positionNames = positions.mapKeys { it.key.name }
        positionNames.keys.forEach {
            removeRememberedCategoryFromOtherPlots(it, plotId, diagnosed = false)
        }
        val runtimePositions = runtimeDetectedCropPositionsByPlot.getOrPut(plotId) { mutableMapOf() }
        if (positionNames.all { runtimePositions[it.key] == it.value }) return
        runtimePositions.putAll(positionNames)
        storage?.detectedCropPositionsByPlot?.getOrPut(plotId) { mutableMapOf() }?.putAll(positionNames)
        fallbackDetectedStorage?.getOrPut(plotId) { mutableMapOf() }?.putAll(positionNames)
        pendingPersistentSave = true
    }

    private fun removeRememberedCategoryFromOtherPlots(name: String, plotId: Int, diagnosed: Boolean) {
        val runtime = if (diagnosed) runtimeDiagnosedPositionsByPlot else runtimeDetectedCropPositionsByPlot
        val profile = if (diagnosed) storage?.diagnosedCropPositionsByPlot else storage?.detectedCropPositionsByPlot
        val fallback = if (diagnosed) fallbackStorage else fallbackDetectedStorage
        var changed = false
        listOfNotNull(runtime, profile, fallback).forEach { positionsByPlot ->
            positionsByPlot.filterKeys { it != plotId }.values.forEach {
                changed = it.remove(name) != null || changed
            }
        }
        if (changed) pendingPersistentSave = true
    }

    private fun rememberedCropPositions(): Map<Int, Map<CropCategory, LorenzVec>> = buildMap {
        detectedCropPositionsByPlot().forEach { (plotId, positions) ->
            val remembered = getOrPut(plotId) { mutableMapOf() }.toMutableMap()
            positions.forEach { (name, position) ->
                CropCategory.fromStorageName(name)?.let { remembered[it] = position }
            }
            put(plotId, remembered)
        }
        diagnosedPositionsByPlot().forEach { (plotId, positions) ->
            val remembered = getOrPut(plotId) { mutableMapOf() }.toMutableMap()
            positions.forEach { (name, position) ->
                CropCategory.fromStorageName(name)?.let { remembered[it] = position }
            }
            put(plotId, remembered)
        }
    }

    private fun detectedCropPositionsByPlot(): Map<Int, Map<String, LorenzVec>> = buildMap {
        storage?.detectedCropPositionsByPlot.orEmpty().forEach { (plotId, positions) ->
            put(plotId, positions.toMutableMap())
        }
        fallbackDetectedStorage.orEmpty().forEach { (plotId, positions) ->
            put(plotId, get(plotId).orEmpty() + positions)
        }
        runtimeDetectedCropPositionsByPlot.forEach { (plotId, positions) ->
            put(plotId, get(plotId).orEmpty() + positions)
        }
    }

    private fun savePendingData() {
        if (!pendingPersistentSave || storage == null && fallbackStorage == null && fallbackDetectedStorage == null) return
        SkyHanniMod.configManager.saveConfig(ConfigFileType.FEATURES, "greenhouse-crop-detection")
        pendingPersistentSave = false
    }

    private fun scoreboardShowsGreenhouse(): Boolean =
        SkyBlockUtils.scoreboardArea == "Greenhouse" ||
            ScoreboardData.sidebarLinesFormatted.any {
                it.removeColor().contains("Greenhouse", ignoreCase = true)
            }

    private fun isInGreenhouse(): Boolean = scoreboardShowsGreenhouse() || GardenPlotApi.inGreenhouse()

    private enum class CropCategory(
        val displayName: String,
        val blocks: Set<Block>,
        vararg val itemNames: String,
    ) {
        WHEAT("Wheat", setOf(Blocks.WHEAT)),
        CARROT("Carrot", setOf(Blocks.CARROTS)),
        POTATO("Potato", setOf(Blocks.POTATOES)),
        NETHER_WART("Nether Wart", setOf(Blocks.NETHER_WART)),
        PUMPKIN(
            "Pumpkin",
            setOf(Blocks.PUMPKIN, Blocks.CARVED_PUMPKIN, Blocks.PUMPKIN_STEM, Blocks.ATTACHED_PUMPKIN_STEM),
        ),
        MELON("Melon", setOf(Blocks.MELON), "Melon Slice"),
        COCOA_BEANS("Cocoa Beans", setOf(Blocks.COCOA)),
        SUGAR_CANE("Sugar Cane", setOf(Blocks.SUGAR_CANE)),
        CACTUS("Cactus", setOf(Blocks.CACTUS)),
        MUSHROOM("Mushroom", setOf(Blocks.RED_MUSHROOM, Blocks.BROWN_MUSHROOM), "Red Mushroom", "Brown Mushroom"),
        SUNFLOWER("Sunflower/Moonflower", setOf(Blocks.SUNFLOWER), "Sunflower", "Moonflower"),
        WILD_ROSE("Wild Rose", setOf(Blocks.ROSE_BUSH)),
        ;

        companion object {
            private val byBlock = entries.flatMap { category ->
                category.blocks.map { it to category }
            }.toMap()

            fun fromBlock(block: Block): CropCategory? = byBlock[block]
            fun fromStorageName(name: String): CropCategory? = entries.firstOrNull { it.name == name }
            fun fromDisplayName(name: String): CropCategory? = entries.firstOrNull {
                name == it.displayName || name in it.itemNames
            }
        }
    }

    private val deadCropBlocks = setOf(Blocks.DEAD_BUSH, Blocks.CHORUS_PLANT, Blocks.CHORUS_FLOWER)
    private val diagnosticOnlyCrops = setOf(CropCategory.PUMPKIN, CropCategory.COCOA_BEANS)
    private val variableHeightCrops = setOf(CropCategory.CACTUS, CropCategory.SUGAR_CANE)
    private val floatingHeadCrops = setOf(
        CropCategory.CACTUS,
        CropCategory.SUNFLOWER,
        CropCategory.COCOA_BEANS,
        CropCategory.PUMPKIN,
        CropCategory.MELON,
    )
    private val playerHeadBlocks = setOf(Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD)
    private val greenhouseStemBlocks = setOf(
        Blocks.MELON_STEM,
        Blocks.ATTACHED_MELON_STEM,
        Blocks.PUMPKIN_STEM,
        Blocks.ATTACHED_PUMPKIN_STEM,
    )

    private const val SCAN_RADIUS = 8
    private const val MIN_GARDEN_Y = 60
    private const val MAX_GARDEN_Y = 100
    private const val DIAGNOSTIC_SEARCH_RADIUS = 2
    private const val VARIABLE_HEIGHT_SEARCH_RADIUS = 2
    private const val FLOATING_HEAD_SEARCH_RADIUS = 2.5
    private const val FLOATING_HEAD_HORIZONTAL_RADIUS = 0.75
    private const val REQUIRED_STABLE_SCANS = 2
    private const val REQUIRED_STABLE_REMEMBERED_CHECKS = 3
}
