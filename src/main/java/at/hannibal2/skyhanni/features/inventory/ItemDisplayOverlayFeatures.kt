package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.pests.PestAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemDisplayOverlayFeatures {
    private val config get() = SkyHanniMod.feature.inventory
    private val rancherBootsSpeedCapPattern = "§7Current Speed Cap: §a(?<cap>.*)".toPattern()
    private val petLevelPattern = "\\[Lvl (?<level>.*)] .*".toPattern()
    private val gardenVacuumPatterm = "§7Vacuum Bag: §6(?<amount>\\d*) Pests?".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        val itemName = item.cleanName()

        val itemNumberAsStackSize = config.itemNumberAsStackSize
        if (itemNumberAsStackSize.contains(0)) {
            when (itemName) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (itemNumberAsStackSize.contains(1) && itemName.matchRegex("(.*)Master Skull - Tier .")) {
            return itemName.substring(itemName.length - 1)
        }

        if (itemNumberAsStackSize.contains(2) && (itemName.contains("Golden ") || itemName.contains(
                "Diamond "
            ))
        ) {
            when {
                itemName.contains("Bonzo") -> return "1"
                itemName.contains("Scarf") -> return "2"
                itemName.contains("Professor") -> return "3"
                itemName.contains("Thorn") -> return "4"
                itemName.contains("Livid") -> return "5"
                itemName.contains("Sadan") -> return "6"
                itemName.contains("Necron") -> return "7"
            }
        }

        if (itemNumberAsStackSize.contains(3) && itemName.startsWith("New Year Cake (")) {
            return "§b" + itemName.between("(Year ", ")")
        }

        if (itemNumberAsStackSize.contains(4)) {
            val chestName = InventoryUtils.openInventoryName()
            if (!chestName.endsWith("Sea Creature Guide") && ItemUtils.isPet(itemName)) {
                petLevelPattern.matchMatcher(itemName) {
                    val rawLevel = group("level")
                    val level = rawLevel.toIntOrNull()
                        ?: throw IllegalStateException("pet level not found for item name '$itemName'")
                    if (level != ItemUtils.maxPetLevel(itemName)) {
                        return "$level"
                    }
                }
            }
        }

        if (itemNumberAsStackSize.contains(5) && itemName.contains(" Minion ") &&
            !itemName.contains("Recipe") && item.getLore().any { it.contains("Place this minion") }
        ) {
            val array = itemName.split(" ")
            val last = array[array.size - 1]
            return last.romanToDecimal().toString()
        }

        if (config.displaySackName && ItemUtils.isSack(item)) {
            val sackName = grabSackName(itemName)
            return (if (itemName.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
        }

        if (itemNumberAsStackSize.contains(8) && itemName.contains("Kuudra Key")) {
            return when (itemName) {
                "Kuudra Key" -> "§a1"
                "Hot Kuudra Key" -> "§22"
                "Burning Kuudra Key" -> "§e3"
                "Fiery Kuudra Key" -> "§64"
                "Infernal Kuudra Key" -> "§c5"
                else -> "§4?"
            }
        }

        if (itemNumberAsStackSize.contains(9) &&
            InventoryUtils.openInventoryName() == "Your Skills" &&
            item.getLore().any { it.contains("Click to view!") }
        ) {
            if (CollectionAPI.isCollectionTier0(item.getLore())) return "0"
            val split = itemName.split(" ")
            if (!itemName.contains("Dungeon")) {
                val text = split.last()
                if (split.size < 2) return "0"
                return "" + text.romanToDecimalIfNeeded()
            }
        }

        if (itemNumberAsStackSize.contains(10) && InventoryUtils.openInventoryName()
                .endsWith(" Collections")
        ) {
            val lore = item.getLore()
            if (lore.any { it.contains("Click to view!") }) {
                if (CollectionAPI.isCollectionTier0(lore)) return "0"
                item.name?.let {
                    if (it.startsWith("§e")) {
                        val text = it.split(" ").last()
                        return "" + text.romanToDecimalIfNeeded()
                    }
                }
            }
        }

        if (itemNumberAsStackSize.contains(11) && itemName.contains("Rancher's Boots")) {
            for (line in item.getLore()) {
                rancherBootsSpeedCapPattern.matchMatcher(line) {
                    return group("cap")
                }
            }
        }

        if (itemNumberAsStackSize.contains(12) && itemName.contains("Larva Hook")) {
            for (line in item.getLore()) {
                "§7§7You may harvest §6(?<amount>.).*".toPattern().matchMatcher(line) {
                    val amount = group("amount").toInt()
                    return when {
                        amount > 4 -> "§a$amount"
                        amount > 2 -> "§e$amount"
                        else -> "§c$amount"
                    }
                }
            }
        }

        if (itemNumberAsStackSize.contains(13) && itemName.startsWith("Dungeon ") && itemName.contains(
                " Potion"
            )
        ) {
            item.name?.let {
                "Dungeon (?<level>.*) Potion".toPattern().matchMatcher(it.removeColor()) {
                    return when (val level = group("level").romanToDecimal()) {
                        in 1..2 -> "§f$level"
                        in 3..4 -> "§a$level"
                        in 5..6 -> "§9$level"
                        else -> "§5$level"
                    }
                }
            }
        }

        if (itemNumberAsStackSize.contains(14)) {
            if (item.getInternalNameOrNull() in PestAPI.vacuumVariants) {
                for (line in item.getLore()) {
                    gardenVacuumPatterm.matchMatcher(line) {
                        val pests = group("amount").formatNumber()
                        return if (config.vacuumBagCap) {
                            if (pests > 39) "§640" else "$pests"
                        } else {
                            if (pests < 40) {
                                "$pests"
                            } else if (pests < 1_000) {
                                "§6$pests"
                            } else if (pests < 100_000) {
                                "§c${pests / 1000}k"
                            } else {
                                "§c${pests / 100_000 / 10.0}m"
                            }
                        }
                    }
                }
            }

        }

        return ""
    }

    var done = false

    private fun grabSackName(name: String): String {
        val split = name.split(" ")
        val text = split[0]
        for (line in arrayOf("Large", "Medium", "Small", "Enchanted")) {
            if (text == line) return grabSackName(name.substring(text.length + 1))
        }
        return text
    }
}
