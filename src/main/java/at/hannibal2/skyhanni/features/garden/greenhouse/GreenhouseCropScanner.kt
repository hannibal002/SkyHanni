package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.features.garden.plot.GardenPlot
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.BlockUtils.isInLoadedChunk
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesInBoundingBox
import at.hannibal2.skyhanni.utils.EntityUtils.getEntitiesInBox
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getHandItem
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getStandHelmet
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.itemType
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import kotlin.math.abs

internal object GreenhouseCropScanner {

    fun findNearbyCropPosition(category: CropCategory, center: LorenzVec): LorenzVec? {
        val world = MinecraftCompat.localWorldOrNull ?: return null
        if (category in headOnlyCrops) return findNearbyFloatingCropHead(category, center)

        val centerPos = center.toBlockPos()
        // Crop Diagnostics can open when the player right-clicks the farmland below a crop.
        // Prefer the clicked block and the block directly above it before searching the surrounding
        // mutation. Otherwise decorative blocks of the same crop type can steal the diagnosed position.
        listOf(centerPos, centerPos.above()).firstOrNull {
            CropCategory.fromBlock(world.getBlockState(it).block) == category
        }?.let { return it.toLorenzVec() }

        val from = centerPos.offset(-DIAGNOSTIC_SEARCH_RADIUS, -DIAGNOSTIC_SEARCH_RADIUS, -DIAGNOSTIC_SEARCH_RADIUS)
        val to = centerPos.offset(DIAGNOSTIC_SEARCH_RADIUS, DIAGNOSTIC_SEARCH_RADIUS, DIAGNOSTIC_SEARCH_RADIUS)
        return BlockPos.betweenClosed(from, to)
            .filter { CropCategory.fromBlock(world.getBlockState(it).block) == category }
            .minByOrNull { it.distSqr(centerPos) }
            ?.toLorenzVec()
    }

    fun scanGreenhouse(plot: GardenPlot): Set<CropCategory> =
        scanGreenhousePositions(plot).keys

    fun scanGreenhousePositions(plot: GardenPlot): Map<CropCategory, LorenzVec> {
        val world = MinecraftCompat.localWorldOrNull ?: return emptyMap()
        val middle = plot.middle.toBlockPos()
        val from = BlockPos(middle.x - SCAN_RADIUS, MIN_GARDEN_Y, middle.z - SCAN_RADIUS)
        val to = BlockPos(middle.x + SCAN_RADIUS, MAX_GARDEN_Y, middle.z + SCAN_RADIUS)
        return buildMap {
            for (pos in BlockPos.betweenClosed(from, to)) {
                val block = world.getBlockState(pos).block
                CropCategory.fromBlock(block)?.let {
                    if (it !in headOnlyCrops) putIfAbsent(it, pos.toLorenzVec())
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

    fun isCompleteScanAreaLoaded(plot: GardenPlot): Boolean {
        val middle = plot.middle
        return listOf(
            middle.add(x = -SCAN_RADIUS, z = -SCAN_RADIUS),
            middle.add(x = -SCAN_RADIUS, z = SCAN_RADIUS),
            middle.add(x = SCAN_RADIUS, z = -SCAN_RADIUS),
            middle.add(x = SCAN_RADIUS, z = SCAN_RADIUS),
        ).all { it.isInLoadedChunk() }
    }

    fun isMissingCrop(position: LorenzVec, category: CropCategory): Boolean {
        val state = position.getBlockStateAt()
        return when {
            state.block in deadCropBlocks -> true

            // Crop Diagnostics supplies the identity for crops that can use custom backing blocks.
            category in diagnosticOnlyCrops -> {
                val hasNearbyCrop = findNearbyCropPosition(category, position) != null
                val hasFloatingHead = category in floatingHeadCrops &&
                    position.hasFloatingHeadAtCropPosition(category)
                !hasNearbyCrop && !hasFloatingHead
            }

            category in floatingHeadCrops && position.hasFloatingHeadAtCropPosition(category) -> false

            category in variableHeightCrops -> {
                (-VARIABLE_HEIGHT_SEARCH_RADIUS..VARIABLE_HEIGHT_SEARCH_RADIUS).none { yOffset ->
                    CropCategory.fromBlock(position.add(y = yOffset).getBlockStateAt().block) == category
                }
            }

            else -> CropCategory.fromBlock(state.block) != category
        }
    }

    private fun findNearbyFloatingCropHead(category: CropCategory, center: LorenzVec): LorenzVec? = buildList {
        getEntitiesInBox<ArmorStand>(center, FLOATING_HEAD_SEARCH_RADIUS) { stand ->
            listOf(stand.getStandHelmet(), stand.getHandItem()).any { it.isFloatingCropHead(category) }
        }.mapTo(this) { it.getLorenzVec() }
        getEntitiesInBox<Display.ItemDisplay>(center, FLOATING_HEAD_SEARCH_RADIUS) {
            it.itemStack.isFloatingCropHead(category)
        }.mapTo(this) { it.getLorenzVec() }
    }.minByOrNull { it.distanceSq(center) }

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

    /**
     * Hypixel sometimes renders Greenhouse crops such as cactus and moonflower as floating player heads.
     * Armor stand positions are at their feet, so use a taller Y search while keeping X/Z tight enough
     * that a decorative head belonging to a neighbouring crop cannot satisfy this position.
     */
    private fun LorenzVec.hasFloatingHeadAtCropPosition(category: CropCategory): Boolean {
        fun net.minecraft.world.entity.Entity.isInCropColumn(): Boolean =
            abs(x - this@hasFloatingHeadAtCropPosition.x) <= FLOATING_HEAD_HORIZONTAL_RADIUS &&
                abs(z - this@hasFloatingHeadAtCropPosition.z) <= FLOATING_HEAD_HORIZONTAL_RADIUS

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

    private fun SafeItemStack?.isFloatingCropHead(category: CropCategory): Boolean =
        floatingCropHeadCategory() == category

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
    private val headOnlyCrops = setOf(
        CropCategory.COCOA_BEANS,
        CropCategory.PUMPKIN,
        CropCategory.MELON,
    )
    private val playerHeadBlocks = setOf(Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD)

    private const val SCAN_RADIUS = 8
    private const val MIN_GARDEN_Y = 60
    private const val MAX_GARDEN_Y = 100
    private const val DIAGNOSTIC_SEARCH_RADIUS = 2
    private const val VARIABLE_HEIGHT_SEARCH_RADIUS = 2
    private const val FLOATING_HEAD_SEARCH_RADIUS = 2.5
    private const val FLOATING_HEAD_HORIZONTAL_RADIUS = 0.75
}

internal enum class CropCategory(
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
