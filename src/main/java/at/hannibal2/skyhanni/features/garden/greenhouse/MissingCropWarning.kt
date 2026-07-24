package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.features.garden.plot.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.BlockUtils.getTargetedBlock
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks

@SkyHanniModule
object MissingCropWarning {

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val storage get() = ProfileStorageData.profileSpecific?.garden?.greenhouse

    private var previousScan: Set<CropCategory>? = null
    private var stableScanCount = 0
    private var lastReportedMissing: Set<CropCategory>? = null
    private var pendingDiagnostic: Pair<CropCategory, LorenzVec>? = null
    private var lastTargetedCrop: Pair<CropCategory, LorenzVec>? = null
    private var lastTargetedPosition: LorenzVec? = null
    private val runtimeDetectedCropsByPlot = mutableMapOf<Int, MutableSet<String>>()
    private val runtimeDiagnosedPositionsByPlot = mutableMapOf<Int, MutableMap<String, LorenzVec>>()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!config.missingCropWarning || !isInGreenhouse()) return
        if (Minecraft.getInstance().screen != null) return
        val position = getTargetedBlock() ?: return
        lastTargetedPosition = position
        CropCategory.fromBlock(position.getBlockStateAt().block)?.let {
            lastTargetedCrop = it to position
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        if (!config.missingCropWarning || !isInGreenhouse()) return
        if (event.clickType != InteractClickType.RIGHT_CLICK) return
        lastTargetedPosition = event.position
        val category = CropCategory.fromBlock(event.blockState.block) ?: return
        pendingDiagnostic = category to event.position
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Crop Diagnostics") return
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
            clearTargetedCrop()
            ChatUtils.chat(
                "§cCould not save ${category.displayName}: its position resolved to " +
                    "§e${targetedCategory.displayName}§c instead. The stale position was cleared.",
            )
            return
        }

        val position = nearbyPosition ?: if (targetedCategory == null) targetPosition.add(y = 1) else targetPosition
        clearTargetedCrop()
        runtimeDiagnosedPositionsByPlot.getOrPut(plotId) { mutableMapOf() }[category.name] = position
        storage?.diagnosedCropPositionsByPlot?.getOrPut(plotId) { mutableMapOf() }?.set(category.name, position)
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
        return BlockPos.betweenClosed(from, to)
            .filter { CropCategory.fromBlock(world.getBlockState(it).block) == category }
            .minByOrNull { it.distSqr(centerPos) }
            ?.toLorenzVec()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSecondPassed() {
        if (!config.missingCropWarning || !isInGreenhouse()) {
            reset()
            return
        }

        storage?.diagnosedCropPositionsByPlot?.let { saved ->
            runtimeDiagnosedPositionsByPlot.forEach { (plotId, positions) ->
                saved.getOrPut(plotId) { mutableMapOf() }.putAll(positions)
            }
        }

        val plot = GardenPlotApi.getCurrentPlot() ?: return
        val present = scanGreenhouse(plot) - missingDiagnosedCropsOnPlot(plot.id)
        if (present != previousScan) {
            previousScan = present
            stableScanCount = 1
            return
        }
        stableScanCount++
        if (stableScanCount < REQUIRED_STABLE_SCANS) return

        val presentNames = present.mapTo(mutableSetOf()) { it.name }
        runtimeDetectedCropsByPlot[plot.id] = presentNames
        storage?.detectedCropsByPlot?.apply {
            putAll(runtimeDetectedCropsByPlot)
            this[plot.id] = presentNames
        }

        val presentAcrossGreenhouses = detectedCropsByPlot().values
            .flatten()
            .mapNotNullTo(mutableSetOf(), CropCategory::fromStorageName)
        val missing = CropCategory.entries.toSet() - presentAcrossGreenhouses - diagnosedPresentCrops(plot.id)
        if (missing == lastReportedMissing) return
        lastReportedMissing = missing

        if (missing.isEmpty()) {
            ChatUtils.chat("§aAll 12 unique Greenhouse crops are planted!")
        } else {
            ChatUtils.chat(
                "§cMissing Greenhouse ${if (missing.size == 1) "crop" else "crops"}: §e" +
                    missing.joinToString("§7, §e") { it.displayName },
            )
        }
    }

    private fun scanGreenhouse(plot: GardenPlot): Set<CropCategory> {
        val world = MinecraftCompat.localWorldOrNull ?: return emptySet()
        val middle = plot.middle.toBlockPos()
        val from = BlockPos(middle.x - SCAN_RADIUS, MIN_GARDEN_Y, middle.z - SCAN_RADIUS)
        val to = BlockPos(middle.x + SCAN_RADIUS, MAX_GARDEN_Y, middle.z + SCAN_RADIUS)
        return buildSet {
            for (pos in BlockPos.betweenClosed(from, to)) {
                CropCategory.fromBlock(world.getBlockState(pos).block)?.let(::add)
                if (size == CropCategory.entries.size) return@buildSet
            }
        }
    }

    private fun reset() {
        previousScan = null
        stableScanCount = 0
        lastReportedMissing = null
        clearTargetedCrop()
    }

    private fun diagnosedPresentCrops(currentPlotId: Int?): Set<CropCategory> =
        diagnosedPositionsByPlot().flatMap { (plotId, positions) ->
            positions.map { (name, position) -> Triple(plotId, name, position) }
        }.mapNotNullTo(mutableSetOf()) { (plotId, name, position) ->
            val category = CropCategory.fromStorageName(name) ?: return@mapNotNullTo null
            category.takeIf {
                plotId != currentPlotId || !position.isInLoadedChunk() || !position.isMissingCrop()
            }
        }

    private fun missingDiagnosedCropsOnPlot(plotId: Int): Set<CropCategory> =
        diagnosedPositionsByPlot()[plotId].orEmpty().entries
            .groupBy({ it.key }, { it.value })
            .mapNotNullTo(mutableSetOf()) { (name, positions) ->
                val category = CropCategory.fromStorageName(name) ?: return@mapNotNullTo null
                category.takeIf {
                    positions.any { it.isInLoadedChunk() } &&
                        positions.filter { it.isInLoadedChunk() }.all { it.isMissingCrop() }
                }
            }

    private fun LorenzVec.isMissingCrop(): Boolean {
        val state = getBlockStateAt()
        return state.isAir || state.block in deadCropBlocks
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shgreenhousecropdebug") {
            description = "Shows the current state of the Greenhouse missing crop checker"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback { showDebugState() }
        }
    }

    private fun showDebugState() {
        val plot = GardenPlotApi.getCurrentPlot()
        val liveCrops = plot?.let(::scanGreenhouse).orEmpty()
        val savedByPlot = detectedCropsByPlot()
        val savedCrops = savedByPlot.values.flatten().mapNotNullTo(mutableSetOf(), CropCategory::fromStorageName)
        ChatUtils.chat(
            buildString {
                appendLine("§6Greenhouse Crop Checker Debug")
                appendLine(" §7Enabled: §e${config.missingCropWarning}")
                appendLine(" §7Scoreboard area: §e${SkyBlockUtils.scoreboardArea}")
                appendLine(" §7Scoreboard says Greenhouse: §e${scoreboardShowsGreenhouse()}")
                appendLine(" §7Plot marked as Greenhouse: §e${GardenPlotApi.inGreenhouse()}")
                appendLine(" §7Current plot: §e${plot?.id ?: "none"}")
                appendLine(" §7Live scan: §e${liveCrops.namesOrNone()}")
                appendLine(" §7Saved crops: §e${savedCrops.namesOrNone()}")
                val allPresent = savedCrops + diagnosedPresentCrops(plot?.id)
                append(" §7Missing: §e${(CropCategory.entries.toSet() - allPresent).namesOrNone()}")
            },
        )
    }

    private fun Collection<CropCategory>.namesOrNone(): String =
        ifEmpty { return "none" }.joinToString(", ") { it.displayName }

    private fun diagnosedPositionsByPlot(): Map<Int, Map<String, LorenzVec>> = buildMap {
        storage?.diagnosedCropPositionsByPlot.orEmpty().forEach { (plotId, positions) ->
            put(plotId, positions.toMutableMap())
        }
        runtimeDiagnosedPositionsByPlot.forEach { (plotId, positions) ->
            put(plotId, get(plotId).orEmpty() + positions)
        }
    }

    private fun detectedCropsByPlot(): Map<Int, Set<String>> =
        storage?.detectedCropsByPlot.orEmpty() + runtimeDetectedCropsByPlot

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
        MELON("Melon", setOf(Blocks.MELON, Blocks.MELON_STEM, Blocks.ATTACHED_MELON_STEM), "Melon Slice"),
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

    private const val SCAN_RADIUS = 8
    private const val MIN_GARDEN_Y = 60
    private const val MAX_GARDEN_Y = 100
    private const val DIAGNOSTIC_SEARCH_RADIUS = 2
    private const val REQUIRED_STABLE_SCANS = 3
}
