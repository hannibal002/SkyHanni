package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SackDisplay {

    companion object {
        var inInventory = false
        var isRuneSack = false
    }

    private val config get() = SkyHanniMod.feature.inventory
    private var display = listOf<List<Any>>()
    private val sackItem = mutableMapOf<Pair<String, String>, Pair<String, String>>()
    private val runeItem = mutableMapOf<Pair<String, String>, String>()
    private val sackPattern = "^(.* Sack|Enchanted .* Sack)$".toPattern()

    private val numPattern =
        "(?:(?:§[0-9a-f](?<level>I{1,3})§7:)?|(?:§7Stored:)?) (?<color>§[0-9a-f])(?<stored>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)§7/(?<total>\\d+(?:\\.\\d+)?(?:,\\d+)?[kKmM]?)".toPattern()


    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (inInventory) {
            config.sackDisplayPosition.renderStringsAndItems(
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

        if (sackItem.isNotEmpty()) {
            val sortedPairs = sackItem.entries.sortedByDescending { it.value.first.formatNumber().toInt() }
            newDisplay.addAsSingletonList("Items in Sacks")
            for ((name, pair) in sortedPairs) {
                val list = mutableListOf<Any>()
                val colorCode = name.first
                val itemName = name.second;
                val internalName = NEUItems.getInternalName(itemName)
                val itemstack = NEUItems.getItemStack(internalName)
                list.add(" §7- ")
                list.add(itemstack)
                list.add(" $itemName: ")
                val item = when (config.showFullNumber) {
                    0 -> "$colorCode${pair.first}§7/§b${pair.second}"
                    1 -> "$colorCode${NumberUtil.format(pair.first.formatNumber())}§7/§b${pair.second}"
                    2 -> "$colorCode${pair.first}§7/§b${String.format("%,d", pair.second.formatNumber())}"
                    else -> "$colorCode${pair.first}§7/§b${pair.second}"
                }
                list.add(item)
                if (colorCode == "§a") // §a = Full, §e = Not full, §7 = Empty
                    list.add(" §c§l(Full!)")
                newDisplay.add(list)
            }
        }

        if (runeItem.isNotEmpty()) {
            newDisplay.addAsSingletonList("Items in Sacks")
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
        if (isEnabled() && !isRuneDisplayEnabled() && inventoryName == "Runes Sack") return
        val match = sackPattern.matcher(inventoryName).matches()
        if (!match) return
        val stacks = event.inventoryItems
        if (inventoryName == "Runes sacks") isRuneSack = true
        inInventory = true
        var runeLine = "";
        for ((_, stack) in stacks) {
            val name = stack.name ?: continue
            val lore = stack.getLore()
            loop@ for (line in lore) {
                numPattern.matchMatcher(line) {
                    val stored = group("stored")
                    val total = group("total")
                    val color = group("color")
                    val colored = Pair(color, name)
                    val item = Pair(stored, total)
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

    private fun isEnabled() = config.showSackDisplay
    private fun isRuneDisplayEnabled() = config.showRuneSackDisplay
}