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
import at.hannibal2.skyhanni.features.garden.GardenApi.lastBrokenCropType
import at.hannibal2.skyhanni.features.garden.GardenApi.readCounter
import at.hannibal2.skyhanni.features.garden.farming.CropMoneyDisplay.SEEDS
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getItemUuid
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeModifier
import net.minecraft.world.item.ItemStack
import kotlin.math.floor
import kotlin.random.Random

@SkyHanniModule
object GardenCropBreakTracker {
    private val storage get() = GardenApi.storage
    private val counterData: MutableMap<String, Long>? get() = storage?.toolCounterData
    private val cropMap: MutableMap<CropType, Int> = mutableMapOf()

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
        itemHasBountiful = event.toolItem.getReforgeModifier() == "bountiful"

        if (counter == null) {
            itemHasCounter = false
            return
        }

        itemHasCounter = true

        val uuid = event.toolItem.getItemUuid() ?: return
        counterData?.put(uuid, counter)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onCropBreak(event: CropClickEvent) {
        if (event.clickType != ClickType.LEFT_CLICK) return
        if (event.crop != lastBrokenCropType) lastBrokenCropType = event.crop

        if (GardenApi.mushroomCowPet) {
            mooshroomCowCrops += weightedRandomRound(CurrentPetApi.currentPet?.level ?: 0)
        }

        if (itemHasCounter || heldItem == null) return

        val fortune = storage?.latestTrueFarmingFortune?.get(event.crop) ?: return
        val amount = ((weightedRandomRound((fortune % 100).toInt()) + floor(fortune / 100) + 1) * event.crop.baseDrops).toInt()
        if (itemHasBountiful) bountifulCoins += amount * .2
        addToCropMap(event.crop, amount)

    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!itemHasCounter || event.itemStack.getItemUuid() != heldItem?.getItemUuid()) return
        val item = event.itemStack
        val uuid = item.getItemUuid() ?: return
        val counter = readCounter(item) ?: return

        val crop = lastBrokenCropType ?: event.itemStack.getCropType()
        if (crop == null) return

        val old = counterData?.get(uuid) ?: return
        var addedCounter = counter - old

        // cult counts both seeds and wheat so we have to split it, ratio is 1 wheat : 1.5 seeds
        if (crop == CropType.WHEAT) {
            val seeds = (addedCounter * .6f).toInt()
            if (itemHasBountiful) bountifulCoins += seeds * .2

            seedAmount += seeds
            addedCounter = (addedCounter * .4f).toLong()
        }

        addToCropMap(crop, addedCounter.toInt())
        if (itemHasBountiful) bountifulCoins += addedCounter * .2
        counterData?.set(uuid, counter)
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
            GardenProfitTracker.addItem(GardenTrackerTypes.BREAKING_CROPS, SEEDS, seedAmount, false)
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
