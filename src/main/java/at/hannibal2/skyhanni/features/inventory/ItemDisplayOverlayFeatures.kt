package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class ItemDisplayOverlayFeatures {

    private val wishingCompassPattern = Pattern.compile("§7Remaining Uses: §e(.*)§8/§e3")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        val name = item.cleanName()

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(0)) {
            when (name) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(1)) {
            if (name.matchRegex("(.*)Master Skull - Tier .")) {
                return name.substring(name.length - 1)
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(2)) {
            if (name.contains("Golden ") || name.contains("Diamond ")) {
                when {
                    name.contains("Bonzo") -> return "1"
                    name.contains("Scarf") -> return "2"
                    name.contains("Professor") -> return "3"
                    name.contains("Thorn") -> return "4"
                    name.contains("Livid") -> return "5"
                    name.contains("Sadan") -> return "6"
                    name.contains("Necron") -> return "7"
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(3)) {
            if (name.startsWith("New Year Cake (")) {
                return "§b" + name.between("(Year ", ")")
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(4)) {
            val chestName = InventoryUtils.openInventoryName()
            if (!chestName.endsWith("Sea Creature Guide")) {
                if (ItemUtils.isPet(name)) {
                    val level = name.between("Lvl ", "] ").toInt()
                    if (level != ItemUtils.maxPetLevel(name)) {
                        return "$level"
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(5)) {
            if (name.contains(" Minion ")) {
                if (item.getLore().any { it.contains("Place this minion") }) {
                    val array = name.split(" ")
                    val last = array[array.size - 1]
                    return last.romanToDecimal().toString()
                }
            }
        }

        if (SkyHanniMod.feature.inventory.displaySackName) {
            if (ItemUtils.isSack(name)) {
                val sackName = grabSackName(name)
                return (if (name.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(7)) {
            if (name.contains("Wishing Compass")) {
                for (line in item.getLore()) {
                    val matcher = wishingCompassPattern.matcher(line)
                    if (matcher.matches()) {
                        val uses = matcher.group(1)
                        if (uses != "3") {
                            return uses
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(8)) {
            if (name.contains("Kuudra Key")) {
                return when (name) {
                    "Kuudra Key" -> "§a1"
                    "Hot Kuudra Key" -> "§22"
                    "Burning Kuudra Key" -> "§e3"
                    "Fiery Kuudra Key" -> "§64"
                    "Infernal Kuudra Key" -> "§c5"
                    else -> "§4?"
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(9)) {
            if (InventoryUtils.openInventoryName() == "Your Skills") {
                if (item.getLore().any { it.contains("Click to view!") }) {
                    item.name?.let {
                        if (!it.contains("Dungeon")) {
                            val text = it.split(" ").last()
                            return "" + text.romanToDecimalIfNeeded()
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(10)) {
            if (InventoryUtils.openInventoryName().endsWith(" Collections")) {
                if (item.getLore().any { it.contains("Click to view!") }) {
                    item.name?.let {
                        if (it.startsWith("§e")) {
                            val text = it.split(" ").last()
                            return "" + text.romanToDecimalIfNeeded()
                        }
                    }
                }
            }
        }
        return ""
    }

    private fun grabSackName(name: String): String {
        val split = name.split(" ")
        val text = split[0]
        for (line in arrayOf("Large", "Medium", "Small", "Enchanted")) {
            if (text == line) return grabSackName(name.substring(text.length + 1))
        }
        return text
    }
}