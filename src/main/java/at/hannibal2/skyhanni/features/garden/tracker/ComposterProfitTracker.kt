package at.hannibal2.skyhanni.features.garden.tracker

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.OwnInventoryData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay.composterInventory
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay.fuelFactors
import at.hannibal2.skyhanni.features.garden.composter.ComposterOverlay.organicMatter
import at.hannibal2.skyhanni.features.garden.tracker.PestProfitTracker.addPriceFromButton
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTimedItemTracker
import at.hannibal2.skyhanni.utils.tracker.TimedTrackerData
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ComposterProfitTracker {
    val config get() = GardenApi.config.composters.profitTracker
    val tracker = SkyHanniTimedItemTracker(
        "Composter Profit Tracker",
        { Data() },
        { it.garden.composterProfitTracker },
        drawDisplay = { drawDisplay(it) },
        trackerConfig = { config.perTrackerConfig },
        customUptimeControl = true
    )

    private val patternGroup = RepoPattern.group("garden.composter.tracker")

    /**
     * REGEX-TEST: §aInserted §r§2190,000 Fuel §r§afrom your sacks!
     */
    private val fuelSackInsertPattern by patternGroup.pattern(
        "fuelsackinsert",
        "§aInserted §r§2(?<amount>[\\d,]+) Fuel §r§afrom your sacks!"
    )

    /**
     * REGEX-TEST: §aInserted §r§2256 §r§aOrganic Matter from your sacks!
     */
    private val organicSackInsertPattern by patternGroup.pattern(
        "organicsackinsert",
        "§aInserted §r§2(?<amount>[\\d,]+) §r§aOrganic Matter from your sacks!"
    )

    /**
     * REGEX-TEST: §aPicked up 64 Compost!
     */
    private val compostPickUpPattern by patternGroup.pattern(
        "compostpickup",
        "§aPicked up (?<amount>\\d+) Compost!"
    )

    val storage get() = GardenApi.storage

    private var lastFuelSackInsert: SimpleTimeMark = SimpleTimeMark.farPast()
    private var lastOrganicSackInsert: SimpleTimeMark = SimpleTimeMark.farPast()
    private var fuelAmount: Int? = null
    private var organicAmount: Int? = null
    private var inventorySnapshot: Map<NeuInternalName, Int>? = null
    private var paused = false

    // Check for inserts from sack
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onChat(event: SkyHanniChatEvent) {
        organicSackInsertPattern.matchMatcher(event.message) {
            organicAmount = group("amount").formatInt()
            lastOrganicSackInsert = SimpleTimeMark.now()
        }
        fuelSackInsertPattern.matchMatcher(event.message) {
            fuelAmount = group("amount").formatInt()
            lastFuelSackInsert = SimpleTimeMark.now()
        }
        // check for compost pickup
        compostPickUpPattern.matchMatcher(event.message) {
            val amount = group("amount").formatInt()
            tracker.modify { it.compostGained += amount }
        }
    }

    // check for items inserted into composter using the "insert from inventory" button
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventoryName.startsWith("Insert Crops") || event.inventoryName.startsWith("Insert Fuel")) {
            inventorySnapshot = OwnInventoryData.getCurrentItems()
        }
    }

    // check for items inserted by clicking on them
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        // player inventory slots
        if (event.slotId >= 54 && composterInventory.isInside()) {
            val item = event.item?.getInternalNameOrNull() ?: return
            val primitiveItem = NeuItems.getPrimitiveMultiplier(item).internalName
            if (primitiveItem !in organicMatter.keys + fuelFactors.keys) return
            // composter will refuse items if full with no warning message
            validateSlotClick(event.item, event.slotId)
            return
        }
        if (event.slotId == 11 && InventoryUtils.openInventoryName().startsWith("Insert ")) {
            compareInventory()
        }
    }

    // check for items inserted via sacks
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onSackChange(event: SackChangeEvent) {
        if (lastFuelSackInsert.passedSince() > 30.seconds && lastOrganicSackInsert.passedSince() > 30.seconds) return
        for (item in event.sackChanges) {
            if (item.internalName in organicMatter.keys + fuelFactors.keys) {
                if (item.delta > 0) continue
                addItem(item.internalName, item.delta)
            }
        }
        lastFuelSackInsert = SimpleTimeMark.farPast()
        lastOrganicSackInsert = SimpleTimeMark.farPast()
    }

    // handle uptime, uptime should only run when composter is active
    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (storage?.composterEmptyTime?.isInPast() == true && !paused) {
            paused = true
            tracker.pauseSessionUptime()
            tracker.activeStopwatches.forEach { it.getActiveStopwatch()?.add(storage?.composterProfitTrackerTimeLeft ?: 0.seconds) }
            storage?.composterProfitTrackerTimeLeft = 0.seconds
        } else if (storage?.composterEmptyTime?.isInFuture() == true) {
            paused = false
            tracker.startSessionUptime()
            // add extra time if offline
            val emptyTime = storage?.composterEmptyTime?.timeUntil()
            val timeLeft = storage?.composterProfitTrackerTimeLeft
            if (emptyTime != null && timeLeft != null) {
                val diff = (timeLeft - emptyTime).inWholeSeconds
                if (diff > 10) {
                    tracker.activeStopwatches.forEach { it.getActiveStopwatch()?.add(diff.seconds) }
                }
            }
            storage?.composterEmptyTime?.timeUntil()?.let { storage?.composterProfitTrackerTimeLeft = it }
        }
    }

    // check inventory diff after clicking "insert from inventory" button
    private fun compareInventory() {
        val oldInventory = inventorySnapshot ?: return
        DelayedRun.runDelayed(.5.seconds) {
            val newInventory = OwnInventoryData.getCurrentItems()
            for (item in oldInventory.keys) {
                val primitiveItem = NeuItems.getPrimitiveMultiplier(item).internalName
                if (primitiveItem !in organicMatter.keys + fuelFactors.keys) continue
                val diff = oldInventory[item]?.minus((newInventory[item] ?: 0)) ?: continue
                if (diff > 0) addItem(item, -diff)
            }
            inventorySnapshot = null
        }
    }

    // make sure items clicked on were actually added
    private fun validateSlotClick(item: ItemStack, slotId: Int) {
        DelayedRun.runDelayed(.5.seconds) {
            val itemName = item.getInternalNameOrNull() ?: return@runDelayed
            val amount = item.stackSize
            val newItem = InventoryUtils.getItemAtSlotNumber(slotId)
            if (newItem == item) return@runDelayed
            if (newItem?.getInternalNameOrNull() != itemName) {
                addItem(itemName, amount)
            } else {
                val diff = amount - newItem.stackSize
                if (diff <= 0) return@runDelayed
                addItem(itemName, -diff)
            }

        }
    }

    private fun addItem(item: NeuInternalName, amount: Int) {
        tracker.addItem(item, abs(amount), false)

    }

    class TimeData : TimedTrackerData<Data>({ Data() })

    data class Data(
        @Expose var compostGained: Long = 0
    ) : ItemTrackerData<SessionUptime.Normal>(SessionUptime.Normal::class) {
        override fun getDescription(timesGained: Long): List<String> {
            return listOf(
                "§7You have inserted this §e${timesGained.addSeparators()} times."
            )
        }

        override fun getCoinName(item: TrackedItem) = "§6Dropped Coins"

        override fun getCoinDescription(item: TrackedItem): List<String> {
            val coinsFormat = item.totalAmount.shortFormat()
            return listOf(
                "§7Something has went wrong if you can see this.",
                "§7You somehow gained §6$coinsFormat coins.",
            )
        }
    }

    fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lComposter Profit Tracker")
        val compostAmount = data.compostGained
        val compostProfit = compostAmount * "COMPOST".toInternalName().getPrice()
        addSearchString("§eCompost Collected: §7${compostAmount.addSeparators()} ${compostProfit.formatCoin()}")
        addSearchString("§eItems Spent:")
        val itemCost = tracker.drawItems(data, { true }, this)
        val profit = compostProfit - itemCost

        val duration = data.getTotalUptime()
        addAll(tracker.addTotalProfit(profit, itemCost.toLong(), "coin spent", duration, "Coins spent"))

        addPriceFromButton(this)
    }

    init {
        tracker.initRenderer({ config.position }, composterInventory) { shouldShowDisplay() }
    }

    private fun shouldShowDisplay(): Boolean {
        if (!GardenApi.inGarden()) return false
        if (!config.enabled) return false
        if (config.onlyInInventory && !composterInventory.isInside()) return false

        return true
    }
}
