package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.Searchable
import at.hannibal2.skyhanni.utils.renderables.toSearchable
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.SkyHanniItemTracker
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HuntingProfitTracker {

    val config get() = SkyHanniMod.feature.hunting.huntingProfitTracker

    private var lastHuntTime = SimpleTimeMark.farPast()
    private val tracker = SkyHanniItemTracker(
        "Hunting Profit Tracker",
        { Data() },
        { it.hunting.huntingProfitTracker },
    ) { drawDisplay(it) }

    class Data : ItemTrackerData() {

        override fun resetItems() {
            totalCatchAmount = 0
            totalShardAmount = 0
        }

        override fun getDescription(item: TrackedItem): List<String> {
            val timesCaught = item.timesCaught
            val itemsCaught = item.totalAmount

            val shardRate = (itemsCaught.toDouble() / timesCaught.toDouble()).roundTo(2)

            return listOf(
                "§7Caught §e${timesCaught.addSeparators()} §7times.",
                "§7Your shards per catch: §c$shardRate",
            )
        }

        override fun getDescription(timesCaught: Long) = listOf<String>()

        override fun getCoinName(item: TrackedItem) = ""

        override fun getCoinDescription(item: TrackedItem) = listOf<String>()

        @Expose
        var totalCatchAmount = 0L

        @Expose
        var totalShardAmount = 0L
    }

    private val toolInternalNames = setOf(
        "SMALL_POCKET_BLACK_HOLE".toInternalName(),
        "MEDIUM_POCKET_BLACK_HOLE".toInternalName(),
        "VENATOR_GENESIS".toInternalName(),
        "SILVA_DOMINUS".toInternalName(),
        "CURSUS_FERAE".toInternalName(),
        "APEX_PRAEDATOR".toInternalName(),
        "NEX_TITANIUM".toInternalName()
    )

    private val ItemTrackerData.TrackedItem.timesCaught get() = timesGained

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lHunting Profit Tracker")

        val profit = tracker.drawItems(data, { true }, this)

        val caughtCount = data.totalCatchAmount
        add(
            Renderable.hoverTips(
                "§7Mobs caught: §e${caughtCount.addSeparators()}",
                listOf("§7You've hunted §e${caughtCount.addSeparators()} §7mobs."),
            ).toSearchable(),
        )

        val shardCount = data.totalShardAmount
        add(
            Renderable.hoverTips(
                "§7Shards collected: §e${shardCount.addSeparators()}",
                listOf("§7You've collected §e${shardCount.addSeparators()} §7shards."),
            ).toSearchable(),
        )

        add(tracker.addTotalProfit(profit, data.totalCatchAmount, "shard"))

        tracker.addPriceFromButton(this)
    }

    private fun addShard(amount: Int) {
        tracker.modify {
            it.totalCatchAmount++
            it.totalShardAmount += amount
        }
        lastHuntTime = SimpleTimeMark.now()
    }

    private val isRecentPickup: Boolean
        get() = config.showWhenPickup && lastHuntTime.passedSince() < 10.seconds

    private val shouldShow: Boolean
        get() = isRecentPickup // || HuntingApi.isHunting(checkRodInHand = false)

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() && config.enabled && (shouldShow || heldItemEnabled()) },
            onRender = {
                tracker.renderDisplay(config.position)
            },
        )
    }

//    private fun tryAddItem(internalName: NeuInternalName, amount: Int, command: Boolean) {
//        if (!isAllowedItem(internalName)) {
//            ChatUtils.debug("Ignored non-hunting item pickup: $internalName'")
//            return
//        }
//
//        tracker.addItem(internalName, amount, command)
//    }

    @HandleEvent
    fun onShardGainEvent(event: ShardGainEvent) {
        if (event.amount <= 0 || !event.caught) return
        addShard(event.amount)
        tracker.addItem(event.shardInternalName, event.amount, command = false)
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock

    private fun heldItemEnabled() = (isHoldingTool() || lastToolHeldTime.passedSince() < 10.seconds)

    private fun isHoldingTool() = isHuntingTool(InventoryUtils.getItemInHand())

    private var lastToolHeldTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var hasHeldTool: Boolean = false

    private fun isHuntingTool(itemStack: ItemStack?): Boolean {
        val itemCategoryOrNull = itemStack?.getItemCategoryOrNull()

        // Check if the item is one of the general hunting tool categories
        if (itemCategoryOrNull == ItemCategory.FISHING_NET ||
            itemCategoryOrNull == ItemCategory.LASSO ||
            itemCategoryOrNull == ItemCategory.AXE
        ) return true

        // Check if the item’s internal name is in the set of specific hunting tools
        return toolInternalNames.contains(itemStack?.getInternalNameOrNull())
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresethuntingtracker") {
            description = "Resets the Hunting Profit Tracker"
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }

    @HandleEvent
    fun onItemChange(event: ItemInHandChangeEvent) {
        val isTool = isHuntingTool(event.newItem.getItemStackOrNull())
        if (isTool != hasHeldTool) {
            if (!isTool) {
                lastToolHeldTime = SimpleTimeMark.now()
            }
            hasHeldTool = isTool
        }
    }
}
