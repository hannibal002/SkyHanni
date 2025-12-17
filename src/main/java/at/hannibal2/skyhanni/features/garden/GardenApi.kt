package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityCollectionStats
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getCropType
import at.hannibal2.skyhanni.features.garden.CropType.Companion.isTimeFlower
import at.hannibal2.skyhanni.features.garden.GardenPlotApi.checkCurrentPlot
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay
import at.hannibal2.skyhanni.features.garden.contest.FarmingContestApi
import at.hannibal2.skyhanni.features.garden.farming.GardenBestCropTime
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed
import at.hannibal2.skyhanni.features.garden.fortuneguide.FFGuideGui
import at.hannibal2.skyhanni.features.garden.inventory.SkyMartCopperPrice
import at.hannibal2.skyhanni.features.garden.pests.PesthunterProfit
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFApi
import at.hannibal2.skyhanni.features.inventory.chocolatefactory.CFShopPrice
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.isBabyCrop
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils.isPlayerInside
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getCultivatingCounter
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHoeExp
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getOldHoeCounter
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object GardenApi {

    private val RARE_MOOSHROOM_COW_PET_ITEM = "MOOSHROOM_COW;2".toInternalName()

    var toolInHand: String? = null
    var itemInHand: ItemStack? = null
    var cropInHand: CropType? = null
    var pestCooldownEndTime = SimpleTimeMark.farPast()
    var lastCropBrokenTime = SimpleTimeMark.farPast()
    val mushroomCowPet
        get() = CurrentPetApi.isCurrentPetOrHigherRarity(RARE_MOOSHROOM_COW_PET_ITEM)
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
    private val cropIconCache = TimeLimitedCache<String, ItemStack>(10.minutes)
    val barnArea = AxisAlignedBB(35.5, 70.0, -4.5, -32.5, 100.0, -46.5)

    private var extraFarmingTools: Set<NeuInternalName> = setOf()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemInHandChange(event: ItemInHandChangeEvent) {
        checkItemInHand()
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        if (!inGarden()) return
        checkItemInHand()
        DelayedRun.runDelayed(500.milliseconds) {
            if (inGarden()) {
                checkItemInHand()
            }
        }
    }

    @HandleEvent
    fun onTick(event: SkyHanniTickEvent) {
        if (!inGarden()) return
        if (event.isMod(10, 1)) {
            inBarn = barnArea.isPlayerInside()
            if (cropInHand.isTimeFlower()) checkItemInHand()

            // We ignore random hypixel moments
            Minecraft.getMinecraft().currentScreen ?: return
            checkItemInHand()
        }
    }

    @HandleEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland != IslandType.GARDEN) return
        checkItemInHand()
        checkCurrentPlot()
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Garden API")
        if (!inGarden()) return event.addIrrelevant("Not in garden")

        event.addData {
            if (cropIconCache.isNotEmpty()) {
                add("cropIconCache:")
                addAll(
                    cropIconCache.map { (key, value) ->
                        " $key: ${value.getInternalName()}"
                    },
                )
            } else {
                add("cropIconCache is empty")
            }
        }
    }

    private fun updateGardenTool() {
        GardenToolChangeEvent(cropInHand, itemInHand).post()
    }

    private fun checkItemInHand() {
        val toolItem = InventoryUtils.getItemInHand()
        val crop = toolItem?.getCropType()
        val newTool = getToolInHand(toolItem, crop)
        if (toolInHand != newTool || crop != cropInHand) {
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

    private fun isOtherTool(internalName: NeuInternalName): Boolean =
        internalName in extraFarmingTools

    fun inGarden() = IslandType.GARDEN.isCurrent()

    fun isCurrentlyFarming() = inGarden() && GardenCropSpeed.averageBlocksPerSecond > 0.0 && hasFarmingToolInHand()

    fun hasFarmingToolInHand() = InventoryUtils.getItemInHand()?.let {
        val crop = it.getCropType()
        getToolInHand(it, crop) != null
    } ?: false

    fun ItemStack.getCropType(): CropType? {
        val internalName = getInternalName()
        if (internalName.startsWith("THEORETICAL_HOE_SUNFLOWER")) {
            return CropType.getTimeFlower()
        }
        return CropType.entries.firstOrNull { internalName.startsWith(it.toolName) }
    }

    fun readCounter(itemStack: ItemStack): Long? =
        itemStack.getCultivatingCounter() ?: itemStack.getHoeExp() ?: itemStack.getOldHoeCounter()

    fun CropType.getItemStackCopy(iconId: String): ItemStack = cropIconCache.getOrPut(iconId) { icon.copy() }

    fun hideExtraGuis() = ComposterOverlay.inInventory ||
        AnitaMedalProfit.inInventory ||
        SkyMartCopperPrice.inInventory ||
        FarmingContestApi.inInventory ||
        VisitorApi.inInventory ||
        FFGuideGui.isInGui() ||
        CFShopPrice.inInventory ||
        CFApi.inChocolateFactory ||
        CFApi.chocolateFactoryPaused ||
        HoppityCollectionStats.inInventory ||
        PesthunterProfit.isInInventory()

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        GardenBestCropTime.reset()
    }

    fun getCurrentlyFarmedCrop(): CropType? {
        val brokenCrop = if (toolInHand != null) GardenCropSpeed.lastBrokenCrop else null
        return cropInHand ?: brokenCrop
    }

    private var lastLocation: LorenzVec? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        val blockState = event.getBlockState
        val cropBroken = blockState.getCropType(event.position) ?: return
        if (cropBroken.multiplier == 1 && blockState.isBabyCrop()) return

        val position = event.position
        if (lastLocation == position) {
            return
        }

        lastLocation = position
        CropClickEvent(position, cropBroken, blockState, event.clickType, event.itemInHand).post()
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

    fun getGardenLevel(overflow: Boolean = true): Int {
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
        if (overflow) {
            totalExp += gardenOverflowExp

            while (totalExp < gardenExp) {
                tier++
                totalExp += gardenOverflowExp
            }
        }
        return tier
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        gardenExperience = data.gardenExp
        totalAmountVisitorsExisting = data.visitors.size
        extraFarmingTools = data.extraFarmingTools
    }

    private var gardenExperience = listOf<Int>()
    private const val gardenOverflowExp = 10000

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetcropspeed") {
            description = "Resets garden crop speed data and best crop time data"
            category = CommandCategory.USERS_RESET
            callback {
                storage?.cropsPerSecond?.clear()
                GardenBestCropTime.reset()
                updateGardenTool()
                ChatUtils.chat("Manually reset all crop speed data!")
            }
        }
    }
}
