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
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import com.google.gson.annotations.Expose
import java.util.EnumMap

@SkyHanniModule
object DragonProfitTracker {
    private val config get() = SkyHanniMod.feature.combat.endIsland.dragon.dragonProfitTracker

    private var lastPlaced: Int = 0
    private val SUMMONING_EYE = "SUMMONING_EYE".toInternalName()

    private val tracker = SkyHanniBucketedItemTracker(
        "Dragon Profit Tracker",
        { BucketData() },
        { it.dragonProfitTracker },
        { drawDisplay(it) },
    )

    class BucketData : BucketedItemTrackerData<DragonType>(DragonType::class) {
        override fun getCoinName(bucket: DragonType?, item: TrackedItem) = "<no coins>"
        override fun getCoinDescription(bucket: DragonType?, item: TrackedItem): List<String> = listOf("<no coins>")

        override fun DragonType.isBucketSelectable(): Boolean = this.selectable

        override fun resetItems() {
            dragonKills.clear()
            eyesPlaced = 0
        }

        override fun getDescription(bucket: DragonType?, timesGained: Long): List<String> {
            val percentage = timesGained.toDouble() / getTotalDragonCount()
            val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
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

        var profit = tracker.drawItems(bucketData, { true }, this)

        val eyePrice = SkyHanniTracker.getPricePer(SUMMONING_EYE)
        val totalEyePrice = eyePrice * bucketData.eyesPlaced
        profit -= totalEyePrice
        val eyeFormat = "§7${bucketData.eyesPlaced}x §5Summoning Eye §c${(-totalEyePrice).shortFormat()}"
        add(StringRenderable(eyeFormat).toSearchable("Summoning Eye"))

        val colorCode = bucketData.selectedBucket?.color ?: LorenzColor.AQUA
        val displayName = bucketData.selectedBucket?.displayName ?: "Total Dragon"
        val killAmount = bucketData.getTotalDragonCount()
        val dragonString = "${colorCode.getChatColor()}$displayName §r§bkills: $killAmount"
        add(StringRenderable(dragonString).toSearchable())

        add(tracker.addTotalProfit(profit, bucketData.getTotalDragonCount(), "Dragon"))

        tracker.addPriceFromButton(this)
    }

    var allowedItems = emptyMap<NeuInternalName, DragonProfitTrackerItemDataJson>()
    var lastDragonKill: DragonType? = null
    var lastDragonPlacement: Int? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        allowedItems = event.getConstant<DragonProfitTrackerItemsJson>("DragonProfitTrackerItems").items
    }

    @HandleEvent
    fun onItemAdd(event: ItemAddEvent) {
        if (!DragonFightAPI.inNestArea() || event.source != ItemAddManager.Source.COMMAND) return
        with(tracker) { event.addItemFromEvent() }
        ChatUtils.debug("Added item to tracker: ${event.internalName} (amount: ${event.amount})")
    }

    init {
        tracker.initRenderer({ config.position }) { config.enabled && DragonFightAPI.inNestArea() }
    }

    fun addEyes(amount: Int) {
        tracker.modify { it.eyesPlaced += amount }
        ChatUtils.debug("Added $amount eyes to tracker")
        lastPlaced = amount
    }

    fun addDragonKill(type: DragonType) {
        tracker.modify { it.dragonKills.addOrPut(type, 1) }
        lastDragonKill = type
        ChatUtils.debug("Added $type to tracker, lastDragonKill: $lastDragonKill")
    }

    fun addDragonLoot(type: DragonType, item: NeuInternalName, amount: Int, command: Boolean = false) {
        tracker.addItem(type, item, amount, command)
        ChatUtils.debug("Added $item to tracker (amount: $amount, type: $type)")
    }

    fun addDragonLootFromList(type: DragonType, items: List<Pair<NeuInternalName, Int>>) {
        if (lastPlaced == 0 && !config.countLeechedDragons) return
        items.forEach { (item, amount) -> addDragonLoot(type, item, amount) }

        val lootMap = mutableMapOf<String, Double>()
        var totalProfit = 0.0
        items.forEach { (internalName, amount) ->
            SkyHanniTracker.getPricePer(internalName).takeIf { price: Double -> price != -1.0 }?.let { pricePer: Double ->
                val profit: Double = amount * pricePer
                val nameFormat = internalName.repoItemName
                val text = "§eFound $nameFormat §8${amount}x §7(§6${profit.shortFormat()}§7)"
                lootMap.addOrPut(text, profit)
                totalProfit += profit
            }
        }


        val eyePrice = SkyHanniTracker.getPricePer(SUMMONING_EYE)
        if (eyePrice != null) {
            totalProfit -= eyePrice * lastPlaced
        }

        val hover = lootMap.sortedDesc().keys.toMutableList()

        val profitPrefix = if (totalProfit < 0) "§c" else "§6"
        val totalMessage = "Profit for Dragon§e: $profitPrefix${totalProfit.shortFormat()}"

        hover.add("§cPlaced §5Summoning Eye§7: §c-${eyePrice.times(lastPlaced).shortFormat()}")
        hover.add("§e$totalMessage")

        ChatUtils.hoverableChat(totalMessage, hover)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresetdragonprofittracker") {
            description = "Resets the Dragon Profit Tracker."
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }
}
