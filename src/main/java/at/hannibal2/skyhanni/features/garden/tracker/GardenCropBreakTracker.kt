package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.pet.CurrentPetApi
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.addCollectionCounter
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.garden.GardenToolChangeEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.GardenApi.getCropType
import at.hannibal2.skyhanni.features.garden.GardenApi.readCounter
import at.hannibal2.skyhanni.features.garden.GardenApi.readCultivatingCounter
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import kotlin.math.floor
import kotlin.random.Random

@SkyHanniModule
object GardenCropBreakTracker {
    private val storage get() = GardenApi.storage
    private val counterData: MutableMap<String, Long>? get() = storage?.toolCounterData
    private val cropMap: MutableMap<CropType, Int> = mutableMapOf()

    private var cropBrokenType: CropType? = null
    private var heldItem: ItemStack? = null
    private var itemHasCounter: Boolean = false
    private var itemHasBountiful = false
    private var mooshroomCowCrops: Int = 0
    private var bountifulCoins: Double = 0.0
    private var seedAmount: Int = 0

    @HandleEvent
    fun onToolChange(event: GardenToolChangeEvent) {
        heldItem = event.toolItem
        if (event.toolItem == null || event.toolInHand == null) return
        val counter = readCounter(event.toolItem)
        itemHasBountiful = event.toolItem.displayName.removeColor().startsWith("Bountiful")

        if (counter == null) {
            itemHasCounter = false
            return
        }

        itemHasCounter = true

        val uuid = event.toolItem.getItemUuid() ?: return
        counterData?.put(uuid, counter)

        if (event.toolItem.getCropType() == CropType.WHEAT) {
            val cultivatingCounter = readCultivatingCounter(event.toolItem) ?: return
            storage?.wheatCultivatingCounterData?.put(uuid, cultivatingCounter)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onCropBreak(event: CropClickEvent) {
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (event.crop != cropBrokenType) cropBrokenType = event.crop

        if (GardenApi.mushroomCowPet) {
            mooshroomCowCrops += weightedRandomRound(CurrentPetApi.currentPet?.level ?: 0)
        }

        if (itemHasCounter || heldItem == null) return

        val fortune = storage?.latestTrueFarmingFortune?.get(event.crop) ?: return
        val amount = ((weightedRandomRound((fortune % 100).toInt()) + floor(fortune / 100) + 1) * event.crop.baseDrops).toInt()
        bountifulCoins += amount * .2
        addToCropMap(event.crop, amount)

    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!itemHasCounter || event.itemStack.getItemUuid() != heldItem?.getItemUuid()) return
        val item = event.itemStack
        val uuid = item.getItemUuid() ?: return
        val counter = readCounter(item) ?: return
        val isHoe = GardenApi.readHoeCounter(item) != null

        val crop = if (isHoe || cropBrokenType == null) event.itemStack.getCropType() else cropBrokenType
        if (crop == null) return

        val old = counterData?.get(uuid) ?: return
        val addedCounter = if (counter < 0 && old > 0) {
            // 32 bit overflow protection
            counter + 4_294_967_296 - old
        } else {
            counter - old
        }

        addToCropMap(crop, addedCounter.toInt())
        bountifulCoins += addedCounter * .2
        counterData?.set(uuid, counter)

        // handle seeds
        if (item.getCropType() == CropType.WHEAT) {
            val cultCounter = readCultivatingCounter(event.itemStack) ?: return
            val oldCult = storage?.wheatCultivatingCounterData?.get(uuid) ?: return
            val addedCult = if (cultCounter < 0 && oldCult > 0) {
                // 32 bit overflow protection
                cultCounter + 4_294_967_296 - oldCult
            } else {
                cultCounter - oldCult
            }

            val seedDiff = addedCult - addedCounter
            seedAmount += seedDiff.toInt()
            bountifulCoins += seedDiff * .2

            storage?.wheatCultivatingCounterData?.set(uuid, cultCounter)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        if (cropMap.isEmpty()) return

        for (crop in cropMap) {
            val amount = cropMap.remove(crop.key)
            crop.key.addCollectionCounter(CropCollectionType.BREAKING_CROPS, amount?.toLong() ?: 0)
        }

        if (mooshroomCowCrops > 0) {
            CropType.MUSHROOM.addCollectionCounter(CropCollectionType.MOOSHROOM_COW, mooshroomCowCrops.toLong())
            mooshroomCowCrops = 0
        }

        if (bountifulCoins > 0) {
            GardenProfitTracker.addItem(GardenTrackerTypes.BREAKING_CROPS, SKYBLOCK_COIN, bountifulCoins.toInt(), false)
            bountifulCoins = 0.0
        }

        if (seedAmount > 0) {
            GardenProfitTracker.addItem(GardenTrackerTypes.BREAKING_CROPS, "seed".toInternalName(), seedAmount, false)
            seedAmount = 0
        }
    }

    private fun weightedRandomRound(num: Int): Int {
        val randomNumber = Random.nextInt(0, 100)
        return if (num >= randomNumber) 1 else 0
    }

    private fun addToCropMap(cropType: CropType, amount: Int) {
        cropMap[cropType] = cropMap[cropType]?.plus(amount) ?: amount
    }
}
