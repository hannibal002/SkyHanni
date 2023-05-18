package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
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
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SackDisplay {

    companion object {
        var inInventory = false
        var isRuneSack = false
    }

    private val config get() = SkyHanniMod.feature.inventory.sackDisplay
    private var display = listOf<List<Any>>()
    private val sackItem = mutableMapOf<Pair<String, String>, Triple<String, String, Int>>()
    private val runeItem = mutableMapOf<Pair<String, String>, String>()
    private val sackPattern = "^(.* Sack|Enchanted .* Sack)$".toPattern()

    private val numPattern =
        "(?:(?:§[0-9a-f](?<level>I{1,3})§7:)?|(?:§7Stored:)?) (?<color>§[0-9a-f])(?<stored>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)§7/(?<total>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)".toPattern()


    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (inInventory) {
            config.position.renderStringsAndItems(
                display,
                extraSpace = 5,
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

            newDisplay.addAsSingletonList("§7Items in Sacks:")
            for ((name, triple) in sortedPairs) {
                val list = mutableListOf<Any>()
                val (colorCode, itemName) = name
                val internalName = NEUItems.getInternalName(itemName)
                val itemStack = NEUItems.getItemStack(internalName)
                val (stored, total, price) = triple
                list.add(" §7- ")
                list.add(itemStack)
                list.add(" $itemName: ")
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
            for ((name, runeLine) in runeItem) {
                val list = mutableListOf<Any>()
                val colorCode = name.first
                val itemName = name.second
                list.add(" §7- ")
                list.add(" $itemName $runeLine")
                if (colorCode == "§a")
                    list.add(" §c§l(Full!)")
                newDisplay.add(list)
            }
        }

        return newDisplay
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
        isRuneSack = false
        sackItem.clear()
        runeItem.clear()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (!isEnabled()) return
        val inventoryName = event.inventoryName
        if (!isRuneDisplayEnabled() && inventoryName == "Runes Sack") return
        val match = sackPattern.matcher(inventoryName).matches()
        if (!match) return
        val stacks = event.inventoryItems
        isRuneSack = inventoryName == "Runes sacks"
        inInventory = true
        var runeLine = ""
        for ((_, stack) in stacks) {
            val name = stack.name ?: continue
            val lore = stack.getLore()
            loop@ for (line in lore) {
                numPattern.matchMatcher(line) {
                    val stored = group("stored")
                    val total = group("total")
                    val color = group("color")
                    val internalName = NEUItems.getInternalName(name)

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
                    NEUItems.getPrice("", true)
                    val colored = Pair(color, name)
                    val item = Triple(stored, total, price)
                    if (group("level") != null) {
                        val level = group("level")
                        if (level == "I") {
                            runeLine = "§cI $color$stored§7/§b$total"
                            continue@loop
                        }
                        if (level == "II") {
                            runeLine += "§7, §cII $color$stored§7/§b$total"
                            continue@loop
                        }
                        if (level == "III") {
                            runeLine += "§7, §cIII $color$stored§7/§b$total"
                        }

                        runeItem.put(colored, runeLine)
                    } else {
                        sackItem.put(colored, item)
                    }

                }
            }

        }

        update()
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
    private fun isRuneDisplayEnabled() = config.showRunes
}