package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import kotlin.time.Duration.Companion.seconds

class SkyHanniBucketedItemTracker<E: Enum<E>, BucketedData : BucketedItemTrackerData<E>>(
    name: String,
    createNewSession: () -> BucketedData,
    getStorage: (ProfileSpecificStorage) -> BucketedData,
    drawDisplay: (BucketedData) -> List<List<Any>>,
) : SkyHanniTracker<BucketedData>(name, createNewSession, getStorage, drawDisplay) {

    companion object {
        val SKYBLOCK_COIN = NEUInternalName.SKYBLOCK_COIN
    }

    fun addCoins(bucket: E, coins: Int) {
        addItem(bucket, SKYBLOCK_COIN, coins)
    }

    fun addItem(bucket: E, internalName: NEUInternalName, amount: Int) {
        modify {
            it.addItem(bucket, internalName, amount)
        }
        getSharedTracker()?.let {
            val hidden = it.get(DisplayMode.TOTAL).getItemsProp()[internalName]!!.hidden
            it.get(DisplayMode.SESSION).getItemsProp()[internalName]!!.hidden = hidden
        }

        val (itemName, price) = SlayerAPI.getItemNameAndPrice(internalName, amount)
        if (config.warnings.chat && price >= config.warnings.minimumChat) {
            ChatUtils.chat("§a+Tracker Drop§7: §r$itemName")
        }
        if (config.warnings.title && price >= config.warnings.minimumTitle) {
            LorenzUtils.sendTitle("§a+ $itemName", 5.seconds)
        }
    }

    fun addPriceFromButton(lists: MutableList<List<Any>>) {
        if (isInventoryOpen()) {
            lists.addSelector<ItemPriceSource>(
                "",
                getName = { type -> type.sellName },
                isCurrent = { it.ordinal == config.priceSource.ordinal }, // todo avoid ordinal
                onChange = {
                    config.priceSource = ItemPriceSource.entries[it.ordinal] // todo avoid ordinal
                    update()
                }
            )
        }
    }

    fun addBucketSelectors(data: BucketedData) = buildList {
        if (isInventoryOpen()) {
            val bucket = data.selectedBucket
            add(
                listOf(
                    Renderable.string("§7Loot source: "),
                    Renderable.optionalLink(
                        if (bucket == null) "§a§l[§r§aAll§r§a§l]" else "§e[All]",
                        { data.selectBucket(null); update(); }
                    ) { bucket != null },
                )
            )
            data.getPoppedBuckets().chunked(3).forEach { bucketChunk ->
                add(bucketChunk.map {
                    Renderable.optionalLink(
                        if (bucket == it) "§a§l[§r$it§r§a§l] " else "§e[§r$it§e] ",
                        { data.selectBucket(it); update(); }
                    ) { bucket != it }
                })
            }
        }
    }

    fun drawItems(
        data: BucketedData,
        filter: (NEUInternalName) -> Boolean,
        lists: MutableList<List<Any>>,
    ): Double {
        var profit = 0.0
        val dataItems = data.getItemsProp()
        val items = mutableMapOf<NEUInternalName, Long>()
        for ((internalName, itemProfit) in dataItems) {
            if (!filter(internalName)) continue

            val amount = itemProfit.totalAmount
            val pricePer =
                if (internalName == SKYBLOCK_COIN) 1.0 else data.getCustomPricePer(internalName)
            val price = (pricePer * amount).toLong()
            val hidden = itemProfit.hidden

            if (isInventoryOpen() || !hidden) {
                items[internalName] = price
            }
            if (!hidden || !config.excludeHiddenItemsInPrice) {
                profit += price
            }
        }

        val limitList = config.hideCheapItems
        var pos = 0
        val hiddenItemTexts = mutableListOf<String>()
        for ((internalName, price) in items.sortedDesc()) {
            val itemProfit = data.getItemsProp()[internalName] ?: error("Item not found for $internalName")

            val amount = itemProfit.totalAmount
            val displayAmount = if (internalName == SKYBLOCK_COIN) itemProfit.timesGained else amount

            val cleanName = if (internalName == SKYBLOCK_COIN) {
                data.getCoinName(itemProfit)
            } else {
                internalName.itemName
            }

            val priceFormat = price.shortFormat()
            val hidden = itemProfit.hidden
            val newDrop = itemProfit.lastTimeUpdated.passedSince() < 10.seconds && config.showRecentDrops
            val numberColor = if (newDrop) "§a§l" else "§7"

            var displayName = if (hidden) {
                "§8§m" + cleanName.removeColor(keepFormatting = true).replace("§r", "")
            } else cleanName
            displayName = " $numberColor${displayAmount.addSeparators()}x $displayName§7: §6$priceFormat"

            pos++
            if (limitList.enabled.get()) {
                if (pos > limitList.alwaysShowBest.get()) {
                    if (price < limitList.minPrice.get() * 1000) {
                        hiddenItemTexts += displayName
                        continue
                    }
                }
            }

            val lore = buildLore(data, itemProfit, hidden, newDrop, internalName)
            val renderable = if (isInventoryOpen()) Renderable.clickAndHover(
                displayName, lore,
                onClick = {
                    if (KeyboardManager.isModifierKeyDown()) {
                        data.removeItem(data.selectedBucket, internalName)
                        ChatUtils.chat("Removed $cleanName §efrom $name${if (data.selectedBucket != null) " (${data.selectedBucket})" else ""}")
                    } else {
                        modify {
                            it.toggleItemHide(data.selectedBucket, internalName)
                        }
                    }
                    update()

                }
            ) else Renderable.string(displayName)

            lists.addAsSingletonList(renderable)
        }
        if (hiddenItemTexts.size > 0) {
            val text = Renderable.hoverTips(" §7${hiddenItemTexts.size} cheap items are hidden.", hiddenItemTexts)
            lists.addAsSingletonList(text)
        }

        return profit
    }

    private fun buildLore(
        data: BucketedData,
        item: ItemTrackerData.TrackedItem,
        hidden: Boolean,
        newDrop: Boolean,
        internalName: NEUInternalName,
    ) = buildList {
        if (internalName == SKYBLOCK_COIN) {
            addAll(data.getCoinDescription(item))
        } else {
            addAll(data.getDescription(data.selectedBucket, item.timesGained))
        }
        add("")
        if (newDrop) {
            add("§aYou obtained this item recently.")
            add("")
        }
        add("§eClick to " + (if (hidden) "show" else "hide") + "!")
        add("§eControl + Click to remove this item!")
        if (SkyHanniMod.feature.dev.debug.enabled) {
            add("")
            add("§7${internalName}")
        }
    }

    fun addTotalProfit(profit: Double, totalAmount: Long, action: String): Renderable {
        val profitFormat = profit.toLong().addSeparators()
        val profitPrefix = if (profit < 0) "§c" else "§6"

        val tips = if (totalAmount > 0) {
            val profitPerCatch = profit / totalAmount
            val profitPerCatchFormat = profitPerCatch.shortFormat()
            listOf("§7Profit per $action: $profitPrefix$profitPerCatchFormat")
        } else emptyList()

        val text = "§eTotal Profit: $profitPrefix$profitFormat coins"
        return Renderable.hoverTips(text, tips)
    }
}
