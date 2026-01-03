package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.garden.CropFeverTrackerConfig.CropFeverTrackerTextEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.tracker.CropFeverTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import java.util.EnumMap
import kotlin.time.Duration

@SkyHanniModule
object CropFeverTracker : SkyHanniBucketedItemTracker<CropType, CropFeverTracker.BucketData>(
    "Crop Fever Tracker",
    ::BucketData,
    { it.garden.cropFeverTracker },
    drawDisplay = { drawDisplay(it) },
) {
    data class BucketData(
        @Expose var blocksBrokenDuring: MutableMap<CropType, Long> = EnumMap(CropType::class.java),
        @Expose var blocksBrokenOutside: MutableMap<CropType, Long> = EnumMap(CropType::class.java),
        @Expose var cropFeverAmount: MutableMap<CropType, Long> = EnumMap(CropType::class.java),
        @Expose var cropFeverDuration: MutableMap<CropType, Stopwatch> = EnumMap(CropType::class.java),
        @Expose var rngDrops: MutableMap<CropType, MutableMap<RngDropEnum, Long>> = EnumMap(CropType::class.java),
    ) : BucketedItemTrackerData<CropType>(CropType::class) {
        override fun getDescription(bucket: CropType?, timesGained: Long): List<String> {
            val dropRate = if (timesGained == 0L) 0 else blocksBrokenDuring[bucket]?.div(timesGained) ?: 0
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Average Blocks Broken Per Drop: §c$dropRate.",
            )
        }

        override fun getCoinName(bucket: CropType?, item: TrackedItem) = "§6Crop Fever Coins"

        override fun getCoinDescription(bucket: CropType?, item: TrackedItem): List<String> {
            return listOf(
                "§7You somehow gained coins from crop fever. Good job.",
            )
        }

        override fun CropType.isBucketSelectable() = this in CropType.entries

        override fun bucketName(): String {
            return "Crop"
        }

        fun getTotalFeverCount(): Long = cropFeverAmount.values.sum()
        fun getTotalDuringCount(): Long = blocksBrokenDuring.values.sum()
        fun getTotalOutsideCount(): Long = blocksBrokenOutside.values.sum()
        fun getTotalDuration(): Duration = cropFeverDuration.values.fold(Duration.ZERO) { acc, stopwatch ->
            acc + stopwatch.getDuration()}
    }

    private val patternGroup = RepoPattern.group("garden.cropfever")
    /**
     * REGEX-TEST: RARE DROP! You dropped 48x Enchanted Melon Slice!
     * REGEX-TEST: UNCOMMON DROP! You dropped 24x Enchanted Melon Slice!
     */
    private val rngDrop by patternGroup.pattern(
        "drop",
        "^(?<rarity>[\\w ]+)! You dropped (?<amount>\\d+)x (?<crop>[\\w ]+)!",
    )

    private val cropFeverStart by patternGroup.pattern(
        "start",
        "^WOAH! You caught a case of the CROP FEVER for 60 seconds!"
    )

    private val cropFeverEnd by patternGroup.pattern(
        "end",
        "^GONE! Your CROP FEVER has been cured!"
    )

    private val config get() = GardenApi.config.cropFeverTracker
    private val blocksBrokenCache: MutableMap<CropType, Long> = EnumMap(CropType::class.java)
    private var isCropFever = false
    private var cropFeverCurrentCrop: CropType? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        cropFeverStart.matchMatcher(event.chatComponent.string) {
            startCropFever()
        }
        cropFeverEnd.matchMatcher(event.chatComponent.string) {
            stopCropFever()
        }
        if (isCropFever) {
            rngDrop.matchMatcher(event.chatComponent.string) {
                val rarity = RngDropEnum.getByNameOrNull(group("rarity")) ?: return
                val amount = group("amount").formatInt()
                val crop = NeuInternalName.fromItemNameOrNull(group("crop")) ?: return

                val currentFarmedCrop = GardenApi.getCurrentlyFarmedCrop() ?:
                CropType.getByNameOrNull(crop.makePrimitiveStack().itemName) ?:
                return

                addItem(currentFarmedCrop, crop, amount, false)

                modify { it.rngDrops.getOrPut(currentFarmedCrop) { mutableMapOf() }.addOrPut(rarity, 1) }
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onCropBreak(event: CropClickEvent) {
        blocksBrokenCache.addOrPut(event.crop, 1)
        if (isCropFever) {
            if (cropFeverCurrentCrop != event.crop) {
                val oldCrop = cropFeverCurrentCrop
                cropFeverCurrentCrop = event.crop
                modify {
                    it.cropFeverDuration[oldCrop]?.pause()
                }
                startCropFever()
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5)) return
        blocksBrokenCache.forEach { cache ->
            modify {data ->
                if (isCropFever) {
                    data.blocksBrokenDuring.addOrPut(cache.key, cache.value)
                } else {
                    data.blocksBrokenOutside.addOrPut(cache.key, cache.value)
                }
                blocksBrokenCache[cache.key] = 0
            }
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        if (!isCropFever) return
        stopCropFever()
    }

    init {
        initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean = config.enabled

    private fun startCropFever(addToTracker: Boolean = true) {
        isCropFever = true
        val currentCrop = GardenApi.getCurrentlyFarmedCrop() ?: return
        modify {
            if (addToTracker) it.cropFeverAmount.addOrPut(currentCrop, 1)
            val stopwatch = it.cropFeverDuration.getOrPut(currentCrop) { Stopwatch() }
            stopwatch.start()
        }
    }

    private fun stopCropFever() {
        isCropFever = false
        modify {
            it.cropFeverDuration.forEach { crop ->
                crop.value.pause()
            }

        }
    }

    private fun drawDisplay(bucketData: BucketData): List<Searchable> {
        val lineMap: MutableMap<CropFeverTrackerTextEntry, Searchable> = EnumMap(CropFeverTrackerTextEntry::class.java)
        val feverAmount = if (bucketData.selectedBucket == null) {
            bucketData.getTotalFeverCount()
        } else {
            bucketData.cropFeverAmount[bucketData.selectedBucket] ?: 0
        }
        lineMap[CropFeverTrackerTextEntry.FEVER_AMOUNT] = Renderable.text("§7Total Crop Fevers: §e${feverAmount.addSeparators()}").toSearchable()

        val (totalDuring, totalOutside) =
            if (bucketData.selectedBucket == null) {
                bucketData.getTotalDuringCount() to bucketData.getTotalOutsideCount()
            } else {
                val bucket = bucketData.selectedBucket
                (bucketData.blocksBrokenDuring[bucket] ?: 0) to (bucketData.blocksBrokenOutside[bucket] ?: 0)
            }
        val totalBlocks = totalDuring + totalOutside
        lineMap[CropFeverTrackerTextEntry.TOTAL_BLOCKS] = Renderable.hoverTips(
            Renderable.text("§7Total Blocks Broken: §e${totalBlocks.addSeparators()}"),
            tips = listOf(
                Renderable.text("§7During Crop Fever: §e${totalDuring.addSeparators()}"),
                Renderable.text("§7Outside of Crop Fever: §e${totalOutside.addSeparators()}")
                )
        ).toSearchable()

        lineMap[CropFeverTrackerTextEntry.SPACER_1] = Renderable.placeholder(10).toSearchable()
        lineMap[CropFeverTrackerTextEntry.SPACER_2] = Renderable.placeholder(10).toSearchable()

        return formatDisplay(lineMap, bucketData)
    }

    private fun buildRngDropList(data: BucketData): List<Searchable> = buildList {
        val rngMap: MutableMap<RngDropEnum, Long> = if (data.selectedBucket == null) {
            data.rngDrops.values
                .flatMap { it.entries }
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, values) -> values.sum() }
                .toMutableMap()
        } else {
            data.rngDrops[data.selectedBucket] ?: mutableMapOf()
        }

        RngDropEnum.entries.forEach {
            val drops = rngMap[it]
            add(Renderable.text("§7- §e${drops ?: 0}x $it").toSearchable())
        }
    }
    private fun formatDisplay(
        lineMap: MutableMap<CropFeverTrackerTextEntry, Searchable>,
        bucketData: BucketData
    ): List<Searchable> = buildList {
        val rngDropList: List<Searchable> = buildRngDropList(bucketData)
        addSearchString("§e§lCrop Fever Tracker")
        addBucketSelector(this, bucketData, "Crop Type")
        val profit = drawItems(bucketData, { true }, mutableListOf())
        config.text.forEach{ line ->
            if (line == CropFeverTrackerTextEntry.ITEM_DROPS) {
                drawItems(bucketData, { true }, this)
            } else if (line == CropFeverTrackerTextEntry.RNG_DROPS) {
                addAll(rngDropList)
            } else if (line == CropFeverTrackerTextEntry.TOTAL_PROFIT) {
                val duration = bucketData.getTotalUptime()
                addAll(addTotalProfit(profit, bucketData.getTotalFeverCount(), "drop", duration, "Drops"))
            } else {
                lineMap[line]?.let { add(it) }
            }
        }
        addPriceFromButton(this)
    }

}
