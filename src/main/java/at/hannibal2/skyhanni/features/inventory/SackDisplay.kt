package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addButton
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SackDisplay {
    private var display = emptyList<List<Any>>()
    private val config get() = SkyHanniMod.feature.inventory.sackDisplay


    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (SackAPI.inSackInventory) {
            if (!isEnabled()) return
            config.position.renderStringsAndItems(
                display, extraSpace = config.extraSpace, itemScale = 1.3, posLabel = "Sacks Items"
            )
        }
    }

    fun update(savingSacks: Boolean) {
        display = drawDisplay(savingSacks)
    }

    private fun drawDisplay(savingSacks: Boolean): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        var totalPrice = 0L
        var rendered = 0
        SackAPI.getSacksData(savingSacks)

        val sackItems = SackAPI.sackItem.toList()
        if (sackItems.isNotEmpty()) {
            val sortedPairs: MutableMap<String, SackAPI.SackOtherItem> = when (config.sortingType) {
                0 -> sackItems.sortedByDescending { it.second.stored.formatNumber() }
                1 -> sackItems.sortedBy { it.second.stored.formatNumber() }
                2 -> sackItems.sortedByDescending { it.second.price }
                3 -> sackItems.sortedBy { it.second.price }
                else -> sackItems.sortedByDescending { it.second.stored.formatNumber() }
            }.toMap().toMutableMap()

            sortedPairs.toList().forEach { (k, v) ->
                if (v.stored == "0" && !config.showEmpty) {
                    sortedPairs.remove(k)
                }
            }

            val amountShowing = if (config.itemToShow > sortedPairs.size) sortedPairs.size else config.itemToShow
            newDisplay.addAsSingletonList("§7Items in Sacks: §o(Rendering $amountShowing of ${sortedPairs.size} items)")
            for ((itemName, item) in sortedPairs) {
                val (internalName, colorCode, stored, total, price) = item
                totalPrice += price
                if (rendered >= config.itemToShow) continue
                if (stored == "0" && !config.showEmpty) continue
                val itemStack = internalName.getItemStack()
                newDisplay.add(buildList {
                    add(" §7- ")
                    add(itemStack)
                    if (!SackAPI.isTrophySack) add(Renderable.optionalLink("${itemName.replace("§k", "")}: ", {
                        BazaarApi.searchForBazaarItem(itemName)
                    }) { !NEUItems.neuHasFocus() })
                    else add("${itemName.replace("§k", "")}: ")

                    add(
                        when (config.numberFormat) {
                            0 -> "$colorCode${stored}§7/§b${total}"
                            1 -> "$colorCode${NumberUtil.format(stored.formatNumber())}§7/§b${total}"
                            2 -> "$colorCode${stored}§7/§b${total.formatNumber().toInt().addSeparators()}"
                            else -> "$colorCode${stored}§7/§b${total}"
                        }
                    )

                    if (colorCode == "§a") add(" §c§l(Full!)")
                    if (config.showPrice && price != 0L) add(" §7(§6${format(price)}§7)")
                })
                rendered++
            }

            val name = SortType.entries[config.sortingType].longName
            newDisplay.addAsSingletonList("§7Sorted By: §c$name")

            newDisplay.addSelector<SortType>(" ",
                getName = { type -> type.shortName },
                isCurrent = { it.ordinal == config.sortingType },
                onChange = {
                    config.sortingType = it.ordinal
                    update(false)
                })
            newDisplay.addButton(
                prefix = "§7Number format: ",
                getName = NumberFormat.entries[config.numberFormat].DisplayName,
                onChange = {
                    config.numberFormat = (config.numberFormat + 1) % 3
                    update(false)
                }
            )
            if (config.showPrice) {
                newDisplay.addSelector<PriceFrom>(" ",
                    getName = { type -> type.displayName },
                    isCurrent = { it.ordinal == config.priceFrom },
                    onChange = {
                        config.priceFrom = it.ordinal
                        update(false)
                    })
                newDisplay.addButton(
                    prefix = "§7Price Format: ",
                    getName = PriceFormat.entries[config.priceFormat].displayName,
                    onChange = {
                        config.priceFormat = (config.priceFormat + 1) % 2
                        update(false)
                    }
                )
                newDisplay.addAsSingletonList("§cTotal price: §6${format(totalPrice)}")
            }
        }

        if (SackAPI.runeItem.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Runes:")
            for ((name, rune) in SackAPI.runeItem) {
                val list = mutableListOf<Any>()
                val (stack, lv1, lv2, lv3) = rune
                list.add(" §7- ")
                stack?.let { list.add(it) }
                list.add(name)
                list.add(" §f(§e$lv1§7-§e$lv2§7-§e$lv3§f)")
                newDisplay.add(list)
            }
        }

        if (SackAPI.gemstoneItem.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Gemstones:")
            for ((name, gem) in SackAPI.gemstoneItem) {
                val (internalName, rough, flawed, fine, flawless, roughprice, flawedprice, fineprice, flawlessprice) = gem
                newDisplay.add(buildList {
                    add(" §7- ")
                    add(internalName.getItemStack())
                    add(Renderable.optionalLink("$name: ", {
                        BazaarApi.searchForBazaarItem(name.dropLast(1))
                    }) { !NEUItems.neuHasFocus() })
                    add(" ($rough-§a$flawed-§9$fine-§5$flawless)")
                    val price = (roughprice + flawedprice + fineprice + flawlessprice).toLong()
                    totalPrice += price
                    if (config.showPrice && price != 0L) add(" §7(§6${format(price)}§7)")
                })
            }
            if (config.showPrice) newDisplay.addAsSingletonList("§eTotal price: §6${format(totalPrice)}")
        }
        return newDisplay
    }

    private fun format(price: Long) = if (config.priceFormat == 0) NumberUtil.format(price) else price.addSeparators()

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    enum class SortType(val shortName: String, val longName: String) {
        STORED_DESC("Stored D", "Stored Descending"),
        STORED_ASC("Stored A", "Stored Ascending"),
        PRICE_DESC("Price D", "Price Descending"),
        PRICE_ASC("Price A", "Price Ascending"),
        ;
    }

    enum class PriceFrom(val displayName: String) {
        BAZAAR("Bazaar Price"),
        NPC("Npc Price"),
        ;
    }

    enum class PriceFormat(val displayName: String) {
        FORMATED("Formatted"),
        UNFORMATED("Unformatted")
        ;
    }

    enum class NumberFormat(val DisplayName: String) {
        DEFAULT("Default"),
        FORMATTED("Formatted"),
        UNFORMATTED("Unformatted")
    }
}