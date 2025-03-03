package at.hannibal2.skyhanni.features.combat.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.ItemAddManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.DragonProfitTrackerItemDataJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.DragonProfitTrackerItemsJson
import at.hannibal2.skyhanni.events.ItemAddEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import java.util.EnumMap

@SkyHanniModule
object DragonProfitTracker {
    private val config get() = SkyHanniMod.feature.combat.endIsland.dragonProfitTracker

    var dragonType: String? = null
    var lastPlaced: Int = 0

    private val tracker = SkyHanniBucketedItemTracker(
        "Dragon Profit Tracker",
        { BucketData() },
        { it.dragonProfitTracker },
        { drawDisplay(it) },
    )

    class BucketData : BucketedItemTrackerData<DragonType>() {
        override fun getCoinName(bucket: DragonType?, item: TrackedItem) = "<no coins>"
        override fun getCoinDescription(bucket: DragonType?, item: TrackedItem): List<String> = listOf("<no coins>")

        override fun DragonType.isBucketSelectable(): Boolean = this.selectable

        override fun resetItems() {
            dragonKills.clear()
            eyesPlaced = 0
        }

        override fun getDescription(bucket: DragonType?, timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / getTotalDragonCount()
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        fun getTotalDragonCount(): Long {
            return if (selectedBucket == null || selectedBucket !in DragonType.values()) {
                dragonKills.values.sum()
            } else {
                dragonKills[selectedBucket] ?: 0
            }
        }

        @Expose
        var dragonKills: MutableMap<DragonType, Long> = EnumMap(DragonType::class.java)

        @Expose
        var eyesPlaced: Long = 0
    }

    private fun drawDisplay(bucketData: BucketData): List<Searchable> = buildList {
        addSearchString("§b§lDragon Profit Tracker")
        tracker.addBucketSelector(this, bucketData, "Dragon Type")

        if (bucketData.getTotalDragonCount() == 0L) return@buildList

        var profit = tracker.drawItems(bucketData, { true }, this)

        var totalEyePrice = 0.0
        val eyePrice = NeuInternalName.fromItemNameOrNull("Summoning Eye")?.getPrice()
        if (eyePrice != null) {
            totalEyePrice = eyePrice * bucketData.eyesPlaced
            profit -= totalEyePrice
            val eyeFormat = "§7${bucketData.eyesPlaced}x §5Summoning Eye §c-${totalEyePrice.shortFormat()}"
            add(
                Renderable.string(eyeFormat).toSearchable()
            )
        }

        val colorCode = bucketData.selectedBucket?.colorCode ?: "§b"
        val displayName = bucketData.selectedBucket?.displayName ?: "Total Dragon"
        val killAmount = bucketData.getTotalDragonCount()
        val dragonString = "$colorCode$displayName §r§bkills: $killAmount"
        add(
            Renderable.string(dragonString).toSearchable()
        )

        add(tracker.addTotalProfit(profit, bucketData.getTotalDragonCount(), "Dragon"))

        tracker.addPriceFromButton(this)
    }

    var allowedItems = emptyMap<NeuInternalName, DragonProfitTrackerItemDataJson>()
    var lastDragonKill: DragonType? = null
    var lastDragonPlacement: Int? = null

    @HandleEvent
    fun onRepoReload(e: RepositoryReloadEvent) {
        allowedItems = e.getConstant<DragonProfitTrackerItemsJson>("DragonProfitTrackerItems").items
        println("Allowed items: $allowedItems")
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!config.enabled || event.source != ItemAddManager.Source.COMMAND) return
        with(tracker) { event.addItemFromEvent() }
    }

    init {
        tracker.initRenderer({ config.position }) { isEnabled() }
    }

    fun addEyes(amount: Int) {
        tracker.modify { it.eyesPlaced += amount }
        lastPlaced = amount
    }

    fun addDragonKill(type: DragonType) {
        tracker.modify { it.dragonKills.addOrPut(type, 1) }
        lastDragonKill = type
    }

    fun addDragonLoot(type: DragonType, item: NeuInternalName, amount: Int) {
        tracker.addItem(type, item, amount)
    }

    fun addDragonLootFromList(type: DragonType, items: List<Pair<NeuInternalName, Int>>) {
        items.forEach { (item, amount) -> addDragonLoot(type, item, amount) }

        val lootMap = mutableMapOf<String, Double>()
        var totalProfit = 0.0
        items.forEach { (internalName, amount) ->
            internalName.getPrice().takeIf { price: Double -> price != -1.0 }?.let { pricePer: Double ->
                val profit: Double = amount * pricePer
                val nameFormat = internalName.itemName
                val text = "§eFound $nameFormat §8${amount}x §7(§6$profit§7)"
                lootMap.addOrPut(text, profit)
                totalProfit += profit
            }
        }

        val eyePrice = NeuInternalName.fromItemNameOrNull("Summoning Eye")?.getPrice()
        if (eyePrice != null) {
            totalProfit -= eyePrice * lastPlaced
        }

        val hover = lootMap.sortedDesc().keys.toMutableList()

        val profitPrefix = if (totalProfit < 0) "§c" else "§6"
        val totalMessage = "Profit for Dragon§e: $profitPrefix${totalProfit.shortFormat()}"

        hover.add("§cUsed §5Summoning Eye§7: §c-${eyePrice?.times(lastPlaced)?.shortFormat()}")
        hover.add("§e$totalMessage")

        ChatUtils.hoverableChat(totalMessage, hover)
    }

    fun isEnabled() =
        LorenzUtils.inSkyBlock && config.enabled && DragonFightAPI.inNestArea()

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetdragonprofittracker") {
            description = "Resets the Dragon Profit Tracker."
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }
}
