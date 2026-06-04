package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ClickType
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
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import at.hannibal2.skyhanni.utils.compat.getEquipmentSlots
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB

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
                ZERO -> -9 to -9
                NINETY -> 9 to -9
                ONE_HUNDRED_EIGHTY -> 9 to 9
                TWO_HUNDRED_SEVENTY -> -9 to 9
            }
            return topLeft.boundingToOffset(dx.toDouble(), 0.0, dz.toDouble()).setMinY(0.0).setMaxY(100.0)
        }

        override fun toString(): String = displayName
    }

    enum class LayoutDisplayType(val displayName: String) {
        ALL("§aAll"),
        INPUTS_AND_TARGETS("§6Inputs §7& §bTargets"),
        INPUTS("§6Inputs"),
        INPUTS_AND_SURFACES("§6Inputs §7& §dSurfaces"),
        TARGETS("§bTargets"),
        TARGETS_AND_SURFACES("§bTargets §7& §dSurfaces"),
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

    enum class GreenhouseCropRole(val color: LorenzColor) {
        TARGET(LorenzColor.AQUA),
        INPUT(LorenzColor.GOLD);

        companion object {
            fun fromInt(value: Int) = entries.getOrElse(value) { INPUT }
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
                updateLayoutDataCommand()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onClick(event: EntityClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) {
            updateCropsInGreenhouse()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType != ClickType.RIGHT_CLICK) return
        val currentLayout = layout ?: return

        val matchingEntry = currentLayout.grid.entries.firstOrNull {
            getWorldPosition(it.key)?.equalsIgnoreY(event.position) == true
        } ?: return

        matchingEntry.value.plantedCrop = event.itemInHand?.cleanName()
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        updateLayoutData(config.layout.get())
        config.layout.whenChanged { _, new -> updateLayoutData(new) }
        config.layoutRotation.whenChanged { _, new -> updateTopLeftOfLayout(GardenPlotApi.getCurrentPlot()) }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onProfileJoin(event: ProfileJoinEvent) {
        updateTopLeftOfLayout(GardenPlotApi.getCurrentPlot())
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onIslandJoin(event: IslandJoinEvent) {
        updateTopLeftOfLayout(GardenPlotApi.getCurrentPlot())
    }

    @HandleEvent
    fun onPlotSwitch(event: PlotChangeEvent) {
        updateTopLeftOfLayout(event.plot)
        updateCropsInGreenhouse()
    }

    fun updateLayoutData(layoutString: String) {
        try {
            layoutDataPattern.matchMatcher(layoutString) {
                layout = GreenhouseLayout(group("data")).takeIf { it.grid.isNotEmpty() }
            }
        } catch (exception: Exception) {
            ErrorManager.logErrorWithData(exception)
        }
    }

    fun updateLayoutDataCommand() {
        val clipboard = OSUtils.readFromClipboard()
        if (clipboard.isNullOrEmpty()) return ChatUtils.userError("Your clipboard is empty!")

        updateLayoutData(clipboard)

        ChatUtils.chat("§aLayout imported!")
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
        val xFix = if (gridPosition.x == 0) 0.001 else -0.001
        val yFix = if (surface == Blocks.FARMLAND && worldPos.getBlockAt() != Blocks.FARMLAND) 0.0625 + 0.001 else 0.001
        val zFix = if (gridPosition.y == 0) 0.001 else -0.001
        worldPos.add(xFix, yFix, zFix)
    }

    fun getWorldPosition(gridPosition: GridPosition) = topLeftOfLayout?.let { topLeft ->
        topLeft - LorenzVec(gridPosition.x, 0, gridPosition.y)
    }
}
