package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.FarmingProfitTrackerConfig.TrackedSource
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.garden.farming.CropCollectionAddEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.farming.HoeLevelDisplay
import at.hannibal2.skyhanni.features.garden.tracker.RareCropTracker.RareCropDropType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.containsKeys
import java.util.EnumMap
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FarmingProfitTrackerCrops {

    private val toolExpCapsule = "TOOL_EXP_CAPSULE".toInternalName()
    private val sackCompactionTimeout = 30.seconds

    private data class CropPrimitiveItem(
        val crop: CropType,
        val rawInternalName: NeuInternalName,
        val rawAmount: Long,
    )

    /**
     * REGEX-TEST: BLESSED! You found an Enchanted Nether Wart!
     * REGEX-TEST: BLESSED! You found a Cropie!
     */
    private val blessedDropPattern by FarmingProfitTracker.patternGroup.pattern(
        "blessed.drop",
        "^BLESSED! You found an? (?<item>.+)!$",
    )

    private val blocksBrokenCache: MutableMap<CropType, Long> = EnumMap(CropType::class.java)
    private val pendingReplenishCosts: MutableMap<CropType, Long> = EnumMap(CropType::class.java)
    private val cropInternalNames = mutableMapOf<CropType, NeuInternalName>()
    private val recentSpecialCropItems = mutableMapOf<NeuInternalName, Long>()

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onCropCollectionAdd(event: CropCollectionAddEvent) {
        val source = event.cropCollectionType.toTrackedSource() ?: return
        if (!FarmingProfitTracker.shouldTrack(source)) return
        trackCropAmount(event.crop, source, event.amount)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onCropClick(event: CropClickEvent) {
        clearSessionStateIfSessionWasReset()
        blocksBrokenCache.addOrPut(event.crop, 1)
        queueReplenishCost(event)
        FarmingProfitTracker.markActivity()
        FarmingProfitTracker.firstUpdate()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5) || blocksBrokenCache.isEmpty()) return
        val pending = EnumMap<CropType, Long>(blocksBrokenCache)
        blocksBrokenCache.clear()
        FarmingProfitTracker.modify { data ->
            pending.forEach { (crop, amount) ->
                data.addBlocksBroken(crop, amount)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        checkRareCropDrop(event.cleanMessage)
        checkBlessedDrop(event.cleanMessage)
        checkCropFeverStart(event.cleanMessage)
        checkCropFeverDrop(event.cleanMessage)
        checkToolExpCapsule(event.cleanMessage)
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    private fun onItemAdd(event: ItemAddEvent) {
        when (event.source) {
            ItemAddManager.Source.COMMAND -> if (FarmingProfitTracker.config.enabled) {
                FarmingProfitTracker.run { event.addItemFromEvent() }
            }

            ItemAddManager.Source.ITEM_ADD,
            ItemAddManager.Source.SACKS,
            -> trackCropItemFromItemAdd(event)

            else -> Unit
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN, priority = HandleEvent.LOWEST)
    private fun onSackChange(event: SackChangeEvent) {
        if (event.sackChanges.isNotEmpty()) recentSpecialCropItems.clear()
    }

    internal fun addRareCropItem(internalName: NeuInternalName?, dropType: RareCropDropType) {
        internalName?.let { rememberSpecialCropItem(it, 1) }
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.RARE_CROPS)) return
        if (internalName != null) {
            FarmingProfitTracker.addTrackedItem(TrackedSource.RARE_CROPS, internalName, 1L)
        }
        FarmingProfitTracker.modify {
            it.rareCropDrops.addOrPut(dropType, 1)
        }
        FarmingProfitTracker.markActivity()
    }

    internal fun rememberSpecialCropItem(internalName: NeuInternalName, amount: Long) {
        if (amount <= 0) return
        internalName.getCropPrimitiveItemOrNull() ?: return
        recentSpecialCropItems.addOrPut(internalName, amount)
    }

    private fun checkRareCropDrop(message: String) {
        for (dropType in RareCropDropType.entries.filter { it.hasRareCropMessage }) {
            if (!dropType.chatPattern.matches(message)) continue
            val internalName = dropType.takeIf { it.isItemDrop }?.let {
                NeuInternalName.fromItemNameOrNull(it.dropName.removeColor())
            }
            addRareCropItem(internalName, dropType)
            return
        }
    }

    private fun checkBlessedDrop(message: String) {
        blessedDropPattern.matchMatcher(message) {
            val internalName = NeuInternalName.fromItemNameOrNull(group("item")) ?: return
            rememberSpecialCropItem(internalName, 1)
            if (!FarmingProfitTracker.shouldTrack(TrackedSource.BLESSED)) return@matchMatcher
            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            val crop = CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor)

            FarmingProfitTracker.addTrackedItem(TrackedSource.BLESSED, internalName, 1L)
            FarmingProfitTracker.modify {
                it.blessedDrops.addOrPut(internalName, 1)
                crop?.let { cropType ->
                    it.addCropAmount(cropType, TrackedSource.BLESSED, primitiveStack.amount.toLong())
                }
            }
            FarmingProfitTracker.markActivity()
        }
    }

    private fun checkCropFeverStart(message: String) {
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.CROP_FEVER)) return
        if (!CropFeverTracker.isCropFeverStartMessage(message)) return
        val crop = GardenApi.getCurrentlyFarmedCrop() ?: GardenApi.lastBrokenCropType ?: return
        FarmingProfitTracker.modify {
            it.cropFevers.addOrPut(crop, 1)
        }
        FarmingProfitTracker.markActivity()
    }

    private fun checkCropFeverDrop(message: String) {
        CropFeverTracker.rngDrop.matchMatcher(message) {
            val rarity = RngDropEnum.getByNameOrNull(group("rarity")) ?: return
            val amount = group("amount").formatInt()
            val internalName = NeuInternalName.fromItemNameOrNull(group("crop")) ?: return
            rememberSpecialCropItem(internalName, amount.toLong())
            if (!FarmingProfitTracker.shouldTrack(TrackedSource.CROP_FEVER)) return@matchMatcher
            val crop = GardenApi.getCurrentlyFarmedCrop() ?: run {
                val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
                CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor)
            } ?: return

            FarmingProfitTracker.addTrackedItem(TrackedSource.CROP_FEVER, internalName, amount.toLong())
            val primitiveStack = NeuItems.getPrimitiveMultiplier(internalName)
            FarmingProfitTracker.modify {
                it.cropFeverDrops.addOrPut(rarity, 1)
                it.addCropAmount(crop, TrackedSource.CROP_FEVER, primitiveStack.amount * amount.toLong())
            }
            FarmingProfitTracker.markActivity()
        }
    }

    private fun checkToolExpCapsule(message: String) {
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.CROPS)) return
        if (!HoeLevelDisplay.levelUpPattern.matches(message)) return
        FarmingProfitTracker.addTrackedItem(TrackedSource.CROPS, toolExpCapsule, 1L)
        FarmingProfitTracker.modify {
            it.toolExpCapsules++
        }
        FarmingProfitTracker.markActivity()
    }

    private fun trackCropAmount(crop: CropType, source: TrackedSource, amount: Long) {
        if (amount <= 0) return
        clearSessionStateIfSessionWasReset()
        val internalName = crop.getCropInternalName()
        FarmingProfitTracker.addTrackedItem(source, internalName, amount, message = false)
        if (source == TrackedSource.CROPS) applyPendingReplenishCost(crop)
        FarmingProfitTracker.modify {
            it.addCropAmount(crop, source, amount)
        }
        FarmingProfitTracker.markActivity()
    }

    private fun queueReplenishCost(event: CropClickEvent) {
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.CROPS)) return
        val crop = event.crop
        if (!crop.replenish) return
        if (event.itemInHand?.getHypixelEnchantments()?.containsKeys("replenish") != true) return
        pendingReplenishCosts.addOrPut(crop, crop.multiplier.toLong())
    }

    private fun applyPendingReplenishCost(crop: CropType) {
        val amount = pendingReplenishCosts.remove(crop) ?: return
        removeTrackedRawCrop(crop.getCropInternalName(), amount)
    }

    private fun trackCropItemFromItemAdd(event: ItemAddEvent) {
        if (!FarmingProfitTracker.shouldTrack(TrackedSource.CROPS) || event.amount <= 0) return
        if (FarmingProfitTracker.lastFarmingActivity.passedSince() > sackCompactionTimeout) return
        clearSessionStateIfSessionWasReset()

        val cropItem = event.internalName.getCropPrimitiveItemOrNull() ?: return
        val currentCrop = GardenApi.getCurrentlyFarmedCrop() ?: GardenApi.lastBrokenCropType ?: return
        if (cropItem.crop != currentCrop || cropItem.rawAmount <= 1) return
        if (cropItem.rawInternalName == event.internalName) {
            FarmingProfitTracker.markActivity()
            return
        }

        val consumedSpecial = consumeRecentSpecialCropItem(event.internalName, event.amount.toLong())
        val amount = event.amount.toLong() - consumedSpecial
        if (amount > 0) {
            removeTrackedRawCrop(cropItem.rawInternalName, amount * cropItem.rawAmount)
            FarmingProfitTracker.addTrackedItem(TrackedSource.CROPS, event.internalName, amount, message = false)
            FarmingProfitTracker.markActivity()
        }
    }

    private fun NeuInternalName.getCropPrimitiveItemOrNull(): CropPrimitiveItem? {
        val primitiveStack = NeuItems.getPrimitiveMultiplier(this)
        val crop = CropType.getByNameOrNull(primitiveStack.internalName.itemNameWithoutColor) ?: return null
        return CropPrimitiveItem(crop, primitiveStack.internalName, primitiveStack.amount.toLong())
    }

    private fun consumeRecentSpecialCropItem(internalName: NeuInternalName, amount: Long): Long {
        val stored = recentSpecialCropItems[internalName] ?: return 0L
        val consumed = minOf(stored, amount)
        val remaining = stored - consumed
        if (remaining > 0) recentSpecialCropItems[internalName] = remaining else recentSpecialCropItems.remove(internalName)
        return consumed
    }

    private fun removeTrackedRawCrop(rawInternalName: NeuInternalName, amount: Long) {
        if (amount <= 0) return
        FarmingProfitTracker.modify { data ->
            val rawAmount = minOf(amount, data.getTrackedCropItemAmount(rawInternalName))
            if (rawAmount <= 0) return@modify
            var remaining = -rawAmount
            while (remaining != 0L) {
                val chunk = remaining.coerceIn(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()).toInt()
                data.addItem(TrackedSource.CROPS, rawInternalName, chunk, command = true)
                remaining -= chunk
            }
        }
    }

    private fun FarmingProfitTrackerData.getTrackedCropItemAmount(internalName: NeuInternalName): Long =
        bucketedItems[TrackedSource.CROPS]?.get(internalName)?.totalAmount?.coerceAtLeast(0L) ?: 0L

    private fun clearSessionStateIfSessionWasReset() {
        if (pendingReplenishCosts.isEmpty() && recentSpecialCropItems.isEmpty()) return
        if (FarmingProfitTracker.sessionDataHasNoFarmingData()) {
            pendingReplenishCosts.clear()
            recentSpecialCropItems.clear()
        }
    }

    private fun CropType.getCropInternalName(): NeuInternalName = cropInternalNames.getOrPut(this) {
        val itemName = if (this == CropType.MUSHROOM) "Red Mushroom" else cropName
        NeuInternalName.fromItemName(itemName)
    }

    private fun CropCollectionType.toTrackedSource(): TrackedSource? = when (this) {
        CropCollectionType.BREAKING_CROPS -> TrackedSource.CROPS
        CropCollectionType.MOOSHROOM_COW -> TrackedSource.MOOSHROOM_COW
        CropCollectionType.GREENHOUSE,
        CropCollectionType.CROP_FEVER,
        CropCollectionType.PEST_BASE,
        CropCollectionType.PEST_RNG,
        CropCollectionType.UNKNOWN,
        -> null
    }
}
