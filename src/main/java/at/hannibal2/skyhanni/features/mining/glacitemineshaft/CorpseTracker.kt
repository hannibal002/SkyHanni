package at.hannibal2.skyhanni.features.mining.glacitemineshaft

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.mining.CorpseLootedEvent
import at.hannibal2.skyhanni.features.mining.mineshaft.CorpseType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.CollectionUtils.addIfNotNull
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.BucketedItemTrackerData
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData.TrackedItem
import at.hannibal2.skyhanni.utils.tracker.SkyHanniBucketedItemTracker
import com.google.gson.annotations.Expose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.EnumMap

@SkyHanniModule
object CorpseTracker {
    private val config get() = SkyHanniMod.feature.mining.glaciteMineshaft.corpseTracker

    private val tracker = SkyHanniBucketedItemTracker<CorpseType, Data>(
        "Corpse Tracker",
        { Data() },
        { it.mining.mineshaft.corpseProfitTracker },
    ) { drawDisplay(it) }

    class Data: BucketedItemTrackerData<CorpseType>() {
        override fun resetItems() {
            corpsesLooted = EnumMap(CorpseType::class.java)
        }

        override fun getDescription(bucket: CorpseType?, timesGained: Long): List<String> {
            val divisor = 1.coerceAtLeast(bucket?.let { corpsesLooted[bucket]?.toInt() } ?: corpsesLooted.sumAllValues().toInt())
            val percentage = timesGained.toDouble() /  divisor
            val dropRate = LorenzUtils.formatPercentage(percentage.coerceAtMost(1.0))
            return listOf(
                "§7Dropped §e${timesGained.addSeparators()} §7times.",
                "§7Your drop rate: §c$dropRate.",
            )
        }

        override fun getCoinName(item: TrackedItem) = "<no coins>"
        override fun getCoinDescription(item: TrackedItem): List<String> = listOf("<no coins>")

        @Expose
        var corpsesLooted: MutableMap<CorpseType, Long> = EnumMap(CorpseType::class.java)

        @Expose
        var corpseCount: Long = selectedBucket?.let { corpsesLooted[it] } ?: corpsesLooted.sumAllValues().toLong()
    }

    private fun addLootedCorpse(type: CorpseType) = tracker.modify { it.corpsesLooted.addOrPut(type, 1) }

    @SubscribeEvent
    fun onCorpseLoot(event: CorpseLootedEvent) {
        addLootedCorpse(event.corpseType)
        for ((itemName, amount) in event.loot) {
            NEUInternalName.fromItemNameOrNull(itemName)?.let { item ->
                tracker.modify {
                    it.addItem(event.corpseType, item, amount)
                }
            }
        }
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList("§b§lGlacite Corpse Profit Tracker")
        var profit = tracker.drawItems(data, { true }, this)

        if (data.corpseCount > 0) {
            val applicableKeys: List<CorpseType> = data.selectedBucket?.let { listOf(it) } ?: CorpseType.entries
            val keyCostStrings: List<String> = mutableListOf()
            var totalKeyCost = 0.0
            var totalKeyCount = 0
            applicableKeys.forEach {
                it.key?.let { key ->
                    val keyName = key.itemName
                    val price = key.getPrice()
                    val count = (data.corpsesLooted[it] ?: 0)
                    val totalPrice = (price * count)
                    if (totalPrice > 0) {
                        keyCostStrings.addIfNotNull("§7${count}x $keyName§7: §c-${totalPrice.shortFormat()}")
                        profit -= totalPrice
                        totalKeyCost += totalPrice
                        totalKeyCount += count.toInt()
                    }
                }
            }
            val keyFormat = "§7${totalKeyCount}x ${if (applicableKeys.count() == 1) applicableKeys.first() else "§eCorpse Keys"}§7: §c-${totalKeyCost.shortFormat()}"
            addAsSingletonList(
                if (applicableKeys.count() == 1) Renderable.string(keyFormat)
                else Renderable.hoverTips(
                    keyFormat,
                    keyCostStrings
                )
            )

            addAsSingletonList(tracker.addTotalProfit(profit, data.corpseCount, "loot"))
        }

        tracker.addPriceFromButton(this)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        tracker.renderDisplay(config.position)
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (event.newIsland == IslandType.MINESHAFT) {
            tracker.firstUpdate()
        }
    }

    fun resetCommand() {
        tracker.resetCommand()
    }

    fun isEnabled() =
        config.enabled && IslandType.MINESHAFT.isInIsland() && (!config.onlyInMineshaft || LorenzUtils.skyBlockArea == "Glacite Mineshafts")
}
