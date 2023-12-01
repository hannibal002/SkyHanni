package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PetAPI
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenBestCropTime
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.inventory.SkyMartCopperPrice
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import at.hannibal2.skyhanni.utils.BlockUtils.isBabyCrop
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCultivatingCounter
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeCounter
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

object GardenAPI {
    var toolInHand: String? = null
    var itemInHand: ItemStack? = null
    var cropInHand: CropType? = null
    val mushroomCowPet get() = PetAPI.currentPet?.contains("Mooshroom Cow") ?: false
    private var inBarn = false
    val onBarnPlot get() = inBarn && inGarden()
    val storage get() = ProfileStorageData.profileSpecific?.garden
    val config get() = SkyHanniMod.feature.garden
    var totalAmountVisitorsExisting = 0
    var gardenExp: Long?
        get() = storage?.experience
        set(value) {
            value?.let {
                storage?.experience = it
            }
        }

    private val barnArea = AxisAlignedBB(35.5, 70.0, -4.5, -32.5, 100.0, -46.5)

    // TODO USE SH-REPO
    private val otherToolsList = listOf(
        "DAEDALUS_AXE",
        "BASIC_GARDENING_HOE",
        "ADVANCED_GARDENING_AXE",
        "BASIC_GARDENING_AXE",
        "ADVANCED_GARDENING_HOE",
        "ROOKIE_HOE",
        "BINGHOE"
    )
    private val LINE_COLOR = LorenzColor.YELLOW.toColor()

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!inGarden()) return
        if (event.packet !is C09PacketHeldItemChange) return
        checkItemInHand()
    }

    @SubscribeEvent
    fun onCloseWindow(event: GuiContainerEvent.CloseWindowEvent) {
        if (!inGarden()) return
        checkItemInHand()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!inGarden()) return
        if (event.isMod(10)) {
            inBarn = barnArea.isPlayerInside()

            // We ignore random hypixel moments
            Minecraft.getMinecraft().currentScreen ?: return
            checkItemInHand()
        }
    }

    // TODO use IslandChangeEvent
    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        DelayedRun.runDelayed(2.seconds) {
            if (inGarden()) {
                checkItemInHand()
            }
        }
    }

    private fun updateGardenTool() {
        GardenToolChangeEvent(cropInHand, itemInHand).postAndCatch()
    }

    private fun checkItemInHand() {
        val toolItem = InventoryUtils.getItemInHand()
        val crop = toolItem?.getCropType()
        val newTool = getToolInHand(toolItem, crop)
        if (toolInHand != newTool) {
            toolInHand = newTool
            cropInHand = crop
            itemInHand = toolItem
            updateGardenTool()
        }
    }

    private fun getToolInHand(toolItem: ItemStack?, crop: CropType?): String? {
        if (crop != null) return crop.cropName

        val internalName = toolItem?.getInternalName() ?: return null
        return if (isOtherTool(internalName)) internalName.asString() else null
    }

    private fun isOtherTool(internalName: NEUInternalName): Boolean {
        return internalName.asString() in otherToolsList
    }

    fun inGarden() = IslandType.GARDEN.isInIsland()

    fun ItemStack.getCropType(): CropType? {
        val internalName = getInternalName()
        return CropType.entries.firstOrNull { internalName.startsWith(it.toolName) }
    }

    fun readCounter(itemStack: ItemStack): Long = itemStack.getHoeCounter() ?: itemStack.getCultivatingCounter() ?: -1L

    fun MutableList<Any>.addCropIcon(crop: CropType) {
        try {
            add(crop.icon)
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    fun hideExtraGuis() = ComposterOverlay.inInventory || AnitaMedalProfit.inInventory ||
        SkyMartCopperPrice.inInventory || FarmingContestAPI.inInventory || VisitorAPI.inInventory || FFGuideGUI.isInGui()

    fun clearCropSpeed() {
        storage?.cropsPerSecond?.clear()
        GardenBestCropTime.reset()
        updateGardenTool()
        LorenzUtils.chat("Manually reset all crop speed data!")
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        GardenBestCropTime.reset()
    }

    fun getCurrentlyFarmedCrop(): CropType? {
        val brokenCrop = if (toolInHand != null) GardenCropSpeed.lastBrokenCrop else null
        return cropInHand ?: brokenCrop
    }

    private var lastLocation: LorenzVec? = null

    @SubscribeEvent
    fun onBlockBreak(event: BlockClickEvent) {
        if (!inGarden()) return

        val blockState = event.getBlockState
        val cropBroken = blockState.getCropType() ?: return
        if (cropBroken.multiplier == 1 && blockState.isBabyCrop()) return

        val position = event.position
        if (lastLocation == position) {
            return
        }

        lastLocation = position
        CropClickEvent(cropBroken, blockState, event.clickType, event.itemInHand).postAndCatch()
    }

    fun getExpForLevel(requestedLevel: Int): Long {
        var totalExp = 0L
        var tier = 0
        for (tierExp in gardenExperience) {
            totalExp += tierExp
            tier++
            if (tier == requestedLevel) {
                return totalExp
            }
        }

        while (tier < requestedLevel) {
            totalExp += gardenOverflowExp
            tier++
            if (tier == requestedLevel) {
                return totalExp
            }
        }
        return 0
    }

    fun getGardenLevel(): Int {
        val gardenExp = this.gardenExp ?: return 0
        var tier = 0
        var totalExp = 0L
        for (tierExp in gardenExperience) {
            totalExp += tierExp
            if (totalExp > gardenExp) {
                return tier
            }
            tier++
        }
        totalExp += gardenOverflowExp

        while (totalExp < gardenExp) {
            tier++
            totalExp += gardenOverflowExp
        }
        return tier
    }

    fun LorenzRenderWorldEvent.renderPlot(plot: GardenPlotAPI.Plot, lineColor: Color, cornerColor: Color) {

        // These don't refer to Minecraft chunks but rather garden plots, but I use
        // the word chunk as the logic closely represents how chunk borders are rendered in latter mc versions
        val plotSize = 96
        val chunkX = floor((plot.middle.x + 48) / plotSize).toInt()
        val chunkZ = floor((plot.middle.z + 48) / plotSize).toInt()
        val chunkMinX = (chunkX * plotSize) - 48
        val chunkMinZ = (chunkZ * plotSize) - 48

        // Lowest point in the garden
        val minHeight = 66
        val maxHeight = 256

        // Render 4 vertical corners
        for (i in 0..plotSize step plotSize) {
            for (j in 0..plotSize step plotSize) {
                val start = LorenzVec(chunkMinX + i, minHeight, chunkMinZ + j)
                val end = LorenzVec(chunkMinX + i, maxHeight, chunkMinZ + j)
                tryDraw3DLine(start, end, cornerColor, 2, true)
            }
        }

        // Render vertical on X-Axis
        for (x in 4..<plotSize step 4) {
            val start = LorenzVec(chunkMinX + x, minHeight, chunkMinZ)
            val end = LorenzVec(chunkMinX + x, maxHeight, chunkMinZ)
            // Front lines
            tryDraw3DLine(start, end, lineColor, 1, true)
            // Back lines
            tryDraw3DLine(start.addZ(plotSize), end.addZ(plotSize), lineColor, 1, true)
        }

        // Render vertical on Z-Axis
        for (z in 4..<plotSize step 4) {
            val start = LorenzVec(chunkMinX, minHeight, chunkMinZ + z)
            val end = LorenzVec(chunkMinX, maxHeight, chunkMinZ + z)
            // Left lines
            tryDraw3DLine(start, end, lineColor, 1, true)
            // Right lines
            tryDraw3DLine(start.addX(plotSize), end.addX(plotSize), lineColor, 1, true)
        }

        // Render horizontal
        for (y in minHeight..maxHeight step 4) {
            val start = LorenzVec(chunkMinX, y, chunkMinZ)
            // (minX, minZ) -> (minX, minZ + 96)
            tryDraw3DLine(start, start.addZ(plotSize), lineColor, 1, true)
            // (minX, minZ + 96) -> (minX + 96, minZ + 96)
            tryDraw3DLine(start.addZ(plotSize), start.addXZ(plotSize, plotSize), lineColor, 1, true)
            // (minX + 96, minZ + 96) -> (minX + 96, minZ)
            tryDraw3DLine(start.addXZ(plotSize, plotSize), start.addX(plotSize), lineColor, 1, true)
            // (minX + 96, minZ) -> (minX, minZ)
            tryDraw3DLine(start.addX(plotSize), start, lineColor, 1, true)
        }
    }

    private fun LorenzRenderWorldEvent.tryDraw3DLine(
        p1: LorenzVec,
        p2: LorenzVec,
        color: Color,
        lineWidth: Int,
        depth: Boolean
    ) {
        if (isOutOfBorders(p1)) return
        if (isOutOfBorders(p2)) return
        draw3DLine(p1, p2, color, lineWidth, depth)
    }

    private fun isOutOfBorders(location: LorenzVec) = when {
        location.x > 240 -> true
        location.x < -240 -> true
        location.z > 240 -> true
        location.z < -240 -> true

        else -> false
    }

    private fun LorenzVec.addX(x: Int) = add(x, 0, 0)
    private fun LorenzVec.addZ(z: Int) = add(0, 0, z)
    private fun LorenzVec.addXZ(x: Int, z: Int) = add(x, 0, z)

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        gardenExperience = data.garden_exp
        totalAmountVisitorsExisting = data.visitors.size
    }

    private var gardenExperience = listOf<Int>()
    private const val gardenOverflowExp = 10000
}
