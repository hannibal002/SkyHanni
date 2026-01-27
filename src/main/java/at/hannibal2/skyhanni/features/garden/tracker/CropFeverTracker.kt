package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.features.garden.CropFeverTrackerConfig.CropFeverTrackerTextEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.garden.CropCollectionApi.addCollectionCounter
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.farming.CropClickEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniTickEvent
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.tracker.CropFeverTracker.drawDisplay
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PrimitiveItemStack.Companion.makePrimitiveStack
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.Stopwatch
import at.hannibal2.skyhanni.utils.TimeUtils.format
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
        // used to avoid double counting fevers in total bucket if crops are swapped mid-fever
        @Expose var partialFeverAmount: MutableMap<CropType, Long> = EnumMap(CropType::class.java),
        @Expose var cropFeverDuration: MutableMap<CropType, Stopwatch> = EnumMap(CropType::class.java),
        @Expose var rngDrops: MutableMap<CropType, MutableMap<RngDropEnum, Long>> = EnumMap(CropType::class.java),
    ) : BucketedItemTrackerData<CropType>(CropType::class) {
        override fun getDescription(bucket: CropType?, timesGained: Long): List<String> {
            val blocksBroken = blocksBrokenDuring[bucket] ?: getTotalDuringCount()
            val dropRate = if (timesGained == 0L) 0 else blocksBroken.div(timesGained)
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
        fun getTotalFeverDuration(): Duration = cropFeverDuration.values.fold(Duration.ZERO) { acc, stopwatch ->
            acc + stopwatch.getDuration()
        }
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

    /**
     * REGEX-TEST: WOAH! You caught a case of the CROP FEVER for 60 seconds!
     */
    private val cropFeverStart by patternGroup.pattern(
        "start",
        "^WOAH! You caught a case of the CROP FEVER for 60 seconds!",
    )

    /**
     * REGEX-TEST: GONE! Your CROP FEVER has been cured!
     */
    private val cropFeverEnd by patternGroup.pattern(
        "end",
        "^GONE! Your CROP FEVER has been cured!",
    )

    private val config get() = GardenApi.config.cropFeverTracker
    private val blocksBrokenCache: MutableMap<CropType, Long> = EnumMap(CropType::class.java)
    private var isCropFever = false
    private var cropFeverCurrentCrop: CropType? = null

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage
        cropFeverStart.matchMatcher(message) {
            startCropFever()
        }
        cropFeverEnd.matchMatcher(message) {
            stopCropFever()
        }
        if (isCropFever) {
            rngDrop.matchMatcher(message) {
                val rarity = RngDropEnum.getByNameOrNull(group("rarity")) ?: return
                val amount = group("amount").formatInt()
                val crop = NeuInternalName.fromItemNameOrNull(group("crop")) ?: return

                val currentFarmedCrop =
                    GardenApi.getCurrentlyFarmedCrop() ?: CropType.getByNameOrNull(crop.makePrimitiveStack().itemName) ?: return

                addItem(currentFarmedCrop, crop, amount, false)

                modify { it.rngDrops.getOrPut(currentFarmedCrop) { mutableMapOf() }.addOrPut(rarity, 1) }

                val primitiveStack = NeuItems.getPrimitiveMultiplier(crop)

                currentFarmedCrop.addCollectionCounter(CropCollectionType.CROP_FEVER, primitiveStack.amount * amount.toLong())
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onCropBreak(event: CropClickEvent) {
        blocksBrokenCache.addOrPut(event.crop, 1)
        // multi crop support if people farm multiple crops during 1 crop fever
        if (isCropFever) {
            if (cropFeverCurrentCrop != event.crop) {
                val oldCrop = cropFeverCurrentCrop
                cropFeverCurrentCrop = event.crop
                modify {
                    it.cropFeverDuration[oldCrop]?.pause()
                }
                startCropFever(true)
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick(event: SkyHanniTickEvent) {
        if (!event.isMod(5) || blocksBrokenCache.isEmpty()) return

        val iterator = blocksBrokenCache.entries.iterator()
        while (iterator.hasNext()) {
            val (key, value) = iterator.next()
            modify { data ->
                if (isCropFever) {
                    data.blocksBrokenDuring.addOrPut(key, value)
                } else {
                    data.blocksBrokenOutside.addOrPut(key, value)
                }
            }

            iterator.remove()
        }
    }

    @HandleEvent
    fun onWorldChange(event: WorldChangeEvent) {
        update()
        if (!isCropFever) return
        stopCropFever()
    }

    @HandleEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        ConditionalUtils.onToggle(config.text) {
            update()
        }
    }

    init {
        initRenderer({ config.position }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean =
        config.enabled &&
            GardenApi.inGarden() &&
            (!config.onlyWithTool || GardenApi.isHoldingCropFever()) &&
            (!config.onlyDuringFever || isCropFever)

    private fun startCropFever(partialFever: Boolean = false) {
        isCropFever = true
        val currentCrop = GardenApi.getCurrentlyFarmedCrop() ?: return
        modify {
            if (!partialFever) it.cropFeverAmount.addOrPut(currentCrop, 1)
            else it.partialFeverAmount.addOrPut(currentCrop, 1)

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
        val bucketName = bucketData.selectedBucket?.cropName ?: "Total"
        val feverAmount: Long = if (bucketData.selectedBucket == null) {
            bucketData.getTotalFeverCount()
        } else {
            val cropFeverAmount = bucketData.cropFeverAmount[bucketData.selectedBucket] ?: 0
            val partialFeverAmount = bucketData.partialFeverAmount[bucketData.selectedBucket] ?: 0
            cropFeverAmount + partialFeverAmount
        }

        val blocksOutside = if (bucketData.selectedBucket == null) {
            bucketData.getTotalOutsideCount()
        } else {
            bucketData.blocksBrokenOutside[bucketData.selectedBucket]
        }

        val breaksPerFever: Long = if (feverAmount == 0L) {
            0L
        } else {
            (blocksOutside ?: 0L) / feverAmount
        }

        lineMap[CropFeverTrackerTextEntry.FEVER_AMOUNT] =
            Renderable.hoverTips(
                Renderable.text("§7$bucketName Crop Fevers: §e${feverAmount.addSeparators()}"),
                tips = listOf(
                    Renderable.text("§7Average Breaks per Fever: §e${breaksPerFever.addSeparators()}"),
                ),
            ).toSearchable()

        val feverUptime: Duration = bucketData.getTotalFeverDuration()
        lineMap[CropFeverTrackerTextEntry.FEVER_DURATION] =
            Renderable.text("§7Crop Fever Duration: §b${feverUptime.format()}").toSearchable()

        val (totalDuring, totalOutside) =
            if (bucketData.selectedBucket == null) {
                bucketData.getTotalDuringCount() to bucketData.getTotalOutsideCount()
            } else {
                val bucket = bucketData.selectedBucket
                (bucketData.blocksBrokenDuring[bucket] ?: 0) to (bucketData.blocksBrokenOutside[bucket] ?: 0)
            }
        val totalBlocks = totalDuring + totalOutside
        lineMap[CropFeverTrackerTextEntry.TOTAL_BLOCKS] = Renderable.hoverTips(
            Renderable.text("§7$bucketName Crops Broken: §e${totalBlocks.addSeparators()}"),
            tips = listOf(
                Renderable.text("§7During Crop Fever: §e${totalDuring.addSeparators()}"),
                Renderable.text("§7Outside of Crop Fever: §e${totalOutside.addSeparators()}"),
            ),
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
            val drops = rngMap[it] ?: 0
            val blocksBroken = if (data.selectedBucket == null) data.getTotalDuringCount()
            else data.blocksBrokenDuring[data.selectedBucket] ?: 0
            val breaksPerDrop = if (drops == 0L) 0 else blocksBroken / drops
            add(
                Renderable.hoverTips(
                    Renderable.text("§7- §e${drops}x $it"),
                    tips = listOf(Renderable.text("§7Block Breaks per Drop: ${breaksPerDrop.addSeparators()}")),
                ).toSearchable(),
            )
        }
    }

    private fun formatDisplay(
        lineMap: MutableMap<CropFeverTrackerTextEntry, Searchable>,
        bucketData: BucketData,
    ): List<Searchable> = buildList {
        val rngDropList: List<Searchable> = buildRngDropList(bucketData)
        addSearchString("§e§lCrop Fever Tracker")
        addBucketSelector(this, bucketData, "Crop Type")
        val profit = drawItems(bucketData, { true }, mutableListOf())
        config.text.get().forEach { line ->
            when (line) {
                CropFeverTrackerTextEntry.ITEM_DROPS -> { drawItems(bucketData, { true }, this) }
                CropFeverTrackerTextEntry.RNG_DROPS -> { addAll(rngDropList) }
                CropFeverTrackerTextEntry.TOTAL_PROFIT -> {
                    val duration = bucketData.getTotalUptime()
                    addAll(
                        addTotalProfit(profit, bucketData.getTotalFeverCount(), "fever", duration, "Fevers"),
                    )
                }
                else -> { lineMap[line]?.let { add(it) } }
            }
        }
        addPriceFromButton(this)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetcropfevertracker") {
            aliases = listOf("shresetcft")
            description = "Resets the Crop Fever Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { resetCommand() }
        }
    }
}
