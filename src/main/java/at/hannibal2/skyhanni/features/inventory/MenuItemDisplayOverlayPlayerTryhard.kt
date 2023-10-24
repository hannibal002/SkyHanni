package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils.anyContains
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.miscgui.CalendarOverlay
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemEnderEye
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerTryhard {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val auctionHousePagePattern = "§7\\((?<pagenumber>[0-9]+).*".toPattern()
    private val otherMenusPagePattern = "§.Page (?<pagenumber>[0-9]+)".toPattern()
    private val rngMeterPattern = ".* (?<odds>§.[A-z ]+).*".toPattern()
    private val genericDurationPattern = "(§.)?(([A-z ])+): (§.)?(?<years>[0-9]+y)?[ ]?(?<days>[0-9]+d)?[ ]?(?<hours>[0-9]+h)?[ ]?(?<minutes>[0-9]+m)?[ ]?(?<seconds>[0-9]+s)?".toPattern()
    private val totalFamePattern = "(§.)?Your total: (§.)?(?<total>(?<useful>[0-9]+)((,[0-9]+))+) Fame".toPattern()
    private val bitsAvailablePattern = "(§.)?Bits Available: (§.)?(?<total>(?<useful>[0-9]+)(?<useless>(,[0-9]+))*)(§.)?.*".toPattern()
    private val magicalPowerPattern = "(§.)?Magical Power: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)".toPattern()
    private val magicalPowerSecondPattern = ".*(§.)?Total: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*".toPattern()
    private val tuningPointsPattern = "(§.)?Tuning Points: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)".toPattern()
    private val slotSourcePattern = "(§.)(?<category>.*)?: (§.)?(\\+?)(?<slots>[0-9]+) (s|S)lots".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.menuItemNumberPlayerTryhardAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.menuItemNumberPlayerTryhardAsStackSize
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(0)) {
            val lore = item.getLore()
            if ((itemName == "Previous Page" || itemName == "Next Page")) {
                val line = lore.first().replace(",", "")
                if (chestName.contains("Auction")) {
                    auctionHousePagePattern.matchMatcher(line) {
                        var pageNum = group("pagenumber").toInt()
                        if (itemName == "Previous Page") {
                            pageNum--
                        } else if (itemName == "Next Page") {
                            pageNum++
                        }
                        if (pageNum > 999) return "1k+"
                        else return "$pageNum"
                    }
                }
                return otherMenusPagePattern.matchMatcher(line) { group("pagenumber") } ?: ""
            }
            if (!(chestName.contains("Auction")) && !(chestName.contains("Abiphone") || chestName.contains("AⒷiphone")) && !(chestName.contains("Contacts Directory"))) {
                if (((itemName.contains("Sort") && (item.getItem() == Item.getItemFromBlock(Blocks.hopper)) && (lore.anyContains("▶ ") ))) || ((itemName.contains("Filter") && (item.getItem() is ItemEnderEye)) && (lore.anyContains("▶ ")))) {
                    for (line in lore) {
                        if (line.contains("▶ ")) {
                            return line.removeColor().replace("▶ ","").replace(" ","").take(3)
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(1) && (chestName.contains(" RNG Meter"))) {
            for (line in item.getLore()) {
                if (line.contains("Odds: ")) {
                    return rngMeterPattern.matchMatcher(line) { group("odds").take(3) } ?: ""
                }
            }
        }
        
        if (stackSizeConfig.contains(2) && (chestName.contains("Community Shop")) || (chestName.contains(" Essence Shop"))) {
            also {
                val lore = item.getLore()
                if (lore.isNotEmpty()) {
                    if (((chestName.contains("Community Shop")) &&
                            ((lore.first().contains(" Upgrade")) ||
                                (lore.last().contains(" to start!")) ||
                                (lore.last().contains("Maxed out")) ||
                                (lore.last().contains("upgrad")))) || ((chestName.contains(" Essence Shop")) && (lore.last().lowercase().contains("unlock")))) { //the .lowercase() here is to match both "click to unlock" and "unlocked" in one fell swoop
                        val lastWord = itemName.split(" ").last()
                        for (char in lastWord) {
                            if (!(("IVXLCDM").contains(char))) {
                                return@also
                            }
                        }
                        return lastWord.romanToDecimalIfNeeded().toString()
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(3) && (chestName.contains("Auction") || chestName.contains("Bazaar") || chestName.contains("Community Shop"))) {
            val lore = item.getLore()
            if (itemName.isNotEmpty() && lore.isNotEmpty()) {
                if (chestName.contains("Community Shop")) {
                    if (lore.last().contains("§aCurrently selected!")) return "§a⬇"
                } else if ((chestName.contains("Auction") || (chestName.contains("Bazaar"))) && (lore.first().contains("Category") && lore.last().contains("§aCurrently "))) {
                    return "§a➡"
                }
            }
        }

        
        if (stackSizeConfig.contains(4)) {
            if ((chestName == "Booster Cookie" && itemName == "Fame Rank")) {
                for (line in item.getLore()) {
                    totalFamePattern.matchMatcher(line) {
                        val totalAsString = group("total").replace(",", "")
                        val usefulPartAsString = group("useful")
                        val suffix = when (totalAsString.length) {
                            in 1..3 -> ""
                            in 4..6 -> "k"
                            in 7..9 -> "M"
                            in 10..12 -> "B"
                            in 13..15 -> "T"
                            else -> "§b§z:)"
                        }
                        if (suffix == "§b§z:)") return suffix
                        else return "" + usefulPartAsString + suffix
                    }
                }
            }
            if ((chestName == "Booster Cookie" && itemName == "Bits")) {
                for (line in item.getLore()) {
                    bitsAvailablePattern.matchMatcher(line) {
                        val totalAsString = group("total").replace(",", "")
                        val usefulPartAsString = group("useful")
                        val suffix = when (totalAsString.length) {
                            in 1..3 -> ""
                            in 4..6 -> "k"
                            in 7..9 -> "M"
                            in 10..12 -> "B"
                            in 13..15 -> "T"
                            else -> "§b§z:)"
                        }
                        if (suffix == "§b§z:)") return suffix
                        else return "" + usefulPartAsString + suffix
                    }
                }
            }
            if ((chestName == "Community Shop" && itemName == "Community Shop")) {
                for (line in item.getLore()) {
                    if (line.contains("Fame Rank: ")) {
                        return when (line.removeColor().replace("Fame Rank: ", "")) {
                            "New Player" -> "NP"
                            "Settler" -> "Str"
                            "Citizen" -> "Ctz"
                            "Contributor" -> "Ctb"
                            "Philanthropist" -> "Phl"
                            "Patron" -> "Ptn"
                            "Famous Player" -> "FP"
                            "Attaché" -> "Atc"
                            "Ambassador" -> "Abd"
                            "Statesperson" -> "Stp"
                            "Senator" -> "Snt"
                            "Dignitary" -> "Dgn"
                            "Councilor" -> "Cnl"
                            "Minister" -> "Mst"
                            "Premier" -> "Pmr"
                            "Chancellor" -> "Chr"
                            "Supreme" -> "Sup"
                            else -> "?"
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(5) && (chestName.isNotEmpty() && item.getLore().isNotEmpty() && itemName.isNotEmpty() && ((itemName.contains("Booster Cookie")) && ((chestName.lowercase() == "skyblock menu") || (chestName == "Booster Cookie"))))) {
            for (line in item.getLore()) {
                if (line.contains("Duration:")) {
                    genericDurationPattern.matchMatcher(line) {
                        val yString = group("years") ?: ""
                        val dString = group("days") ?: ""
                        val hString = group("hours") ?: ""
                        val mString = group("minutes") ?: ""
                        val sString = group("seconds") ?: ""
                        if (yString.isNotEmpty() && !(yString.startsWith("0"))) return yString
                        if (dString.isNotEmpty() && !(dString.startsWith("0"))) return dString
                        if (hString.isNotEmpty() && !(hString.startsWith("0"))) return hString
                        if (mString.isNotEmpty() && !(mString.startsWith("0"))) return mString
                        if (sString.isNotEmpty() && !(sString.startsWith("0"))) return sString
                    }
                }
            }
        }

        if (stackSizeConfig.contains(6) && (chestName.contains("Equipment and Stats") && itemName.contains("Active Effects"))) {
            for (line in item.getLore()) {
                if (line.contains("Currently Active: ")) {
                    return line.split(" ").last()
                }
            }
        }

        if (stackSizeConfig.contains(7)) {
            val lore = item.getLore()
            if (chestName.contains("Your Bags") && itemName.contains("Accessory Bag")) {
                for (line in lore) {
                    if (line.contains("Magical Power: ")) {
                        magicalPowerPattern.matchMatcher(line) {
                            val usefulAsString = group("useful")
                            val totalAsString = group("total").replace(",", "")
                            val suffix = when (totalAsString.length) {
                                in 1..3 -> ""
                                in 4..6 -> "k"
                                in 7..9 -> "M"
                                else -> "§b§z:)"
                            }
                            if (suffix == "§b§z:)") return suffix
                            else return "" + usefulAsString + suffix
                        }
                    }
                }
            }
            if (chestName.contains("Accessory Bag Upgrades") && itemName == ("Accessory Bag Upgrades")) {
                var totalSlotsResult = 0
                for (line in lore) {
                    if (line.contains(" Slots") && (line.contains("Elizabeth") || line.contains("Redstone ") || line.contains("Jacobus"))) {
                        slotSourcePattern.matchMatcher(line){
                            totalSlotsResult += group("slots").toInt()
                        }
                    }
                }
                return "$totalSlotsResult"
            }
            if (chestName.contains("Stats Tuning") && itemName == ("Stats Tuning")) {
                if (lore.anyContains("Tuning Points: ")) {
                    for (line in lore) {
                        if (line.contains("Tuning Points: ")) {
                            tuningPointsPattern.matchMatcher(line) {
                                val usefulAsString = group("useful")
                                val totalAsString = group("total").replace(",", "")
                                val suffix = when (totalAsString.length) {
                                    in 1..3 -> ""
                                    in 4..6 -> "k"
                                    else -> "§b§z:)"
                                }
                                if (suffix == "§b§z:)") return suffix
                                else return "" + usefulAsString + suffix
                            }
                        }
                    }
                }
            }
            if (chestName.contains("Power Stones Guide")) {
                if (lore.isNotEmpty() && item.getLore().last().contains("Learned: ")) {
                    val symbol = lore.last().split(" ").last()
                    if (symbol == "✖") return "§c$symbol"
                    else return "§a$symbol"
                }
            }
            if (chestName.contains(" Thaumaturgy") && itemName.contains(" Breakdown")) {
                for (line in lore) {
                    if (line.contains("Total: ")) {
                        magicalPowerSecondPattern.matchMatcher(line) {
                            val usefulString = group("useful")
                            val totalString = group("total").replace(",", "")
                            val suffix = when (totalString.length) {
                                in 1..3 -> ""
                                in 4..6 -> "k"
                                in 7..9 -> "M"
                                else -> "§b§z:)"
                            }
                            if (suffix == "§b§z:)") return suffix
                            else return "" + usefulString + suffix
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(8)) {
            val lore = item.getLore()
            var theStringToUse = ""
            if (lore.isNotEmpty() && (chestName.lowercase() == ("skyblock menu") && itemName == ("Calendar and Events"))) {
                if (lore.anyContains(" in: ")) {
                    for (line in lore) {
                        if (line.contains(" in: ")) {
                            theStringToUse = line
                        }
                    }
                }
            }
            if (lore.isNotEmpty() && lore.first().contains(" in: ") && chestName == ("Calendar and Events") && !CalendarOverlay.isEnabled()) {
                theStringToUse = lore.first()
            }
            genericDurationPattern.matchMatcher(theStringToUse) {
                val yString = group("years") ?: ""
                val dString = group("days") ?: ""
                val hString = group("hours") ?: ""
                val mString = group("minutes") ?: ""
                val sString = group("seconds") ?: ""
                if (yString.isNotEmpty() && !(yString.startsWith("0"))) return yString
                if (dString.isNotEmpty() && !(dString.startsWith("0"))) return dString
                if (hString.isNotEmpty() && !(hString.startsWith("0"))) return hString
                if (mString.isNotEmpty() && !(mString.startsWith("0"))) return mString
                if (sString.isNotEmpty() && !(sString.startsWith("0"))) return sString
            }
        }

        if (stackSizeConfig.contains(9) && (chestName.contains("Equipment and Stats") && itemName.lowercase().contains("skyblock achievements"))) {
            for (line in item.getLore()) {
                if (line.contains("Points: ")) {
                    return line.removeColor().split(" ").last().between("(", "%)")
                }
            }
        }
        
        return ""
    }
}
