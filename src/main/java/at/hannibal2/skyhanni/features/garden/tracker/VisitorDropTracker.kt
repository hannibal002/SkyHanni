package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.visitor.DropsStatisticsConfig.DropsStatisticsTextEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker.addItemFromEvent
import at.hannibal2.skyhanni.features.garden.tracker.VisitorDropTracker.drawDisplay
import at.hannibal2.skyhanni.features.garden.visitor.VisitorApi
import at.hannibal2.skyhanni.features.garden.visitor.VisitorRarity
import at.hannibal2.skyhanni.features.garden.visitor.VisitorReward
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.add
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTimedBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import com.google.gson.annotations.Expose
import java.util.EnumMap
import java.util.regex.Pattern
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object VisitorDropTracker : SkyHanniTimedBucketedItemTracker<VisitorRarity, VisitorDropTracker.BucketData>(
    "Visitor Drop Tracker",
    { BucketData() },
    { it.garden.visitorDropTracker },
    drawDisplay = { drawDisplay(it) },
    trackerConfig = { SkyHanniMod.feature.garden.visitors.dropsStatistics.perTrackerConfig },
    customUptimeControl = true
) {
    private val patternGroup = RepoPattern.group("garden.visitor.droptracker")
    val config get() = VisitorApi.config.dropsStatistics
    private var lastAccept = SimpleTimeMark.farPast()
    private var lastVisitorRarity: VisitorRarity? = null

    /**
     * REGEX-TEST: OFFER ACCEPTED with Duke (UNCOMMON)
     */
    private val acceptPattern by patternGroup.pattern(
        "accept",
        "OFFER ACCEPTED with (?<visitor>.*) \\((?<rarity>.*)\\)",
    )

    /**
     * REGEX-TEST: +20 Copper
     */
    private val copperPattern by patternGroup.pattern(
        "copper",
        "[+](?<amount>.*) Copper",
    )

    /**
     * REGEX-TEST: +20 Garden Experience
     */
    private val gardenExpPattern by patternGroup.pattern(
        "gardenexp",
        "[+](?<amount>.*) Garden Experience",
    )

    /**
     * REGEX-TEST: +18.2k Farming XP
     */
    private val farmingExpPattern by patternGroup.pattern(
        "farmingexp",
        "[+](?<amount>.*) Farming XP",
    )

    /**
     * REGEX-TEST: +12 Bits
     */
    private val bitsPattern by patternGroup.pattern(
        "bits",
        "[+](?<amount>.*) Bits",
    )

    /**
     * REGEX-TEST: +968 Mithril Powder
     */
    private val mithrilPowderPattern by patternGroup.pattern(
        "powder.mithril",
        "[+](?<amount>.*) Mithril Powder",
    )

    /**
     * REGEX-TEST: +754 Gemstone Powder
     */
    private val gemstonePowderPattern by patternGroup.pattern(
        "powder.gemstone",
        "[+](?<amount>.*) Gemstone Powder",
    )

    private val patternStorageAccessorMap: Map<Pattern, (Int, VisitorRarity) -> Unit> = mapOf(
        copperPattern to { amount, rarity -> modify { it.copper[rarity] = (it.copper[rarity] ?: 0) + amount } },
        farmingExpPattern to { amount, rarity -> modify { it.farmingXp[rarity] = (it.copper[rarity] ?: 0) + amount } },
        gardenExpPattern to { amount, rarity -> modify { it.gardenXp[rarity] = (it.copper[rarity] ?: 0) + amount } },
        bitsPattern to { amount, rarity -> modify { it.bits[rarity] = (it.copper[rarity] ?: 0) + amount } },
        mithrilPowderPattern to { amount, rarity -> modify { it.mithrilPowder[rarity] = (it.copper[rarity] ?: 0) + amount } },
        gemstonePowderPattern to { amount, rarity -> modify { it.gemstonePowder[rarity] = (it.copper[rarity] ?: 0) + amount } },
    )

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onVisitorAccepted(event: VisitorAcceptedEvent) {
        lastAccept = SimpleTimeMark.now()
        lastVisitorRarity = getRarityFromVisitorName(event.visitor.visitorName)
    }

    @HandleEvent
    fun onVisitorAccept(event: VisitorAcceptEvent) {
        if (!GardenApi.onBarnPlot) return
        if (!ProfileStorageData.loaded) return

        val rarity = getRarityFromVisitorName(event.visitor.visitorName) ?: return
        for (internalName in event.visitor.allRewards) {
            addItem(rarity, internalName, 1, false)
        }
        modify { it.coinsSpent[rarity] = (it.coinsSpent[rarity] ?: 0) + round(event.price).toLong() }
    }

    @HandleEvent
    fun onVisitorRefused(event: VisitorRefusedEvent) {
        val rarity = getRarityFromVisitorName(event.visitor.visitorName) ?: return
        modify { it.visitorsRejected[rarity] = (it.visitorsRejected[rarity] ?: 0) + 1 }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        if (!GardenApi.onBarnPlot) return
        if (!ProfileStorageData.loaded) return
        if (lastAccept.passedSince() > 1.seconds) return

        val message = event.message.removeColor().trim()

        patternStorageAccessorMap.forEach { (pattern, accessor) ->
            pattern.matchMatcher(message) {
                val amount = group("amount").formatInt()
                val rarity = lastVisitorRarity ?: VisitorRarity.UNKNOWN
                accessor.invoke(amount, rarity)
            }
        }

        acceptPattern.matchMatcher(message) {
            val rarity = VisitorRarity.getByNameOrNull(group("rarity")) ?: return@matchMatcher
            lastVisitorRarity = rarity
            modify{ it.visitorsAccepted[rarity] = (it.visitorsAccepted[rarity] ?: 0) + 1 }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onItemAdd(event: ItemAddEvent) {
        if (config.enabled.get() && event.source == ItemAddManager.Source.COMMAND) {
            event.addItemFromEvent()
        }
    }

    private fun getRarityFromVisitorName(name: String): VisitorRarity? {
        val charArray = name.toCharArray()
        // names should start with color code, but just in case they don't
        if (charArray[0] != '§') return null
        return VisitorRarity.getFromColorCode(charArray[1])
    }

    class TimeData : TimedTrackerData<BucketData>({ BucketData() })

    data class BucketData(
        @Expose val visitorsAccepted: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose val visitorsRejected: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose val farmingXp: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose val gardenXp: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose val coinsSpent: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose val bits: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose val mithrilPowder: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose val gemstonePowder: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        @Expose var copper: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
        // old visitor tracker tracked visitors accepted by rarity but without any of the other relevant data
        @Expose var legacyVisitorRarity: MutableMap<VisitorRarity, Long> = EnumMap(VisitorRarity::class.java),
    ) : BucketedItemTrackerData<VisitorRarity, SessionUptime.Garden>(VisitorRarity::class, SessionUptime.Garden::class) {
        override fun getDescription(bucket: VisitorRarity?, timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / getTotalVisitorCount()
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun VisitorRarity.isBucketSelectable() = this in VisitorRarity.filterableEntries

        override fun getCoinName(bucket: VisitorRarity?, item: TrackedItem) = "§6Visitor Coins"

        override fun getCoinDescription(bucket: VisitorRarity?, item: TrackedItem): List<String> {
            val visitorCoinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7If you're seeing this something went wrong.",
                "§7You somehow got §6$visitorCoinsFormat coins §7from visitors.",
            )
        }

        override fun bucketName(): String {
            return "Visitor Rarity"
        }

        fun getTotalVisitorCount(): Long =
            if (selectedBucket != null) (visitorsAccepted[selectedBucket] ?: 0L) + (visitorsRejected[selectedBucket] ?: 0L)
            else visitorsAccepted.entries.sumOf { it.value } + visitorsRejected.entries.sumOf { it.value }

        fun getVisitorsAccepted(bucket: VisitorRarity? = selectedBucket, includeLegacy: Boolean = false): Long {
            val fromBucketData = if (bucket != null) visitorsAccepted[bucket] ?: 0L
            else visitorsRejected.entries.sumOf { it.value }

            return if (includeLegacy && bucket != null) fromBucketData + (legacyVisitorRarity[bucket] ?: 0) else fromBucketData
        }

        fun getVisitorsRejected(): Long =
            if (selectedBucket != null) visitorsRejected[selectedBucket] ?: 0L
            else visitorsRejected.entries.sumOf { it.value }

        fun getFarmingXp(): Long =
            if (selectedBucket != null) farmingXp[selectedBucket] ?: 0L
            else farmingXp.entries.sumOf { it.value }

        fun getGardenXp(): Long =
            if (selectedBucket != null) gardenXp[selectedBucket] ?: 0L
            else gardenXp.entries.sumOf { it.value }

        fun getCoinsSpent(): Long =
            if (selectedBucket != null) coinsSpent[selectedBucket] ?: 0L
            else coinsSpent.entries.sumOf { it.value }

        fun getBits(): Long =
            if (selectedBucket != null) bits[selectedBucket] ?: 0L
            else bits.entries.sumOf { it.value }

        fun getMithrilPowder(): Long =
            if (selectedBucket != null) mithrilPowder[selectedBucket] ?: 0L
            else mithrilPowder.entries.sumOf { it.value }

        fun getGemstonePowder(): Long =
            if (selectedBucket != null) gemstonePowder[selectedBucket] ?: 0L
            else gemstonePowder.entries.sumOf { it.value }

        fun getCopper(): Long =
            if (selectedBucket != null) copper[selectedBucket] ?: 0L
            else copper.entries.sumOf { it.value }
    }

    fun drawDisplay(bucketData: BucketData): List<Searchable> {
        val displayMap: MutableMap<DropsStatisticsTextEntry, Searchable> = mutableMapOf()
        val selectedBucket = bucketData.selectedBucket
        val itemList = mutableListOf<Searchable>()
        val profit = drawItems(bucketData, { true }, itemList)

        displayMap[DropsStatisticsTextEntry.TITLE] = Renderable.text("§e§lVisitor Statistics").toSearchable()
        displayMap[DropsStatisticsTextEntry.PROFIT_LIST] = Renderable.placeholder(0).toSearchable()
        displayMap[DropsStatisticsTextEntry.SPACER_1] = Renderable.placeholder(10).toSearchable()
        displayMap[DropsStatisticsTextEntry.SPACER_2] = Renderable.placeholder(10).toSearchable()

        fun visitorByRarity(): String {
            val includeLegacy = selectedBucket == null
            val uncommon =
                "§a${if (selectedBucket == VisitorRarity.UNCOMMON) "§l" else ""}" +
                    "${bucketData.getVisitorsAccepted(VisitorRarity.UNCOMMON, includeLegacy).addSeparators()}§r"
            val rare =
                "§9${if (selectedBucket == VisitorRarity.RARE) "§l" else ""}" +
                    "${bucketData.getVisitorsAccepted(VisitorRarity.RARE, includeLegacy).addSeparators()}§r"
            val legendary =
                "§6${if (selectedBucket == VisitorRarity.LEGENDARY) "§l" else ""}" +
                    "${bucketData.getVisitorsAccepted(VisitorRarity.LEGENDARY, includeLegacy).addSeparators()}§r"
            val mythic =
                "§d${if (selectedBucket == VisitorRarity.MYTHIC) "§l" else ""}" +
                    "${bucketData.getVisitorsAccepted(VisitorRarity.MYTHIC, includeLegacy).addSeparators()}§r"
            val special =
                "§c${if (selectedBucket == VisitorRarity.SPECIAL) "§l" else ""}" +
                    "${bucketData.getVisitorsAccepted(VisitorRarity.SPECIAL, includeLegacy).addSeparators()}§r"

            val rarityList = listOf(uncommon, rare, legendary, mythic, special)
            return rarityList.joinToString(separator = "-")
        }
        displayMap[DropsStatisticsTextEntry.VISITORS_BY_RARITY] = Renderable.text(visitorByRarity()).toSearchable()

        displayMap[DropsStatisticsTextEntry.TOTAL_VISITORS] =
            Renderable.text(format(bucketData.getTotalVisitorCount(), "Total", "§e", "")).toSearchable()

        displayMap[DropsStatisticsTextEntry.ACCEPTED] =
            Renderable.text(format(bucketData.getVisitorsAccepted(), "Accepted", "§2", "")).toSearchable()

        displayMap[DropsStatisticsTextEntry.DENIED] =
            Renderable.text(format(bucketData.getVisitorsRejected(), "Denied", "§c", "")).toSearchable()

        displayMap[DropsStatisticsTextEntry.COPPER] =
            Renderable.text(format(bucketData.getCopper(), "Copper", "§c", "")).toSearchable()

        displayMap[DropsStatisticsTextEntry.FARMING_EXP] =
            Renderable.text(format(bucketData.getFarmingXp(), "Farming EXP", "§3", "§7")).toSearchable()

        displayMap[DropsStatisticsTextEntry.GARDEN_EXP] =
            Renderable.text(format(bucketData.getGardenXp(), "Garden EXP", "§2", "§7")).toSearchable()

        displayMap[DropsStatisticsTextEntry.COINS_SPENT] =
            Renderable.text(format(bucketData.getCoinsSpent(), "Coins Spent", "§6", "")).toSearchable()

        displayMap[DropsStatisticsTextEntry.BITS] =
            Renderable.text(format(bucketData.getBits(), "Bits", "§b", "§b")).toSearchable()

        displayMap[DropsStatisticsTextEntry.MITHRIL_POWDER] =
            Renderable.text(format(bucketData.getMithrilPowder(), "Mithril Powder", "§2", "§2")).toSearchable()

        displayMap[DropsStatisticsTextEntry.GEMSTONE_POWDER] =
            Renderable.text(format(bucketData.getGemstonePowder(), "Gemstone Powder", "§d", "§d")).toSearchable()

        return formatDisplay(displayMap, bucketData, profit)
    }

    fun formatDisplay(
        displayMap: Map<DropsStatisticsTextEntry, Searchable>,
        data: BucketData,
        profit: Double
    ): List<Searchable> {
        val newList = mutableListOf<Searchable>()
        addBucketSelector(newList, data, "Visitor Rarity")
        val sortedList = config.textFormat.get().mapNotNull { key -> displayMap[key]?.let { key to it } }

        for (line in sortedList) {
            if (line.first == DropsStatisticsTextEntry.PROFIT_LIST) {
                drawItems(data, { true }, newList)
            } else {
                newList.add(line.second)
            }
        }

        val duration = data.getTotalUptime()
        newList.addAll(addTotalProfit(profit, data.getTotalVisitorCount().toLong(), "visitor", duration, "Visitors"))

        PestProfitTracker.addPriceFromButton(newList)

        return newList
    }

    fun format(amount: Number, name: String, color: String, amountColor: String = color) =
        if (config.displayNumbersFirst.get())
            "$color${format(amount)} $name"
        else
            "$color$name: $amountColor${format(amount)}"

    fun format(amount: Number): String {
        if (amount is Int) return amount.addSeparators()
        if (amount is Long) return amount.shortFormat()
        return "$amount"
    }

    init {
        initRenderer({ config.pos }) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!config.enabled.get() || !GardenApi.inGarden()) return false
        if (GardenApi.hideExtraGuis()) return false
        if (config.onlyOnBarn.get() && !GardenApi.onBarnPlot) return false
        return true
    }

    private val visitorRarityEntries: List<LorenzRarity> = listOf(
        LorenzRarity.UNCOMMON,
        LorenzRarity.RARE,
        LorenzRarity.LEGENDARY,
        LorenzRarity.MYTHIC,
        LorenzRarity.SPECIAL,
    )

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        val originalPrefix = "garden.visitorDropsStatistics."
        val newPrefix = "garden.visitors.dropsStatistics."
        event.move(3, "${originalPrefix}enabled", "${newPrefix}enabled")
        event.move(3, "${originalPrefix}textFormat", "${newPrefix}textFormat")
        event.move(3, "${originalPrefix}displayNumbersFirst", "${newPrefix}displayNumbersFirst")
        event.move(3, "${originalPrefix}displayIcons", "${newPrefix}displayIcons")
        event.move(3, "${originalPrefix}onlyOnBarn", "${newPrefix}onlyOnBarn")
        event.move(3, "${originalPrefix}visitorDropPos", "${newPrefix}pos")

        // Was a list of longs, now a map of rarity to count
        event.move(
            85,
            "#profile.garden.visitorDrops.visitorRarities",
            "#profile.garden.visitorDrops.acceptedRarities",
        ) { element ->
            val list = element.asJsonArray.map { it.asLong }.toMutableList()

            // Adding the mythic rarity between legendary and special, if missing
            if (list.size == 4) {
                val special = list.last()
                list[3] = 0L
                list.add(special)
            }

            val map = mutableMapOf<LorenzRarity, Long>()
            for ((index, rarity) in visitorRarityEntries.withIndex()) {
                map[rarity] = list[index]
            }

            ConfigManager.gson.toJsonTree(map, MutableMap::class.java)
        }

        val path = "#profile.garden.visitorDrops"
        val timeData = TimeData()
        val bucketData = BucketData()
        event.transform(110, "$path.rewardsCount") { entry ->
            val itemData = ConfigManager.gson.fromJson<MutableMap<VisitorReward, Int>>(entry)
            itemData.entries.forEach { itemEntry ->
                val internalName = itemEntry.key.internalName
                val amount = itemEntry.value
                bucketData.addItem(VisitorRarity.UNKNOWN, internalName, amount, false)
            }
            entry
        }

        event.transform(110, "$path.acceptedVisitors") { entry ->
            bucketData.visitorsAccepted[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Int>(entry).toLong()
            entry
        }
        event.transform(110, "$path.deniedVisitors") { entry ->
            bucketData.visitorsRejected[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Int>(entry).toLong()
            entry
        }
        event.transform(110, "$path.acceptedRarities") { entry ->
            val oldMap = ConfigManager.gson.fromJson<MutableMap<LorenzRarity, Long>>(entry)
            val newMap = mutableMapOf<VisitorRarity, Long>()
            oldMap.entries.forEach { rarityEntry ->
                val visitorRarity = when (rarityEntry.key) {
                    LorenzRarity.UNCOMMON -> VisitorRarity.UNCOMMON
                    LorenzRarity.RARE -> VisitorRarity.RARE
                    LorenzRarity.LEGENDARY -> VisitorRarity.LEGENDARY
                    LorenzRarity.MYTHIC -> VisitorRarity.MYTHIC
                    LorenzRarity.SPECIAL -> VisitorRarity.SPECIAL
                    else -> VisitorRarity.UNKNOWN
                }
                newMap.add(visitorRarity to rarityEntry.value)
            }
            bucketData.legacyVisitorRarity = newMap
            entry
        }
        event.transform(110, "$path.copper") { entry ->
            bucketData.copper[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Long>(entry)
            entry
        }
        event.transform(110, "$path.farmingExp") { entry ->
            bucketData.farmingXp[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Long>(entry)
            entry
        }
        event.transform(110, "$path.gardenExp") { entry ->
            bucketData.gardenXp[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Long>(entry)
            entry
        }
        event.transform(110, "$path.coinsSpent") { entry ->
            bucketData.coinsSpent[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Long>(entry)
            entry
        }
        event.transform(110, "$path.bits") { entry ->
            bucketData.bits[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Long>(entry)
            entry
        }
        event.transform(110, "$path.mithrilPowder") { entry ->
            bucketData.mithrilPowder[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Long>(entry)
            entry
        }
        event.transform(110, "$path.gemstonePowder") { entry ->
            bucketData.gemstonePowder[VisitorRarity.UNKNOWN] = ConfigManager.gson.fromJson<Long>(entry)
            entry
        }
        event.add(110, "#profile.garden.visitorDropTracker") {
            timeData.createEntry(DisplayMode.TOTAL, "total", bucketData)
            ConfigManager.gson.toJsonTree(timeData)
        }
    }
}
