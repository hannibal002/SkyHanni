package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBloodGodKills
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getNecronHandlesFound
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPrehistoricEggBlocksWalked
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getYetiRodFishesCaught
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemDisplayOverlayFeatures {
    private val rancherBootsSpeedCapPattern = "§7Current Speed Cap: §a(?<cap>.*)".toPattern()
    private val petLevelPattern = "\\[Lvl (?<level>.*)] .*".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.itemNumberAsStackSize
        val chestName = InventoryUtils.openInventoryName()
        /*
        -------------------------------IMPORTANT------------------------------------
        
        If at *any* point someone tells you to not nest your code, do the following
        (in any order, but more importantly in the order given below):
        - tell them to "cope and seethe", especially if they cite CodeAesthetic
        - kindly remind them that the last time someone attempted this, friendships
          were almost shattered in the process
        - you will lose your sanity as you try to figure out what the fuck went wrong
          in your forays with string manipulation
        - remind them of the Single-responsibility Principle:
          https://en.wikipedia.org/wiki/Single-responsibility_principle
        - make sure you have an IDE capable of debugging within your reach
        
        This concludes the PSA. Happy writing! -Erymanthus

        PS: T'was all a joke. Just don't do stupid shit like
        ` if (!(chestName == "Visitor's Logbook")) return "" `
        and you *should* be fine for the most part.
        ----------------------------------------------------------------------------
        */

        //NOTE: IT'S String.length, NOT String.length()!

        if (stackSizeConfig.contains(0)) {
            when (itemName) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (stackSizeConfig.contains(1)) {
            if (itemName.matchRegex("(.*)Master Skull - Tier .")) {
                return itemName.substring(itemName.length - 1)
            }
        }

        if (stackSizeConfig.contains(2)) {
            if (itemName.contains("Golden ") || itemName.contains("Diamond ")) {
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
        }

        if (stackSizeConfig.contains(3)) {
            if (itemName.startsWith("New Year Cake (")) {
                return "§b" + itemName.between("(Year ", ")")
            }
        }

        if (stackSizeConfig.contains(4)) {
            if (!chestName.endsWith("Sea Creature Guide")) {
                if (ItemUtils.isPet(itemName)) {
                    petLevelPattern.matchMatcher(itemName) {
                        val level = group("level").toInt()
                        if (level != ItemUtils.maxPetLevel(itemName)) {
                            return "$level"
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(5)) {
            if (itemName.contains(" Minion ")) {
                if (item.getLore().any { it.contains("Place this minion") }) {
                    val array = itemName.split(" ")
                    val last = array[array.size - 1]
                    return last.romanToDecimal().toString()
                }
            }
        }

        if (SkyHanniMod.feature.inventory.displaySackName) {
            if (ItemUtils.isSack(itemName)) {
                val sackName = grabSackName(itemName)
                return (if (itemName.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
            }
        }

        
        if (stackSizeConfig.contains(7)) {
            if (itemName.contains("Kuudra Key")) {
                return when (itemName) {
                    "Kuudra Key" -> "§a1"
                    "Hot Kuudra Key" -> "§22"
                    "Burning Kuudra Key" -> "§e3"
                    "Fiery Kuudra Key" -> "§64"
                    "Infernal Kuudra Key" -> "§c5"
                    else -> "§4?"
                }
            }
        }

        if (stackSizeConfig.contains(8)) {
            if (itemName.contains("Rancher's Boots")) {
                for (line in item.getLore()) {
                    rancherBootsSpeedCapPattern.matchMatcher(line) {
                        return group("cap")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(9)) {
            if (item.getInternalName_old() == "LARVA_HOOK") {
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
        }

        if (stackSizeConfig.contains(10)) {
            if (item.getInternalName_old() == "POTION") {
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
        }

        if (stackSizeConfig.contains(11)) {
            if (itemName == "Prehistoric Egg") {
                val lore = item.getLore()
                if (lore.lastOrNull() == null) return ""
                val rarity = lore.last().removeColor().trim()
                val blocksWalked = item.getPrehistoricEggBlocksWalked() ?: return ""
                val threshold = when (rarity) {
                    "COMMMON" -> 4000
                    "UNCOMMON" -> 10000
                    "RARE" -> 20000
                    "EPIC" -> 40000
                    "LEGENDARY" -> 100000
                    else -> 1
                }
                if (threshold != 1) { return (((blocksWalked.toFloat()) / (threshold.toFloat())) * 100).toInt().toString() }
            }
        }

        if (stackSizeConfig.contains(12)) {
            if (itemName.contains("Necron's Ladder")) {
                return item.getNecronHandlesFound().toString().replace("null", "")
            }
        }

        if (stackSizeConfig.contains(13)) {
            if (itemName.contains("Fruit Bowl")) {
                val lore = item.getLore()
                if (lore.any { it.contains(" found:") }) {
                    var numFound = 0
                    for (line in lore) {
                        if (line.contains("§e")) {
                            numFound += (line.split("§e").size - 1) //shoutout to IR42 for this one-liner: https://stackoverflow.com/a/61752225
                        } else if (line.contains("Names missing:")) {
                            return numFound.toString()
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(14)) {
            if (itemName.contains("Beastmaster Crest")) {
                for (line in item.getLore()) { //§7Your kills: §21,581§8/2,500
                    if (line.contains("Your kills: ")) {
                        val num = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").first()
                        val denom = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").last()
                        return (((num.toFloat() / denom.toFloat()) * 100).toString().take(2))
                    }
                }
            }
        }

        if (stackSizeConfig.contains(15)) {
            if (item.getInternalName_old().contains("CAMPFIRE_TALISMAN_")) {
                return ((item.getInternalName_old().replace("CAMPFIRE_TALISMAN_", "").toInt()) + 1).toString()
            }
        }

        if (stackSizeConfig.contains(16)) {
            if (item.getInternalName_old() == ("BLOOD_GOD_CREST")) {
                return (item.getBloodGodKills().toString().length.toString())
            }
        }

        if (stackSizeConfig.contains(17)) {
            if (item.getInternalName_old() == ("YETI_ROD")) {
                val kills = item.getYetiRodFishesCaught().toString()
                if (kills == "null") { return "" }
                if (kills.length >= 4){ return "100" }
                else { return (kills.dropLast(1)) }
            }
        }

        if (stackSizeConfig.contains(18)) {
            if (item.getLore().any { it.contains("Edition ") }) {
                val edition = item.getEdition().toString()
                if (edition == "null") { return "" }
                if (edition.length >= 4){ return "" }
                else { return (edition) }
            }
        }

        return ""
    }
    
    private fun grabSackName(name: String): String {
        val split = name.split(" ")
        val text = split[0]
        if (text == "Ink") return "§k" //hotfix line because recursive functions are POG and we need to get rid of this false postive
        for (line in arrayOf("Large", "Medium", "Small", "Enchanted")) {
            if (text == line) return grabSackName(name.substring(text.length + 1))
        }
        return text
    }
}