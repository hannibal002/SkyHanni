package at.hannibal2.hanni.features.rift.area.mountaintop

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.commands.CommandCategory
import at.hannibal2.hanni.config.commands.CommandRegistrationEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.ItemAddManager
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.events.ItemAddEvent
import at.hannibal2.hanni.features.rift.RiftApi
import at.hannibal2.hanni.features.rift.RiftApi.motesNpcPrice
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.InventoryUtils
import at.hannibal2.hanni.utils.ItemUtils
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.hanni.utils.NeuItems
import at.hannibal2.hanni.utils.NeuItems.getItemStack
import at.hannibal2.hanni.utils.NumberUtil.shortFormat
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.hanni.utils.renderables.Searchable
import at.hannibal2.hanni.utils.tracker.ItemTrackerData
import at.hannibal2.hanni.utils.tracker.HanniItemTracker
import kotlin.time.Duration.Companion.seconds

@HanniModule
object TimiteTracker {

    private val config get() = HanniMod.feature.rift.area.mountaintop.timite
    private val HIGHLITE = "HIGHLITE".toInternalName()
    private val TIMITE = "TIMITE".toInternalName()

    class Data : ItemTrackerData() {
        override fun getDescription(timesGained: Long): List<String> = emptyList()

        override fun getCoinName(item: TrackedItem): String = "§5Motes"

        override fun getCoinDescription(item: TrackedItem): List<String> = emptyList()

        override fun getCustomPricePer(internalName: NeuInternalName): Double {
            return internalName.getItemStack().motesNpcPrice() ?: 0.0
        }

        fun getTime(): Int = this.items[TIMITE]?.let {
            it.totalAmount.toInt() * 2
        } ?: 0
    }

    private fun drawDisplay(data: Data): List<Searchable> = buildList {
        addSearchString("§9§lTimite Tracker")
        val profit = tracker.drawItems(data, { true }, this)

        NeuItems.getRecipes(HIGHLITE).singleOrNull()?.let { highliteRecipe ->
            var craftableAmount = 0

            for (neededItem in ItemUtils.neededItems(highliteRecipe)) {
                if (neededItem.key in validItems) {
                    data.items[neededItem.key]?.let {
                        val amountCanCraft = it.totalAmount.toInt() / neededItem.value
                        if (craftableAmount == 0 || amountCanCraft < craftableAmount) {
                            craftableAmount = amountCanCraft
                        }
                    }
                }
            }
            val motes = HIGHLITE.motesNpcPrice()?.times(craftableAmount)?.shortFormat() ?: "0"
            if (craftableAmount > 0) {
                addSearchString(" §7${craftableAmount.shortFormat()}x ${HIGHLITE.repoItemName} Craftable§7: §5$motes motes")
            }
        }

        addSearchString("§aTime§7: §a${data.getTime().seconds.format()}ф")
        addSearchString("§dTotal Profit§7: §5${profit.toInt().shortFormat()} Motes")
    }

    private val tracker = HanniItemTracker("Timite Tracker", { Data() }, { it.rift.timiteTracker }) {
        drawDisplay(it)
    }

    private val validItems = listOf(
        TIMITE,
        "YOUNGITE".toInternalName(),
        "OBSOLITE".toInternalName(),
    )

    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onItem(event: ItemAddEvent) {
        if (event.internalName in validItems) {
            tracker.addItem(event.internalName, event.amount, event.source == ItemAddManager.Source.COMMAND)
        }
    }

    // TODO use RenderDisplayHelper
    @HandleEvent(onlyOnIsland = IslandType.THE_RIFT)
    fun onRender(event: GuiRenderEvent) {
        if (!isEnabled()) return

        tracker.renderDisplay(config.trackerPosition)
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shresettimitetracker") {
            description = "Resets the Timite Tracker."
            category = CommandCategory.USERS_RESET
            callback { tracker.resetCommand() }
        }
    }

    private fun isEnabled() =
        RiftApi.inMountainTop() &&
            config.tracker &&
            (!config.onlyShowWhileHolding || InventoryUtils.itemInHandId in timiteItems)

    private val timiteItems = listOf(
        "ANTI_SENTIENT_PICKAXE".toInternalName(),
        "EON_PICKAXE".toInternalName(),
        "CHRONO_PICKAXE".toInternalName(),
        "TIME_GUN".toInternalName(),
    )
}
