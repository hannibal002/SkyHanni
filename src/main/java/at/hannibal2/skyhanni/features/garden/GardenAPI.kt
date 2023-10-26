package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.CropClickEvent
import at.hannibal2.skyhanni.events.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenBestCropTime
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGUI
import at.hannibal2.skyhanni.features.garden.inventory.SkyMartCopperPrice
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import at.hannibal2.skyhanni.utils.BlockUtils.isBabyCrop
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.MinecraftDispatcher
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCultivatingCounter
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeCounter
import at.hannibal2.skyhanni.utils.jsonobjects.GardenJson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.AxisAlignedBB
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

object GardenAPI {
    var toolInHand: String? = null
    var itemInHand: ItemStack? = null
    var cropInHand: CropType? = null
    var mushroomCowPet = false
    private var inBarn = false
    val onBarnPlot get() = inBarn && inGarden()
    val config get() = ProfileStorageData.profileSpecific?.garden
    var gardenExp: Long?
        get() = config?.experience
        set(value) {
            value?.let {
                config?.experience = it
            }
        }

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
    fun onTick(event: LorenzTickEvent) {
        if (!inGarden()) return
        if (event.isMod(10)) {
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
        return if (isOtherTool(internalName)) internalName.asString() else null
    }

    private fun isOtherTool(internalName: NEUInternalName): Boolean {
        if (internalName.startsWith("DAEDALUS_AXE")) return true

        if (internalName.startsWith("BASIC_GARDENING_HOE")) return true
        if (internalName.startsWith("ADVANCED_GARDENING_AXE")) return true

        if (internalName.startsWith("BASIC_GARDENING_AXE")) return true
        if (internalName.startsWith("ADVANCED_GARDENING_HOE")) return true

        if (internalName.startsWith("ROOKIE_HOE")) return true

        if (internalName.startsWith("BINGHOE")) return true

        return false
    }

    fun inGarden() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN

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

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        gardenExperience = data.garden_exp
    }

    private var gardenExperience = listOf<Int>()
    private const val gardenOverflowExp = 10000
}
