package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.entity.ItemAddInInventoryEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.test.PriceSource
import at.hannibal2.skyhanni.utils.ItemUtils.getItemName
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.jsonobjects.FishingProfitItemsJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FishingProfitTracker {
    private val config get() = SkyHanniMod.feature.fishing.fishingProfitTracker
    private var lastClickDelay = 0L

    private val tracker =
        SkyHanniTracker("Fishing Profit Tracker", { Data() }, { it.fishing.fishingProfitTracker }) { drawDisplay(it) }

    class Data : TrackerData() {
        override fun reset() {
            items.clear()
        }

        @Expose
        var items = mutableMapOf<NEUInternalName, FishingItem>()

        @Expose
        var totalCatchAmount = 0L

        class FishingItem {
            @Expose
            var internalName: NEUInternalName? = null

            @Expose
            var timesCaught: Long = 0

            @Expose
            var totalAmount: Long = 0

            @Expose
            var hidden = false

            override fun toString() = "FishingItem{" +
                "internalName='" + internalName + '\'' +
                ", timesDropped=" + timesCaught +
                ", totalAmount=" + totalAmount +
                ", hidden=" + hidden +
                '}'
        }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§e§lFishing Profit Tracker")

        var profit = 0.0
        val map = mutableMapOf<Renderable, Long>()
        for ((internalName, itemProfit) in data.items) {
            val amount = itemProfit.totalAmount

            val price = (getPrice(internalName) * amount).toLong()

//             val cleanName = SlayerAPI.getNameWithEnchantmentFor(internalName)
            val cleanName = internalName.getItemName()
            var name = cleanName
            val priceFormat = NumberUtil.format(price)
            val hidden = itemProfit.hidden
            if (hidden) {
                while (name.startsWith("§f")) {
                    name = name.substring(2)
                }
                name = StringUtils.addFormat(name, "§m")
            }
            val text = " §7${amount.addSeparators()}x $name§7: §6$priceFormat"

            val timesCaught = itemProfit.timesCaught
            val percentage = timesCaught.toDouble() / data.totalCatchAmount
            val catchRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))

            val renderable = if (tracker.isInventoryOpen()) Renderable.clickAndHover(
                text, listOf(
                    "§7Caught §e${timesCaught.addSeparators()} §7times.",
                    "§7Your catch rate: §c$catchRate",
                    "",
                    "§eClick to " + (if (hidden) "show" else "hide") + "!",
                    "§eControl + Click to remove this item!",
                )
            ) {
                if (System.currentTimeMillis() > lastClickDelay + 150) {

                    if (KeyboardManager.isControlKeyDown()) {
                        data.items.remove(internalName)
                        LorenzUtils.chat("§e[SkyHanni] Removed $cleanName §efrom Fishing Frofit Tracker.")
                        lastClickDelay = System.currentTimeMillis() + 500
                    } else {
                        itemProfit.hidden = !hidden
                        lastClickDelay = System.currentTimeMillis()
                    }
                    tracker.update()
                }
            } else Renderable.string(text)
            if (tracker.isInventoryOpen() || !hidden) {
                map[renderable] = price
            }
            profit += price
        }

        for (text in map.sortedDesc().keys) {
            addAsSingletonList(text)
        }

        val fishedCount = data.totalCatchAmount
        addAsSingletonList(
            Renderable.hoverTips(
                "§7Times fished: §e${fishedCount.addSeparators()}",
                listOf("§7You catched §e${fishedCount.addSeparators()} §7times something.")
            )
        )

        val profitFormat = NumberUtil.format(profit)
        val profitPrefix = if (profit < 0) "§c" else "§6"

        val profitPerCatch = profit / data.totalCatchAmount
        val profitPerCatchFormat = NumberUtil.format(profitPerCatch)

        val text = "§eTotal Profit: $profitPrefix$profitFormat"
        addAsSingletonList(Renderable.hoverTips(text, listOf("§7Profit per catch: $profitPrefix$profitPerCatchFormat")))

        if (tracker.isInventoryOpen()) {
            addSelector<PriceSource>(
                "",
                getName = { type -> type.displayName },
                isCurrent = { it.ordinal == config.priceFrom },
                onChange = {
                    config.priceFrom = it.ordinal
                    tracker.update()
                }
            )
        }
    }

    @SubscribeEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!isEnabled()) return

        for (sackChange in event.sackChanges) {
            val change = sackChange.delta
            if (change > 0) {
                val internalName = sackChange.internalName
                addItem(internalName, change)
            }
        }
    }

    @SubscribeEvent
    fun onItemAdd(event: ItemAddInInventoryEvent) {
        if (!isEnabled()) return

        addItem(event.internalName, event.amount)
    }

    private fun addItemPickup(internalName: NEUInternalName, stackSize: Int) {
        tracker.modify {
            val fishingItem = it.items.getOrPut(internalName) { Data.FishingItem() }

            fishingItem.timesCaught++
            fishingItem.totalAmount += stackSize
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return

        tracker.renderDisplay(config.position)
    }

    private fun addItem(internalName: NEUInternalName, amount: Int) {
        if (!isAllowedItem(internalName)) {
            LorenzUtils.debug("Ignored non-fishing item pickup: $internalName'")
            return
        }

        addItemPickup(internalName, amount)
    }

    private var allowedItems = listOf<NEUInternalName>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        allowedItems = event.getConstant<FishingProfitItemsJson>("FishingProfitItems").items
    }

    private fun isAllowedItem(internalName: NEUInternalName): Boolean {
        return internalName in allowedItems
    }

    private fun getPrice(internalName: NEUInternalName) = when (config.priceFrom) {
        0 -> internalName.getBazaarData()?.sellPrice ?: internalName.getPriceOrNull() ?: 0.0
        1 -> internalName.getBazaarData()?.buyPrice ?: internalName.getPriceOrNull() ?: 0.0

        else -> internalName.getNpcPriceOrNull() ?: 0.0
    }

    fun resetCommand(args: Array<String>) {
        tracker.resetCommand(args, "shresetfishingtracker")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && FishingAPI.hasFishingRodInHand()
}
