package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.IslandJoinEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.garden.PlotChangeEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.greenhouse
import at.hannibal2.skyhanni.features.garden.greenhouse.GreenhouseCropUtils.blockToCropName
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.getEquipmentSlots
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object GreenhouseLayoutApi {

    private val config get() = SkyHanniMod.feature.garden.greenhouse.designImporter
    private val patternGroup = RepoPattern.group("greenhouse.design")

    private val layoutDataPattern by patternGroup.pattern(
        "layout.data",
        "(?:https://(?:api\\.skyshards\\.com|skymutations\\.eu)/(?:share/|greenhouse\\?layout=))?(?<data>.+)",
    )

    var layout: GreenhouseLayout? = null
    var topLeftOfLayout: LorenzVec? = null
    var layoutBoundingBox: AABB? = null

    enum class LayoutRotation(val displayName: String) {
        ZERO("0°"),
        NINETY("90°"),
        ONE_HUNDRED_EIGHTY("180°"),
        TWO_HUNDRED_SEVENTY("270°");

        fun getTopLeftOfLayout(box: AABB): LorenzVec {
            val base = LorenzVec(box.maxX - 44, 73.0, box.maxZ - 44)
            val (dx, dz) = when (this) {
                ZERO -> 0 to 0
                NINETY -> -9 to 0
                ONE_HUNDRED_EIGHTY -> -9 to -9
                TWO_HUNDRED_SEVENTY -> 0 to -9
            }
            return base.add(x = dx, z = dz).roundTo(0)
        }

        fun getLayoutBoundingBox(topLeft: LorenzVec): AABB {
            val (dx, dz) = when (this) {
                ZERO -> -10 to -10
                NINETY -> 10 to -10
                ONE_HUNDRED_EIGHTY -> 10 to 10
                TWO_HUNDRED_SEVENTY -> -10 to 10
            }
            val topLeftChangeX = if (dx == -10) 1 else 0
            val topLeftChangeZ = if (dx == -10) 1 else 0

            val topLeftCorner = topLeft.add(x = topLeftChangeX, z = topLeftChangeZ)
            return topLeftCorner.boundingToOffset(dx.toDouble(), 0.0, dz.toDouble()).setMinY(0.0).setMaxY(100.0)
        }

        override fun toString(): String = displayName
    }

    enum class LayoutDisplayType(val displayName: String) {
        ALL("§aAll"),
        INPUTS_AND_TARGETS("§6Inputs §7& §bTargets"),
        INPUTS_AND_SURFACES("§6Inputs §7& §dSurfaces"),
        TARGETS_AND_SURFACES("§bTargets §7& §dSurfaces"),
        INPUTS("§6Inputs"),
        TARGETS("§bTargets"),
        SURFACES("§dSurfaces");

        fun shouldRenderCrop(slotInfo: SlotInfo) = when (this) {
            ALL, INPUTS_AND_TARGETS -> true
            INPUTS, INPUTS_AND_SURFACES -> slotInfo.type == GreenhouseCropRole.INPUT
            TARGETS, TARGETS_AND_SURFACES -> slotInfo.type == GreenhouseCropRole.TARGET
            SURFACES -> false
        }

        fun shouldRenderSurface() = listOf(ALL, INPUTS_AND_SURFACES, TARGETS_AND_SURFACES, SURFACES).contains(this)

        override fun toString(): String = displayName
    }

    enum class GreenhouseCropRole {
        TARGET,
        INPUT;

        companion object {
            fun fromInt(value: Int) = entries.getOrElse(value) { INPUT }
            fun GreenhouseCropRole.getColor() = when (this) {
                TARGET -> config.targetColor
                INPUT -> config.inputColor
            }
        }
    }

    data class GridPosition(val x: Int, val y: Int)
    data class SlotInfo(val crop: String, val type: GreenhouseCropRole, val surface: Block, var plantedCrop: String? = null)

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier(name = "shimportgreenhouse") {
            description = "Import a greenhouse layout from your clipboard."
            category = CommandCategory.USERS_ACTIVE
            simpleCallback {
                updateLayoutCommand()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onEntityClick(event: EntityClickEvent) {
        updateCropsInGreenhouse()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType != InteractClickType.RIGHT_CLICK) {
            DelayedRun.runDelayed(1.seconds, { updateCropsInGreenhouse() })
            return
        }
        val currentLayout = layout ?: return

        val matchingEntry = currentLayout.grid.entries.firstOrNull {
            getWorldPosition(it.key)?.equalsIgnoreY(event.position) == true
        } ?: return

        matchingEntry.value.plantedCrop = event.itemInHand?.cleanName()
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        updateLayout(config.layout.get())
        config.layout.whenChanged { _, new -> updateLayout(new) }
        config.layoutRotation.whenChanged { _, new -> updateLayoutData() }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onProfileJoin(event: ProfileJoinEvent) {
        updateLayoutData()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onIslandJoin(event: IslandJoinEvent) {
        updateLayoutData()
    }

    @HandleEvent
    fun onPlotChange(event: PlotChangeEvent) {
        updateLayoutData(event.plot)
    }

    fun updateLayout(layoutString: String) {
        try {
            layoutDataPattern.matchMatcher(layoutString) {
                layout = GreenhouseLayout(group("data")).takeIf { it.grid.isNotEmpty() }
                config.layout = Property.of(group("data"))
                updateLayoutData()
            }
        } catch (exception: Exception) {
            ErrorManager.logErrorWithData(exception)
        }
    }

    fun updateLayoutCommand() {
        val clipboard = OSUtils.readFromClipboard()
        if (clipboard.isNullOrEmpty()) return ChatUtils.userError("Your clipboard is empty!")

        updateLayout(clipboard)

        if (layout?.grid?.isEmpty() == false) ChatUtils.chat("§aLayout imported!")
    }

    private fun updateTopLeftOfLayout(plot: GardenPlotApi.Plot?) {
        if (plot == null || !plot.greenhouse) {
            topLeftOfLayout = null
            layoutBoundingBox = null
            return
        }

        val gridRotation = config.layoutRotation.get()
        val topLeft = gridRotation.getTopLeftOfLayout(plot.box)
        topLeftOfLayout = topLeft
        layoutBoundingBox = gridRotation.getLayoutBoundingBox(topLeft)
    }

    fun updateCropsInGreenhouse() {
        val currentData = layout ?: return
        val boundingBox = layoutBoundingBox ?: return

        val entities = EntityUtils.getEntitiesInBoundingBox<ArmorStand>(boundingBox)
            .filter { stand -> stand.getEquipmentSlots().any { it.value != null } }

        val entriesByWorldPos = currentData.grid.entries.associateBy { entry -> getWorldPosition(entry.key) }
        val mutationDetections = detectMutations(entities, entriesByWorldPos)

        currentData.grid.forEach { (gridPos, slotInfo) ->
            val worldPos = getWorldPosition(gridPos) ?: return@forEach
            val detectedMutation = mutationDetections[gridPos]

            if (detectedMutation != null) {
                slotInfo.plantedCrop = detectedMutation
            } else {
                slotInfo.plantedCrop = blockToCropName(worldPos.getBlockAt())
            }
        }
    }

    private fun detectMutations(
        entities: List<ArmorStand>,
        entriesByWorldPos: Map<LorenzVec?, Map.Entry<GridPosition, SlotInfo>>,
    ): Map<GridPosition, String> = buildMap {
        for (entity in entities) {
            val entityPos = entity.getLorenzVec()
            val entityVec3 = entityPos.toVec3()

            val matchedGridKey = entriesByWorldPos.entries.firstOrNull { entry ->
                entry.key?.getBoundingBox()?.contains(entityVec3) == true
            }?.value?.key ?: continue

            val mutationName = getMutationName(entity) ?: continue
            put(matchedGridKey, mutationName)
        }
    }

    private fun getMutationName(entity: ArmorStand): String? {
        for ((_, stack) in entity.getEquipmentSlots()) {
            val adjustedStack = stack.orNull() ?: continue
            val name = adjustedStack.cleanName()
            if (name.isNotEmpty()) return name
        }
        return null
    }

    private fun LorenzVec.getBoundingBox(): AABB {
        return when (config.layoutRotation.get()) {
            LayoutRotation.ZERO -> boundingToOffset(1.0, 0.0, 1.0)
            LayoutRotation.NINETY -> boundingToOffset(-1.0, 0.0, 1.0)
            LayoutRotation.ONE_HUNDRED_EIGHTY -> boundingToOffset(-1.0, 0.0, -1.0)
            LayoutRotation.TWO_HUNDRED_SEVENTY -> boundingToOffset(1.0, 0.0, -1.0)
        }.setMinY(0.0).setMaxY(100.0)
    }

    fun getFakeSurfacePosition(gridPosition: GridPosition, surface: Block) = getWorldPosition(gridPosition)?.let { worldPos ->
        val (xFix, zFix) = when (config.layoutRotation.get()) {
            LayoutRotation.ZERO -> {
                (if (gridPosition.x == 0) 0.001 else -0.001) to (if (gridPosition.y == 0) 0.001 else -0.001)
            }
            LayoutRotation.NINETY -> {
                (if (gridPosition.y == 0) -0.001 else 0.001) to (if (gridPosition.x == 0) 0.001 else -0.001)
            }
            LayoutRotation.ONE_HUNDRED_EIGHTY -> {
                (if (gridPosition.x == 0) -0.001 else 0.001) to (if (gridPosition.y == 0) -0.001 else 0.001)
            }
            LayoutRotation.TWO_HUNDRED_SEVENTY -> {
                (if (gridPosition.y == 0) 0.001 else -0.001) to (if (gridPosition.x == 0) -0.001 else 0.001)
            }
        }
//         val xFix = if (gridPosition.x == 0) 0.001 else -0.001
        val yFix = if (surface == Blocks.FARMLAND && worldPos.getBlockAt() != Blocks.FARMLAND) 0.0625 + 0.001 else 0.001
//         val zFix = if (gridPosition.y == 0) 0.001 else -0.001
        worldPos.add(xFix, yFix, zFix)
    }

    fun getWorldPosition(gridPosition: GridPosition): LorenzVec? {
        val topLeft = topLeftOfLayout ?: return null

        val (dx, dz) = when (config.layoutRotation.get()) {
            LayoutRotation.ZERO -> -gridPosition.x to -gridPosition.y
            LayoutRotation.NINETY -> gridPosition.y to -gridPosition.x
            LayoutRotation.ONE_HUNDRED_EIGHTY -> gridPosition.x to gridPosition.y
            LayoutRotation.TWO_HUNDRED_SEVENTY -> -gridPosition.y to gridPosition.x
        }

        return topLeft.add(x = dx, y = 0, z = dz)
    }

    private fun updateLayoutData(plot: GardenPlotApi.Plot? = null) {
        updateTopLeftOfLayout(plot ?: GardenPlotApi.getCurrentPlot())
        updateCropsInGreenhouse()
    }
}
