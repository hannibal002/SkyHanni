package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.addSelector
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
        var isTrophySack = false
        var sackRarity: TrophyRarity? = null
    }

    private val config get() = SkyHanniMod.feature.inventory.sackDisplay
    private var display = emptyList<List<Any>>()
    private val sackItem = mutableMapOf<String, Item>()
    private val runeItem = mutableMapOf<String, Rune>()
    private val gemstoneItem = mutableMapOf<String, Gemstone>()
    private val sackPattern = "^(.* Sack|Enchanted .* Sack)$".toPattern()
    private val stackList = mutableMapOf<Int, ItemStack>()
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
            "(?:(?:§[0-9a-f](?<level>I{1,3})§7:)?|(?:§7Stored:)?) (?<color>§[0-9a-f])(?<stored>[0-9.,kKmMbB]+)§7/(?<total>\\d+(?:[0-9.,]+)?[kKmMbB]?)".toPattern()
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

    private fun update() {
        display = drawDisplay()
    }

    private fun init() {
        for ((_, stack) in stackList) {
            val name = stack.name ?: continue
            val lore = stack.getLore()
            val gem = Gemstone()
            val rune = Rune()
            val item = Item()
            loop@ for (line in lore) {
                if (isGemstoneSack) {
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
                        val internalName = stack.getInternalName_old()
                        item.internalName = internalName
                        item.colorCode = group("color")
                        item.stored = stored
                        item.total = total
                        if (isTrophySack) {
                            val trophyName = internalName.lowercase()
                                .substringBeforeLast("_").replace("_", "")
                            val filletValue = TrophyFishManager.getInfoByName(trophyName)?.getFilletValue(sackRarity!!) ?: 0
                            val storedNumber = stored.formatNumber().toInt()
                            item.price = calculatePrice("MAGMA_FISH", (filletValue * storedNumber).toString())
                        } else {
                            item.price = if (calculatePrice(internalName, stored) < 0) 0 else calculatePrice(internalName, stored)
                        }
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
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        var totalPrice = 0
        var rendered = 0
        init()

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

            val amountShowing = if (config.itemToShow > sortedPairs.size) sortedPairs.size else config.itemToShow
            newDisplay.addAsSingletonList("§7Items in Sacks: §o(Rendering $amountShowing of ${sortedPairs.size} items)")
            for ((itemName, item) in sortedPairs) {
                val (internalName, colorCode, stored, total, price) = item
                totalPrice += price
                if (rendered >= config.itemToShow) continue
                if (stored == "0" && !config.showEmpty) continue
                val itemStack = NEUItems.getItemStack(internalName)
                newDisplay.add(buildList {
                    add(" §7- ")
                    add(itemStack)
                    if (!isTrophySack)
                        add(Renderable.optionalLink("${itemName.replace("§k", "")}: ", {
                            BazaarApi.searchForBazaarItem(itemName)
                        }) { !NEUItems.neuHasFocus() })
                    else
                        add("${itemName.replace("§k", "")}: ")

                    add(
                            when (config.numberFormat) {
                                0 -> "$colorCode${stored}§7/§b${total}"
                                1 -> "$colorCode${NumberUtil.format(stored.formatNumber())}§7/§b${total}"
                                2 -> "$colorCode${stored}§7/§b${total.formatNumber().toInt().addSeparators()}"
                                else -> "$colorCode${stored}§7/§b${total}"
                            }
                    )

                    if (colorCode == "§a")
                        add(" §c§l(Full!)")
                    if (config.showPrice && price != 0)
                        add(" §7(§6${format(price)}§7)")
                })
                rendered++
            }

            val name = SortType.entries[config.sortingType].longName
            newDisplay.addAsSingletonList("§7Sorted By: §c$name")

            newDisplay.addSelector<SortType>(
                " ",
                getName = { type -> type.shortName },
                isCurrent = { it.ordinal == config.sortingType },
                onChange = {
                    config.sortingType = it.ordinal
                    update()
                })

            if (config.showPrice) {
                newDisplay.addAsSingletonList("§cTotal price: §6${format(totalPrice)}")
                newDisplay.addSelector<PriceFrom>(
                    " ",
                    getName = { type -> type.displayName },
                    isCurrent = { it.ordinal == config.priceFrom },
                    onChange = {
                        config.priceFrom = it.ordinal
                        update()
                    })
            }
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
                        BazaarApi.searchForBazaarItem(name.dropLast(1))
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
        isTrophySack = false
        runeItem.clear()
        gemstoneItem.clear()
        sackItem.clear()
        stackList.clear()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        val inventoryName = event.inventoryName
        if (!isRuneDisplayEnabled() && inventoryName == "Runes Sack") return
        val match = sackPattern.matcher(inventoryName).matches()
        if (!match) return
        val stacks = event.inventoryItems
        isRuneSack = inventoryName == "Runes Sack"
        isGemstoneSack = inventoryName == "Gemstones Sack"
        isTrophySack = inventoryName.contains("Trophy Fishing Sack")
        sackRarity = inventoryName.getTrophyRarity()
        inInventory = true
        stackList.putAll(stacks)
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
        0 -> {
            (NEUItems.getPrice(internalName, true) * stored.formatNumber()).toInt().let {
                if (it < 0)
                    0
                else it
            }
        }

        1 -> try {
            val npcPrice = BazaarApi.getBazaarDataByInternalName(internalName)?.npcPrice ?: 0.0
            (npcPrice * stored.formatNumber()).toInt()
        } catch (e: Exception) {
            0
        }

        else -> 0
    }

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

    private fun String.getTrophyRarity(): TrophyRarity? {
        return if (this.startsWith("Bronze"))
            TrophyRarity.BRONZE
        else
            if (this.startsWith("Silver"))
                TrophyRarity.SILVER
            else null
    }
}
