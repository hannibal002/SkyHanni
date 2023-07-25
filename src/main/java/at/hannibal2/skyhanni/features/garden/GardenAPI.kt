package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenBestCropTime
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.features.garden.inventory.SkyMartCopperPrice
import at.hannibal2.skyhanni.utils.BlockUtils.isBabyCrop
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MinecraftDispatcher
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCultivatingCounter
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeCounter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.time.Duration.Companion.seconds

object GardenAPI {
    var toolInHand: String? = null
    var itemInHand: ItemStack? = null
    var cropInHand: CropType? = null
    var mushroomCowPet = false
    private var inBarn = false
    val onBarnPlot get() = inBarn && inGarden()
    val config get() = ProfileStorageData.profileSpecific?.garden

    var tick = 0
    private val barnArea = AxisAlignedBB(35.5, 70.0, -4.5, -32.5, 100.0, -46.5)

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
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (!inGarden()) return
        tick++
        if (tick % 10 == 0) {
            inBarn = barnArea.isPlayerInside()

            // We ignore random hypixel moments
            Minecraft.getMinecraft().currentScreen ?: return
            checkItemInHand()
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (inGarden()) {
            mushroomCowPet = event.tabList.any { it.startsWith(" Strength: §r§c❁") }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        SkyHanniMod.coroutineScope.launch {
            delay(2.seconds)
            withContext(MinecraftDispatcher) {
                if (inGarden()) {
                    checkItemInHand()
                }
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
        return if (isOtherTool(internalName)) internalName else null
    }

    private fun isOtherTool(internalName: String): Boolean {
        if (internalName.startsWith("DAEDALUS_AXE")) return true

        if (internalName.startsWith("BASIC_GARDENING_HOE")) return true
        if (internalName.startsWith("ADVANCED_GARDENING_AXE")) return true

        if (internalName.startsWith("BASIC_GARDENING_AXE")) return true
        if (internalName.startsWith("ADVANCED_GARDENING_HOE")) return true

        if (internalName.startsWith("ROOKIE_HOE")) return true

        return false
    }

    fun inGarden() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN

    fun ItemStack.getCropType(): CropType? {
        val internalName = getInternalName()
        return CropType.values().firstOrNull { internalName.startsWith(it.toolName) }
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
            SkyMartCopperPrice.inInventory || FarmingContestAPI.inInventory

    fun clearCropSpeed() {
        config?.cropsPerSecond?.clear()
        GardenBestCropTime.reset()
        updateGardenTool()
        LorenzUtils.chat("§e[SkyHanni] Manually reset all crop speed data!")
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
        if (cropBroken.multiplier == 1) {
            if (blockState.isBabyCrop()) return
        }

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
        return 0
    }

    fun getLevelForExp(gardenExp: Long): Int {
        var tier = 0
        var totalExp = 0L
        for (tierExp in gardenExperience) {
            totalExp += tierExp
            if (totalExp > gardenExp) {
                return tier
            }
            tier++
        }
        return tier
    }

    private val gardenExperience = listOf(
        0,
        70,
        100,
        140,
        240,
        600,
        1500,
        2000,
        2500,
        3000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000, // level 15

        // overflow levels till 40 for now, in 10k steps
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
        10_000,
    )
}