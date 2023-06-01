package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SackDisplay {

    companion object {
        var inInventory = false
        var isRuneSack = false
        var isGemstoneSack = false
    }

    private val config get() = SkyHanniMod.feature.inventory.sackDisplay
    private var display = listOf<List<Any>>()
    private val sackItem = mutableMapOf<String, Item>()
    private val runeItem = mutableMapOf<String, Rune>()
    private val gemstoneItem = mutableMapOf<String, Gemstone>()
    private val sackPattern = "^(.* Sack|Enchanted .* Sack)$".toPattern()
    private val gemstoneMap = mapOf(
        "Jade Gemstones" to "ROUGH_JADE_GEM",
        "Amber Gemstones" to "ROUGH_AMBER_GEM",
        "Topaz Gemstones" to "ROUGH_TOPAZ_GEM",
        "Sapphire Gemstones" to "ROUGH_SAPPHIRE_GEM",
        "Amethyst Gemstones" to "ROUGH_AMETHYST_GEM",
        "Jasper Gemstones" to "ROUGH_JASPER_GEM",
        "Ruby Gemstones" to "ROUGH_RUBY_GEM",
        "Opal Gemstones" to "ROUGH_OPAL_GEM"
    )

    private val numPattern =
        "(?:(?:§[0-9a-f](?<level>I{1,3})§7:)?|(?:§7Stored:)?) (?<color>§[0-9a-f])(?<stored>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)§7/(?<total>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)".toPattern()
    private val gemstonePattern =
        " §[0-9a-f](?<gemrarity>[A-z]*): §[0-9a-f](?<stored>\\d+(?:\\.\\d+)?(?:(?:,\\d+)?)+[kKmM]?)(?: §[0-9a-f]\\(\\d+(?:\\.\\d+)?(?:(?:,\\d+)?)+[kKmM]?\\))?".toPattern()


    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (inInventory) {
            config.position.renderStringsAndItems(
                display,
                extraSpace = config.extraSpace,
                itemScale = 1.3,
                posLabel = "Sacks Items"
            )
        }
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun update() {
        updateDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        var totalPrice = 0
        var rendered = 0
        if (sackItem.isNotEmpty()) {
            val sortedPairs: MutableMap<String, Item> = when (config.sortingType) {
                0 -> sackItem.toList().sortedByDescending { it.second.stored.formatNumber() }.toMap().toMutableMap()
                1 -> sackItem.toList().sortedBy { it.second.stored.formatNumber() }.toMap().toMutableMap()
                2 -> sackItem.toList().sortedByDescending { it.second.price }.toMap().toMutableMap()
                3 -> sackItem.toList().sortedBy { it.second.price }.toMap().toMutableMap()
                else -> sackItem.toList().sortedByDescending { it.second.stored.formatNumber() }.toMap().toMutableMap()
            }

            sortedPairs.toList().forEach {
                if (it.second.stored == "0" && !config.showEmpty) {
                    sortedPairs.remove(it.first)
                }
            }

            newDisplay.addAsSingletonList("§7Items in Sacks: §o(Rendering ${if (config.itemToShow > sortedPairs.size) sortedPairs.size else config.itemToShow} of ${sortedPairs.size} items)")
            for ((itemName, item) in sortedPairs) {
                val (internalName, colorCode, stored, total, price) = item
                totalPrice += price
                if (rendered >= config.itemToShow) continue
                val list = mutableListOf<Any>()
                if (stored == "0" && !config.showEmpty) continue
                val itemStack = NEUItems.getItemStack(internalName)
                list.add(" §7- ")
                list.add(itemStack)

                list.add(Renderable.optionalLink("$itemName: ", {
                    if (!NEUItems.neuHasFocus() && !LorenzUtils.noTradeMode) {
                        LorenzUtils.sendCommandToServer("bz ${itemName.removeColor()}")
                    }
                }) { !NEUItems.neuHasFocus() })

                val displayItem = when (config.numberFormat) {
                    0 -> "$colorCode${stored}§7/§b${total}"
                    1 -> "$colorCode${NumberUtil.format(stored.formatNumber())}§7/§b${total}"
                    2 -> "$colorCode${stored}§7/§b${total.formatNumber().toInt().addSeparators()}"
                    else -> "$colorCode${stored}§7/§b${total}"
                }

                list.add(displayItem)
                if (colorCode == "§a") // §a = Full, §e = Not full, §7 = Empty
                    list.add(" §c§l(Full!)")

                if (config.showPrice && price != 0)
                    list.add(" §7(§6${format(price)}§7)")

                rendered++
                newDisplay.add(list)
            }

            newDisplay.add(buildList {
                add("§eSort by: §6Stored ")
                add(Renderable.optionalLink("§7[§bDESC§7] ", {
                    config.sortingType = 0
                    update()
                }) { !NEUItems.neuHasFocus() })
                add(Renderable.optionalLink("§7[§bASC§7] ", {
                    config.sortingType = 1
                    update()
                }) { !NEUItems.neuHasFocus() })

                if (config.showPrice) {
                    add("§6Price ")
                    add(Renderable.optionalLink("§7[§bDESC§7] ", {
                        config.sortingType = 2
                        update()
                    }) { !NEUItems.neuHasFocus() })
                    add(Renderable.optionalLink("§7[§bASC§7]", {
                        config.sortingType = 3
                        update()
                    }) { !NEUItems.neuHasFocus() })
                }
            })

            if (config.showPrice)
                newDisplay.addAsSingletonList("§eTotal price: §6${format(totalPrice)}")
        }

        if (runeItem.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Runes:")
            for ((name, rune) in runeItem) {
                val list = mutableListOf<Any>()
                val (stack, lv1, lv2, lv3) = rune
                list.add(" §7- ")
                stack?.let { list.add(it) }
                list.add(name)
                list.add(" §f(§e$lv1§7-§e$lv2§7-§e$lv3§f)")
                newDisplay.add(list)
            }
        }

        if (gemstoneItem.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Gemstones:")
            for ((name, gem) in gemstoneItem) {
                val (internalName, rough, flawed, fine, flawless, roughprice, flawedprice, fineprice, flawlessprice) = gem
                newDisplay.add(buildList {
                    add(" §7- ")
                    add(NEUItems.getItemStack(internalName))
                    add(Renderable.optionalLink("$name: ", {
                        if (!NEUItems.neuHasFocus() && !LorenzUtils.noTradeMode) {
                            LorenzUtils.sendCommandToServer("bz ${name.removeColor().dropLast(1)}")
                        }
                    }) { !NEUItems.neuHasFocus() })
                    add(" ($rough-§a$flawed-§9$fine-§5$flawless)")
                    val price = (roughprice + flawedprice + fineprice + flawlessprice)
                    totalPrice += price
                    if (config.showPrice && price != 0)
                        add(" §7(§6${format(price)}§7)")
                })
            }
            if (config.showPrice)
                newDisplay.addAsSingletonList("§eTotal price: §6${format(totalPrice)}")
        }
        return newDisplay
    }

    private fun format(price: Int) = if (config.priceFormat == 0) NumberUtil.format(price) else price.addSeparators()

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        isRuneSack = false
        isGemstoneSack = false
        runeItem.clear()
        gemstoneItem.clear()
        sackItem.clear()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        val inventoryName = event.inventoryName
        if (!isRuneDisplayEnabled() && inventoryName == "Runes Sack") return
        val match = sackPattern.matcher(inventoryName).matches()
        if (!match) return
        val stacks = event.inventoryItems
        isRuneSack = inventoryName == "Runes Sack"
        isGemstoneSack = inventoryName == "Gemstones Sack"
        inInventory = true
        for ((_, stack) in stacks) {
            val name = stack.name ?: continue
            val lore = stack.getLore()
            val gem = Gemstone()
            val rune = Rune()
            val item = Item()
            loop@ for (line in lore) {
                if (isGemstoneSack) {
                    println(line)
                    gemstonePattern.matchMatcher(line) {
                        val rarity = group("gemrarity")
                        val stored = group("stored")
                        gem.internalName = gemstoneMap[name.removeColor()].toString()
                        if (gemstoneMap.containsKey(name.removeColor())) {
                            val internalName =
                                "${rarity.uppercase()}_${name.uppercase().split(" ")[0].removeColor()}_GEM"

                            when (rarity) {
                                "Rough" -> {
                                    gem.rough = stored
                                    gem.roughPrice = calculatePrice(internalName, stored)
                                }

                                "Flawed" -> {
                                    gem.flawed = stored
                                    gem.flawedPrice = calculatePrice(internalName, stored)
                                }

                                "Fine" -> {
                                    gem.fine = stored
                                    gem.finePrice = calculatePrice(internalName, stored)
                                }

                                "Flawless" -> {
                                    gem.flawless = stored
                                    gem.flawlessPrice = calculatePrice(internalName, stored)
                                }
                            }
                            gemstoneItem[name] = gem
                        }
                    }
                } else {
                    numPattern.matchMatcher(line) {
                        val stored = group("stored")
                        val total = group("total")
                        val internalName = stack.getInternalName()
                        item.internalName = internalName
                        item.colorCode = group("color")
                        val price: Int = when (config.priceFrom) {
                            0 -> {
                                (NEUItems.getPrice(internalName) * stored.formatNumber()).toInt()
                            }

                            1 -> {
                                try {
                                    val bazaarData = BazaarApi.getBazaarDataByInternalName(internalName)
                                    (((bazaarData?.npcPrice?.toInt() ?: 0) * stored.formatNumber())).toInt()

                                } catch (e: Exception) {
                                    0
                                }
                            }

                            else -> 0
                        }
                        item.stored = stored
                        item.total = total
                        item.price = price

                        if (isRuneSack) {
                            val level = group("level")
                            rune.stack = stack
                            if (level == "I") {
                                rune.lvl1 = stored
                                continue@loop
                            }
                            if (level == "II") {
                                rune.lvl2 = stored
                                continue@loop
                            }
                            if (level == "III") {
                                rune.lvl3 = stored
                            }
                            runeItem.put(name, rune)
                        } else {
                            sackItem.put(name, item)
                        }
                    }
                }
            }
        }
        update()
    }


    data class Gemstone(
        var internalName: String = "",
        var rough: String = "0",
        var flawed: String = "0",
        var fine: String = "0",
        var flawless: String = "0",
        var roughPrice: Int = 0,
        var flawedPrice: Int = 0,
        var finePrice: Int = 0,
        var flawlessPrice: Int = 0,
    )

    data class Rune(
        var stack: ItemStack? = null,
        var lvl1: String = "0",
        var lvl2: String = "0",
        var lvl3: String = "0",
    )

    data class Item(
        var internalName: String = "",
        var colorCode: String = "",
        var stored: String = "0",
        var total: String = "0",
        var price: Int = 0,
    )

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    private fun isRuneDisplayEnabled() = config.showRunes

    private fun calculatePrice(internalName: String, stored: String) = when (config.priceFrom) {
        0 -> (NEUItems.getPrice(internalName) * stored.formatNumber()).toInt()

        1 -> try {
            val npcPrice = BazaarApi.getBazaarDataByInternalName(internalName)?.npcPrice ?: 0.0
            (npcPrice * stored.formatNumber()).toInt()
        } catch (e: Exception) {
            0
        }

        else -> 0
    }
}
