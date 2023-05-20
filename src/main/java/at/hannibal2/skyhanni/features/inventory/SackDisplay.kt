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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SackDisplay {

    companion object {
        var inInventory = false
        var isRuneSack = false
        var isGemstoneSack = false
    }

    private val config get() = SkyHanniMod.feature.inventory.sackDisplay
    private var display = listOf<List<Any>>()
    private val sackItem = mutableMapOf<Pair<String, String>, Triple<String, String, Int>>()
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
        " (?:§[0-9a-f])(?<gemrarity>[A-z]*): §[0-9a-f](?<stored>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?) §[0-9a-f]\\((?:\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)\\)".toPattern()


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
            val sortedPairs = when (config.sortingType) {
                0 -> sackItem.entries.sortedByDescending { it.value.first.formatNumber().toInt() }
                1 -> sackItem.entries.sortedBy { it.value.first.formatNumber().toInt() }
                2 -> sackItem.entries.sortedByDescending { it.value.third }
                3 -> sackItem.entries.sortedBy { it.value.third }
                else -> {
                    sackItem.entries.sortedByDescending { it.value.first.formatNumber().toInt() }
                }
            }

            newDisplay.addAsSingletonList("§7Items in Sacks: §o(Rendering ${if (config.itemToShow > sortedPairs.size) sortedPairs.size else config.itemToShow} of ${sortedPairs.size} items)")
            for ((name, triple) in sortedPairs) {
                if (rendered >= config.itemToShow) continue
                val list = mutableListOf<Any>()
                val (colorCode, itemName) = name
                val internalName = NEUItems.getInternalName(itemName)
                val itemStack = NEUItems.getItemStack(internalName)
                val (stored, total, price) = triple
                list.add(" §7- ")
                list.add(itemStack)

                list.add(Renderable.optionalLink("$itemName: ", {
                    if (!NEUItems.neuHasFocus() && !LorenzUtils.noTradeMode) {
                        LorenzUtils.sendCommandToServer("bz ${itemName.removeColor()}")
                    }
                }) { !NEUItems.neuHasFocus() })

                val item = when (config.numberFormat) {
                    0 -> "$colorCode${stored}§7/§b${total}"
                    1 -> "$colorCode${NumberUtil.format(stored.formatNumber())}§7/§b${total}"
                    2 -> "$colorCode${stored}§7/§b${total.formatNumber().toInt().addSeparators()}"
                    else -> "$colorCode${stored}§7/§b${total}"
                }

                list.add(item)
                if (colorCode == "§a") // §a = Full, §e = Not full, §7 = Empty
                    list.add(" §c§l(Full!)")

                val format: String = when (config.priceFormat) {
                    0 -> NumberUtil.format(price)
                    1 -> price.addSeparators()
                    else -> ""
                }
                if (config.showPrice && format.isNotEmpty() && format != "0")
                    list.add(" §7(§6$format§7)")

                totalPrice += price
                rendered++
                newDisplay.add(list)
            }
            val finalPrice: String = when (config.priceFormat) {
                0 -> NumberUtil.format(totalPrice)
                1 -> totalPrice.addSeparators()
                else -> ""
            }
            if (config.showPrice && finalPrice.isNotEmpty())
                newDisplay.addAsSingletonList("§eTotal price: §6$finalPrice")

        }

        if (runeItem.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Items in Sacks:")
            for ((name, rune) in runeItem) {
                val list = mutableListOf<Any>()
                val (lv1, lv2, lv3) = rune
                list.add(" §7- ")
                list.add(name)
                list.add(" §f(§e$lv1§7-§e$lv2§7-§e$lv3§f)")
                newDisplay.add(list)
            }
        }

        if (gemstoneItem.isNotEmpty()) {
            newDisplay.addAsSingletonList("§7Gemstones:")
            for (gemstone in gemstoneItem) {
                val list = mutableListOf<Any>()
                val (name, gem) = gemstone
                val (internalName, rough, flawed, fine, flawless, roughprice, flawedprice, fineprice, flawlessprice) = gem

                list.add(" §7- ")
                list.add(NEUItems.getItemStack(internalName))
                list.add(name)
                list.add(" ($rough-§a$flawed-§9$fine-§5$flawless)")
                val price = (roughprice + flawedprice + fineprice + flawlessprice)
                val finalPrice: String = when (config.priceFormat) {
                    0 -> NumberUtil.format(price)
                    1 -> price.addSeparators()
                    else -> ""
                }
                if (config.showPrice && finalPrice.isNotEmpty() && finalPrice != "0")
                    list.add(" §7(§6$finalPrice§7)")
                newDisplay.add(list)
            }
        }
        return newDisplay
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        isRuneSack = false
        isGemstoneSack = false
        sackItem.clear()
        runeItem.clear()
        gemstoneItem.clear()
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
            loop@ for (line in lore) {
                if (isGemstoneSack) {
                    gemstonePattern.matchMatcher(line) {
                        val rarity = group("gemrarity")
                        val stored = group("stored")
                        gem.internalName = gemstoneMap[name.removeColor()].toString()
                        if (gemstoneMap.containsKey(name.removeColor())) {
                            when (rarity) {
                                "Rough" -> {
                                    val internalName = "${rarity.uppercase()}_${name.uppercase().split(" ")[0].removeColor()}_GEM"
                                    gem.rough = stored
                                    gem.roughPrice = when (config.priceFrom) {
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
                                }

                                "Flawed" -> {
                                    val internalName = "${rarity.uppercase()}_${name.uppercase().split(" ")[0].removeColor()}_GEM"
                                    gem.flawed = stored
                                    gem.flawedPrice = when (config.priceFrom) {
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
                                }

                                "Fine" -> {
                                    val internalName = "${rarity.uppercase()}_${name.uppercase().split(" ")[0].removeColor()}_GEM"
                                    gem.fine = stored
                                    gem.finePrice = when (config.priceFrom) {
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
                                }

                                "Flawless" -> {
                                    val internalName = "${rarity.uppercase()}_${name.uppercase().split(" ")[0].removeColor()}_GEM"
                                    gem.flawless = stored
                                    gem.flawlessPrice = when (config.priceFrom) {
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
                                }
                            }
                            gemstoneItem[name] = gem
                        }
                    }
                } else {
                    numPattern.matchMatcher(line) {
                        val stored = group("stored")
                        val total = group("total")
                        val color = group("color")
                        val colored = Pair(color, name)
                        val internalName = stack.getInternalName()
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
                        val item = Triple(stored, total, price)

                        if (isRuneSack) {
                            val level = group("level")
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
                            sackItem.put(colored, item)
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
        var flawlessPrice: Int = 0
    )

    data class Rune(
        var lvl1: String = "0",
        var lvl2: String = "0",
        var lvl3: String = "0"
    )

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    private fun isRuneDisplayEnabled() = config.showRunes
}