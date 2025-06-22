package at.hannibal2.skyhanni.features.inventory.sacks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.isBazaarItem
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemPriceSource
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.getNumberedName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.MinMaxNumber
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addSearchString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.renderables.SearchTextInput
import at.hannibal2.skyhanni.utils.renderables.buildSearchBox
import at.hannibal2.skyhanni.utils.renderables.toSearchable

// shows the sack items and price in sacks while not in the sacks
@SkyHanniModule
object OutsideSackValue {
    private val config get() = SkyHanniMod.feature.inventory.outsideSackValue

    private val textInput = SearchTextInput()
    private val scrollValue = ScrollValue()

    private var display = listOf<Renderable>()
    private var advanced = false

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { SkyBlockUtils.inSkyBlock && config.enabled && !SackApi.inventory.isInside() },
            onRender = {
                config.position.renderRenderables(display, posLabel = "Outside Sacks Value")
            },
        )
    }

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (event.repeatSeconds(5)) {
            update()
        }
    }

    @HandleEvent(SackChangeEvent::class)
    fun onSackChange() {
        update()
    }

    @HandleEvent(InventoryOpenEvent::class)
    fun onInventoryOpen() {
        if (advanced) {
            advanced = false
            reset()
        }
    }

    private fun reset() {
        scrollValue.setValue(0.0)
        update()
    }

    @HandleEvent(ProfileJoinEvent::class)
    fun onProfileJoin() {
        advanced = false
        reset()
    }

    private fun update() {
        display = if (advanced) {
            createAdvancedDisplay()
        } else {
            createSimpleDisplay()
        }
    }

    private fun createAdvancedDisplay(): List<Renderable> {
        val (label, data) = calculateData()
        val result = buildList {
            add(
                Renderable.clickable(
                    Renderable.string(label),
                    tips = buildList {
                        add(label)
                        add("")
                        add("§eLeft click to show less infos!")
                        add("§eRight click to open sacks!")
                    },
                    onAnyClick = onAnyClick(),
                ),
            )

        }.map { it.toSearchable() }.toMutableList()
        val tableData = mutableMapOf<List<Renderable>, String>()

        for (sackData in data) {
            val list = listOf(
                Renderable.hoverTips(sackData.sackName, tips = sackData.lore),
                Renderable.hoverTips(sackData.sackPrice.toString(), tips = sackData.lore),
            )
            tableData[list] = "${sackData.sackName},${sackData.itemNames.joinToString(",")}"
        }
        if (tableData.isEmpty()) {
            result.addSearchString("§cNo Items in sacks!")
        }
        Renderable.searchableScrollable(
            tableData,
            key = 99,
            lines = 8,
            textInput = textInput,
            scrollValue = scrollValue,
            velocity = 5.0,
        )?.let {
            result.add(it.toSearchable())
        }

        return listOf(result.buildSearchBox(textInput))
    }

    private fun Long.formatItemAmount(): String {
        return "§7(${addSeparators()} items)"
    }

    private fun createSimpleDisplay(): List<Renderable> {
        val (label, data) = calculateData()
        return listOf(
            Renderable.clickable(
                Renderable.string(label),
                tips = buildList {
                    add(label)
                    add("")
                    if (data.isEmpty()) {
                        add("§cNo Items in sacks!")
                    } else {
                        addAll(data.map { "${it.sackName} ${it.sackPrice}" })
                    }
                    add("")
                    add("§eLeft click to show more infos!")
                    add("§eRight click to open sacks!")
                },
                onAnyClick = onAnyClick(),
            ),
        )
    }

    private fun onAnyClick() = mapOf(
        KeyboardManager.RIGHT_MOUSE to {
            HypixelCommands.sacks()
        },
        KeyboardManager.LEFT_MOUSE to {
            advanced = !advanced
            reset()
        },
    )

    class SackData(val sackName: String, val sackPrice: MinMaxNumber, val itemNames: List<String>, val lore: List<String>)

    private fun calculateData(): Pair<String, List<SackData>> {
        val sackForItem = mutableMapOf<NeuInternalName, String>()
        for ((name, items) in SackApi.sacks) {
            for (item in items) {
                sackForItem[item] = name
            }
        }

        var totalAmount = 0L
        var totalPrice = MinMaxNumber(0.0, 0.0)
        val pricePerSack = mutableMapOf<String, MinMaxNumber>()
        val amountPerSack = mutableMapOf<String, Long>()
        val itemPricesPerSack = mutableMapOf<String, MutableMap<NeuInternalName, MinMaxNumber>>()

        for ((internalName, data) in SackApi.sackData) {
            val amount = data.amount
            if (amount == 0) continue
            if (!internalName.isBazaarItem()) continue

            val priceMin = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_SELL) * amount
            val priceMax = internalName.getPrice(ItemPriceSource.BAZAAR_INSTANT_BUY) * amount
            val price = MinMaxNumber(priceMin, priceMax)
            // edge case for invalid items that fall in "No Sack"
            val sack = sackForItem[internalName] ?: "§cNo"
            pricePerSack.addOrPut(sack, price)
            amountPerSack.addOrPut(sack, amount.toLong())
            itemPricesPerSack.getOrPut(sack) { mutableMapOf() }.addOrPut(internalName, price)
            totalAmount += amount
            totalPrice += price
        }

        val datas = mutableListOf<SackData>()
        for ((sack, sackPrice) in pricePerSack.entries.sortedByDescending { it.value.min }) {
            val itemPrices = itemPricesPerSack[sack] ?: continue
            val lore = mutableListOf<String>()
            val name = "§a$sack Sack"
            lore.add("$name $sackPrice")
            val sackAmount = amountPerSack[sack] ?: 0
            lore.add(sackAmount.formatItemAmount())
            lore.add("")
            val itemNames = mutableListOf<String>()
            for ((item, price) in itemPrices.entries.sortedByDescending { it.value.min }) {
                val amount = SackApi.sackData[item]?.amount ?: error("no amount for $item")
                lore.add("${item.getNumberedName(amount)}§8: $price")
                itemNames.add(item.repoItemName.removeColor())
            }
            datas.add(SackData(name, sackPrice, itemNames, lore))
        }

        return "$totalPrice §ein sacks ${totalAmount.formatItemAmount()}" to datas
    }
}
