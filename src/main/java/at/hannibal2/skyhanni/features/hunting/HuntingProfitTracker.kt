package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.item.ShardGainEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName
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
import net.minecraft.world.item.ItemStack
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

    data class Data(
        @Expose var totalMobsCaught: Long = 0,
        @Expose var totalShardsGained: Long = 0
    ) : ItemTrackerData() {

        override fun getDescription(item: TrackedItem): List<String> {
            val timesCaught = item.timesCaught
            val shardsGained = item.totalAmount

            val shardRate = (if (timesCaught != 0L) shardsGained.toDouble() / timesCaught else 0.0).roundTo(2)

            return listOf(
                "§7Caught §e${timesCaught.addSeparators()} §7times.",
                "§7Your shards per catch: §c$shardRate",
            )
        }

        override fun getDescription(timesGained: Long) = listOf<String>()

        override fun getCoinName(item: TrackedItem) = ""

        override fun getCoinDescription(item: TrackedItem) = listOf<String>()
    }

    private var huntingTools = listOf<NeuInternalName>()

    private val ItemTrackerData.TrackedItem.timesCaught get() = timesGained

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§e§lHunting Profit Tracker")

        val profit = tracker.drawItems(data, { true }, this)

        val caughtCount = data.totalMobsCaught
        add(
            Renderable.hoverTips(
                "§7Mobs caught: §e${caughtCount.addSeparators()}",
                listOf("§7You've hunted §e${caughtCount.addSeparators()} §7mobs."),
            ).toSearchable(),
        )

        val shardCount = data.totalShardsGained
        add(
            Renderable.hoverTips(
                "§7Shards collected: §e${shardCount.addSeparators()}",
                listOf("§7You've collected §e${shardCount.addSeparators()} §7shards."),
            ).toSearchable(),
        )

        val duration = data.getTotalUptime()
        addAll(tracker.addTotalProfit(profit, data.totalMobsCaught, "shard", duration, "Shards"))

        tracker.addPriceFromButton(this)
    }

    private fun addShard(amount: Int) {
        tracker.modify {
            it.totalMobsCaught++
            it.totalShardsGained += amount
        }
        lastHuntTime = SimpleTimeMark.now()
    }

    private val isRecentPickup: Boolean
        get() = config.showWhenPickup && lastHuntTime.passedSince() < 10.seconds

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { isEnabled() && config.enabled && (isRecentPickup || heldItemEnabled()) },
            onRender = {
                tracker.renderDisplay(config.position)
            },
        )
    }

    @HandleEvent
    fun onShardGainEvent(event: ShardGainEvent) {
        if (event.amount <= 0) return
        addShard(event.amount)
        tracker.addItem(event.shardInternalName, event.amount, command = false)
    }

    @HandleEvent
    fun onItemChange(event: ItemInHandChangeEvent) {
        val isTool = isHuntingTool(event.newItem.getItemStackOrNull())
        if (isTool != hasHeldTool) {
            hasHeldTool = isTool
            if (!isTool) lastToolHeldTime = SimpleTimeMark.now()
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    private fun heldItemEnabled() = isHoldingTool() || lastToolHeldTime.passedSince() < 10.seconds

    private fun isHoldingTool() = isHuntingTool(InventoryUtils.getItemInHand())

    private var lastToolHeldTime: SimpleTimeMark = SimpleTimeMark.farPast()
    private var hasHeldTool: Boolean = false

    private fun isHuntingTool(itemStack: ItemStack?): Boolean {
        val itemCategoryOrNull = itemStack?.getItemCategoryOrNull()

        // Check if the item is one of the general hunting tool categories
        if (itemCategoryOrNull == ItemCategory.FISHING_NET ||
            itemCategoryOrNull == ItemCategory.LASSO
        ) return true

        // Check if the item’s internal name is in the set of specific hunting tools
        val internalName = itemStack?.getInternalNameOrNull() ?: return false
        return internalName in huntingTools
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<ItemsJson>("Items")
        huntingTools = buildList {
            data.huntingBlackholes?.let { addAll(it) }
            data.huntingAxes?.let { addAll(it) }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresethuntingtracker") {
            description = "Resets the Hunting Profit Tracker"
            category = CommandCategory.USERS_RESET
            simpleCallback { tracker.resetCommand() }
        }
    }
}
