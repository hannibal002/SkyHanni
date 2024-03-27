package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.inventory.SackDisplayConfig
import at.hannibal2.skyhanni.config.features.inventory.SackDisplayConfig.NumberFormatEntry
import at.hannibal2.skyhanni.config.features.inventory.SackDisplayConfig.PriceFormatEntry
import at.hannibal2.skyhanni.config.features.inventory.SackDisplayConfig.SortingTypeEntry
import at.hannibal2.skyhanni.data.SackAPI
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addButton
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
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

    @SubscribeEvent
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!SackAPI.inSackInventory) return
        if (!config.highlightFull) return
        for (slot in InventoryUtils.getItemsInOpenChest()) {
            val lore = slot.stack.getLore()
            if (lore.any { it.startsWith("§7Stored: §a") }) {
                slot highlight LorenzColor.RED
            }
        }
    }

    fun update(savingSacks: Boolean) {
        display = drawDisplay(savingSacks)
    }

    private fun drawDisplay(savingSacks: Boolean): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        var totalPrice = 0L
        var rendered = 0
        var totalMagmaFish = 0L
        SackAPI.getSacksData(savingSacks)

        val sackItems = SackAPI.sackItem.toList()
        if (sackItems.isNotEmpty()) {
            val sortedPairs: MutableMap<String, SackAPI.SackOtherItem> = when (config.sortingType) {
                SortingTypeEntry.DESC_STORED -> sackItems.sortedByDescending { it.second.stored.formatNumber() }
                SortingTypeEntry.ASC_STORED -> sackItems.sortedBy { it.second.stored.formatNumber() }
                SortingTypeEntry.DESC_PRICE -> sackItems.sortedByDescending { it.second.price }
                SortingTypeEntry.ASC_PRICE -> sackItems.sortedBy { it.second.price }
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

                val (internalName, colorCode, stored, total, price, magmaFish) = item
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
                            NumberFormatEntry.DEFAULT -> "$colorCode${stored}§7/§b${total}"
                            NumberFormatEntry.FORMATTED -> "$colorCode${NumberUtil.format(stored.formatNumber())}§7/§b${total}"
                            NumberFormatEntry.UNFORMATTED -> "$colorCode${stored}§7/§b${
                                total.formatNumber().addSeparators()
                            }"

                            else -> "$colorCode${stored}§7/§b${total}"
                        }
                    )

                    if (colorCode == "§a") add(" §c§l(Full!)")
                    if (SackAPI.isTrophySack && magmaFish > 0) {
                        totalMagmaFish += magmaFish
                        add(
                            Renderable.hoverTips(
                                " §7(§d${magmaFish} ",
                                listOf(
                                    "§6Magmafish: §b${magmaFish.addSeparators()}",
                                    "§6Magmafish value: §b${price / magmaFish}",
                                    "§6Magmafish per: §b${magmaFish / stored.formatNumber()}"
                                )
                            )
                        )
                        add("MAGMA_FISH".asInternalName().getItemStack())
                        add("§7)")
                    }
                    if (config.showPrice && price != 0L) add(" §7(§6${format(price)}§7)")
                })
                rendered++
            }

            if (SackAPI.isTrophySack) newDisplay.addAsSingletonList("§cTotal Magmafish: §6${totalMagmaFish.addSeparators()}")

            val name = SortType.entries[config.sortingType.ordinal].longName // todo avoid ordinal
            newDisplay.addAsSingletonList("§7Sorted By: §c$name")

            newDisplay.addSelector<SortType>(" ",
                getName = { type -> type.shortName },
                isCurrent = { it.ordinal == config.sortingType.ordinal }, // todo avoid ordinal
                onChange = {
                    config.sortingType = SortingTypeEntry.entries[it.ordinal] // todo avoid ordinals
                    update(false)
                })

            newDisplay.addButton(
                prefix = "§7Number format: ",
                getName = NumberFormat.entries[config.numberFormat.ordinal].DisplayName, // todo avoid ordinal
                onChange = {
                    // todo avoid ordinal
                    config.numberFormat =
                        NumberFormatEntry.entries[(config.numberFormat.ordinal + 1) % 3]
                    update(false)
                }
            )

            if (config.showPrice) {
                newDisplay.addSelector<PriceFrom>(" ",
                    getName = { type -> type.displayName },
                    isCurrent = { it.ordinal == config.priceFrom.ordinal }, // todo avoid ordinal
                    onChange = {
                        config.priceFrom = SackDisplayConfig.PriceFrom.entries[it.ordinal] // todo avoid ordinal
                        update(false)
                    })
                newDisplay.addButton(
                    prefix = "§7Price Format: ",
                    getName = PriceFormat.entries[config.priceFormat.ordinal].displayName, // todo avoid ordinal
                    onChange = {
                        // todo avoid ordinal
                        config.priceFormat =
                            PriceFormatEntry.entries[(config.priceFormat.ordinal + 1) % 2]
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
                val (internalName, rough, flawed, fine, roughprice, flawedprice, fineprice) = gem
                newDisplay.add(buildList {
                    add(" §7- ")
                    add(internalName.getItemStack())
                    add(Renderable.optionalLink("$name: ", {
                        BazaarApi.searchForBazaarItem(name.dropLast(1))
                    }) { !NEUItems.neuHasFocus() })
                    add(" ($rough-§a$flawed-§9$fine)")
                    val price = roughprice + flawedprice + fineprice
                    totalPrice += price
                    if (config.showPrice && price != 0L) add(" §7(§6${format(price)}§7)")
                })
            }
            if (config.showPrice) newDisplay.addAsSingletonList("§eTotal price: §6${format(totalPrice)}")
        }
        return newDisplay
    }

    private fun format(price: Long) =
        if (config.priceFormat == PriceFormatEntry.FORMATTED) NumberUtil.format(price) else price.addSeparators()

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
        NPC("NPC Price"),
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

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(15, "inventory.sackDisplay.numberFormat") { element ->
            ConfigUtils.migrateIntToEnum(element, NumberFormatEntry::class.java)
        }
        event.transform(15, "inventory.sackDisplay.priceFormat") { element ->
            ConfigUtils.migrateIntToEnum(element, PriceFormatEntry::class.java)
        }
        event.transform(15, "inventory.sackDisplay.priceFrom") { element ->
            ConfigUtils.migrateIntToEnum(element, SackDisplayConfig.PriceFrom::class.java)
        }
        event.transform(15, "inventory.sackDisplay.sortingType") { element ->
            ConfigUtils.migrateIntToEnum(element, SortingTypeEntry::class.java)
        }
    }
}
