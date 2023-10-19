package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.anyContains
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAuctionNumber
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBloodGodKills
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEdition
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getNecronHandlesFound
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPrehistoricEggBlocksWalked
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getYetiRodFishesCaught
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemDisplayOverlayFeatures {
    private val rancherBootsSpeedCapPattern = "§7Current Speed Cap: §a(?<cap>.*)".toPattern()
    private val petLevelPattern = "\\[Lvl (?<level>.*)] .*".toPattern()
    private val whyHaventTheAdminsAddedShredderBonusDamageInfoToItemNBTDataYetPattern = "(§.)?Bonus Damage \\([0-9]+ cap\\): (§.)?(?<dmgbonus>[0-9]+)".toPattern()
    private val iReallyHateTheBottleOfJerryPattern = "(§.)?Intelligence Bonus: (§.)?(?<intelbonus>[0-9]+)".toPattern()
    private val xOutOfYNoColorRequiredPattern = ".*: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?(?<total>[0-9]+).*".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.itemNumberAsStackSize
        val chestName = InventoryUtils.openInventoryName()
        val internalName = item.getInternalName().asString()

        if (stackSizeConfig.contains(0)) {
            when (itemName) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (stackSizeConfig.contains(1) && itemName.matchRegex("(.*)Master Skull - Tier .")) {
            return itemName.substring(itemName.length - 1)
        }

        if (stackSizeConfig.contains(2) && (itemName.contains("Golden ") || itemName.contains("Diamond "))) {
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

        if (stackSizeConfig.contains(3)) {
            if (itemName.startsWith("New Year Cake (")) return "§b" + itemName.between("(Year ", ")")
            if (itemName.startsWith("Spooky Pie")) {
                for (line in item.getLore()) {
                    if (line.lowercase().contains("skyblock year ")) return "§b" + line.lowercase().removeColor().between("skyblock year ", ".")
                }
            }
        }

        if (stackSizeConfig.contains(4) && (!chestName.endsWith("Sea Creature Guide")) && (ItemUtils.isPet(itemName))) {
            petLevelPattern.matchMatcher(itemName) {
                val level = group("level").toInt()
                if (level != ItemUtils.maxPetLevel(itemName)) {
                    return "$level"
                }
            }
        }

        if (stackSizeConfig.contains(5) && (itemName.contains(" Minion ")) && !(itemName.endsWith(" Recipes")) && (item.getLore().anyContains("Place this minion"))) {
            val array = itemName.split(" ")
            val last = array[array.size - 1]
            return last.romanToDecimal().toString()
        }

        if (SkyHanniMod.feature.inventory.displaySackName && ItemUtils.isSack(item)) {
            val sackName = grabSackName(itemName)
            return (if (itemName.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
        }

        if (stackSizeConfig.contains(7) && itemName.contains("Kuudra Key")) {
            return when (itemName) {
                "Kuudra Key" -> "§a1"
                "Hot Kuudra Key" -> "§22"
                "Burning Kuudra Key" -> "§e3"
                "Fiery Kuudra Key" -> "§64"
                "Infernal Kuudra Key" -> "§c5"
                else -> "§4?"
            }
        }

        if (stackSizeConfig.contains(8) && itemName.contains("Rancher's Boots")) {
            for (line in item.getLore()) {
                rancherBootsSpeedCapPattern.matchMatcher(line) {
                    return group("cap")
                }
            }
        }

        if (stackSizeConfig.contains(9) && itemName.contains("Larva Hook")) {
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

        if (stackSizeConfig.contains(10) && (itemName.startsWith("Dungeon ") && itemName.contains(" Potion"))) {
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

        if (stackSizeConfig.contains(11) && (itemName == "Prehistoric Egg")) {
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

        if (stackSizeConfig.contains(12) && itemName.contains("Necron's Ladder")) {
            return item.getNecronHandlesFound().toString().replace("null", "")
        }

        if (stackSizeConfig.contains(13) && itemName.contains("Fruit Bowl")) {
            val lore = item.getLore()
            if (lore.anyContains(" found:")) {
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

        if (stackSizeConfig.contains(14) && itemName.contains("Beastmaster Crest")) {
            for (line in item.getLore()) { //§7Your kills: §21,581§8/2,500
                if (line.contains("Your kills: ")) {
                    val num = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").first()
                    val denom = line.removeColor().replace("Your kills: ", "").replace(",", "").split("/").last()
                    return (((num.toFloat() / denom.toFloat()) * 100).toString().take(2))
                }
            }
        }

        if (stackSizeConfig.contains(15) && internalName.contains("CAMPFIRE_TALISMAN_")) {
            return ((internalName.replace("CAMPFIRE_TALISMAN_", "").toInt()) + 1).toString()

        }

        if (stackSizeConfig.contains(16) && internalName == ("BLOOD_GOD_CREST")) {
            return (item.getBloodGodKills().toString().length.toString())
        }

        if (stackSizeConfig.contains(17) && internalName == ("YETI_ROD")) {
            val kills = item.getYetiRodFishesCaught().toString()
            if (kills == "null") { return "" }
            if (kills.length >= 4){ return "100" }
            else { return (kills.dropLast(1)) }
        }

        if (stackSizeConfig.contains(18) && internalName == ("THE_SHREDDER")) {
            val lore = item.getLore()
                if ((lore.anyContains("cap): ")) && (lore.anyContains("Bonus Damage "))) {
                    for (line in lore) {
                        whyHaventTheAdminsAddedShredderBonusDamageInfoToItemNBTDataYetPattern.matchMatcher(line) {
                            return group("dmgbonus")
                        }
                    }
                }
        }

        if (stackSizeConfig.contains(19) && internalName == ("BOTTLE_OF_JYRRE")) {
            val lore = item.getLore()
            if (lore.anyContains("Intelligence Bonus: ")) {
                for (line in lore) {
                    iReallyHateTheBottleOfJerryPattern.matchMatcher(line) {
                        return group("intelbonus")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(20) && internalName.startsWith("SOULFLOW_") && !(chestName.contains("Auction"))) {
            //§7Internalized: §316,493⸎ Soulflow
            //Internalized: 16,493⸎ Soulflow
            val line = item.getLore().first()
            if (line.contains("Internalized: ") && line.contains(" Soulflow")) {
                val soulflowCount = line.removeColor().between("Internalized: ", "⸎ Soulflow")
                val soulflowCountWithoutCommas = soulflowCount.replace(",", "")
                val usefulAsString = "(?<leading>[0-9]+)(?<trailing>,[0-9]{0,3})*".toPattern().matchMatcher(soulflowCount) { group("leading") } ?: ""
                val suffix = when (soulflowCountWithoutCommas.length) {
                    in 1..3 -> ""
                    in 4..6 -> "k"
                    in 7..9 -> "M"
                    in 10..12 -> "B"
                    in 13..15 -> "T"
                    else -> "§b§z:)"
                }
                if (usefulAsString.isEmpty()) return ""
                if (suffix == "§b§z:)") return suffix
                else return "" + usefulAsString + suffix
            }
        }

        if (stackSizeConfig.contains(21) && internalName.startsWith("CRUX_")) {
            val lore = item.getLore()
            var numberOfLines = 0 //"zoMG ERY WHY NOT JUST REPLACE "CRUX_TALISMAN"?!?!?" yeah i considered that too but then realized hypixel might change that one day
            var killCount = 0
            val currentKillThresholdPerMobFamily = 100 //change this in case hypixel increases the kill count to max a crux accessory
            if (lore.any {it.endsWith("kills")}) {
                for (line in lore) {
                    if (line.endsWith("kills")) {
                        numberOfLines++
                        xOutOfYNoColorRequiredPattern.matchMatcher(line) {
                            val mobSpecificKillCount = group("useful").toInt() ?: 0
                            killCount += mobSpecificKillCount
                        }
                    }
                }
                val totalKillsNecessary = currentKillThresholdPerMobFamily * numberOfLines
                val percent = (((killCount.toFloat()) / (totalKillsNecessary.toFloat())) * 100) //keep this line here for easier debugging
                return percent.toInt().toString()
            }
        }

        if (stackSizeConfig.contains(22) && internalName.endsWith("_ENCHANTED_CHEST") && itemName.endsWith(" Storage")) {
            var colorCode = item.name ?: return ""
            colorCode = colorCode.take(2)
            val numSlots = when (itemName) {
                ("Small Storage") -> "3"
                ("Medium Storage") -> "9"
                ("Large Storage") -> "15"
                ("X-Large Storage") -> "21"
                ("XX-Large Storage") -> "27"
                else -> ""
            }
            return "" + colorCode + numSlots
        }

        if (stackSizeConfig.contains(23)) {
            if (chestName.contains("Personal Compactor ") || chestName.contains("Personal Deletor ")) {
                //§aCompactor Currently OFF!
                //§aCompactor Currently ON!
                if (itemName.contains(" Currently ")) {
                    return when (itemName.replace("Compactor ", "").replace("Deletor ", "")) {
                        "Currently OFF!" -> "§c§l✖"
                        "Currently ON!" -> "§a✔"
                        else -> ""
                    }
                }
            }
            if (itemName.startsWith("Personal ") && internalName.contains("PERSONAL_")) {
                return itemName.replace("Personal ", "").replace("Compactor ", "").replace("Deletor ", "").dropLast(3) + "K"
            }
        }

        if (stackSizeConfig.contains(24) && internalName.startsWith("ABIPHONE_")) {
            return when (internalName) {
                "ABIPHONE_X_PLUS" -> "X"
                "ABIPHONE_X_PLUS_SPECIAL_EDITION" -> "X§b§zSE"
                "ABIPHONE_XI_ULTRA" -> "11"
                "ABIPHONE_XI_ULTRA_STYLE" -> "11§b§zS"
                "ABIPHONE_XII_MEGA" -> "12"
                "ABIPHONE_XII_MEGA_COLOR" -> "12§b§zC"
                "ABIPHONE_XIII_PRO" -> "13"
                "ABIPHONE_XIII_PRO_GIGA" -> "13§b§zG"
                "ABIPHONE_XIV_ENORMOUS" -> "14"
                "ABIPHONE_XIV_ENORMOUS_BLACK" -> "§714"
                "ABIPHONE_XIV_ENORMOUS_PURPLE" -> "§714"
                "ABIPHONE_FLIP_DRAGON" -> "Fl§b§zD"
                "ABIPHONE_FLIP_NUCLEUS" -> "Fl§b§zN"
                "ABIPHONE_FLIP_VOLCANO" -> "Fl§b§zV"
                else -> ""
            }
        }
		
		if (stackSizeConfig.contains(25)) {
            var thatNumber = ""
            if (item.getLore().anyContains("Auction ")) {
                thatNumber = item.getAuctionNumber().toString()
            }
            if (item.getLore().anyContains("Edition ")) {
                thatNumber = item.getEdition().toString()
            }
            if (thatNumber == "null") { return "" }
            if (thatNumber.length >= 4){ return "" }
            else { return (thatNumber) }
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