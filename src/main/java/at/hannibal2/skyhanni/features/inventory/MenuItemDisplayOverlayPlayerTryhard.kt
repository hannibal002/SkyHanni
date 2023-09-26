package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerTryhard {
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\.[0-9]*)?(§.)?%".toPattern()
    private val auctionHousePagePattern = "§7\\((?<pagenumber>[0-9]+).*".toPattern()
    private val otherMenusPagePattern = "§.Page (?<pagenumber>[0-9]+)".toPattern()
    private val rngMeterPattern = ".* (?<odds>§.[A-z ]+).*".toPattern()
    private val boosterCookieLoreLinePattern = "(§.)?Duration: (§.)?((?<years>[0-9]+)?y)? ((?<days>[0-9]{0,2})d)? ((?<hours>[0-9]{0,2})h)? ((?<minutes>[0-9]{0,2})m)? ((?<seconds>[0-9]{0,2})s)?".toPattern()
    private val totalFamePattern = "(§.)?Your total: (§.)?(?<total>(?<useful>[0-9]+)((,[0-9]+))+) Fame".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun lazilyGetPercent(original: String, thingToExtract: String = ""): String {
        return original.removeColor().replace(thingToExtract, "").replace("100%", "a✔").take(2).replace(".", "").replace("a✔", "§a✔").replace("%", "")
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.menuItemNumberPlayerTryhardAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.menuItemNumberPlayerTryhardAsStackSize
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
            if (itemName == "Previous Page" || itemName == "Next Page") {
                val line = item.getLore().first().replace(",", "")
                if (chestName.contains("Auction")) { return auctionHousePagePattern.matchMatcher(line) { group("pagenumber") } ?: "" }
                return otherMenusPagePattern.matchMatcher(line) { group("pagenumber") } ?: ""
            }
        }

        if (stackSizeConfig.contains(1)) {
            if ((chestName.contains(" RNG Meter"))) {
                val lore = item.getLore()
                for (line in lore) {
                    if (line.contains("Odds: ")) {
                        return rngMeterPattern.matchMatcher(line) { group("odds").take(3) } ?: ""
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(2)) {
            if ((chestName.contains("Community Shop"))) {
                val lore = item.getLore()
                if (!(lore.isEmpty())) {
                    if ((lore.first().contains(" Upgrade")) ||
                    (lore.last().contains(" to start!")) ||
                    (lore.last().contains("Maxed out")) ||
                    (lore.last().contains("upgrad"))) {
                        return itemName.split(" ").last().romanToDecimalIfNeeded().toString()
                    }
                }
            }  
        }
        
        if (stackSizeConfig.contains(3)) {
            if (chestName.contains("Auction") || chestName.contains("Bazaar") || chestName.contains("Community Shop")) {
                val lore = item.getLore()
                if (!(itemName.isEmpty()) && !(lore.isEmpty())) {
                    if (chestName.contains("Community Shop")) {
                        if (lore.last().contains("§aCurrently selected!")) return "§a⬇"
                    } else if ((chestName.contains("Auction") || (chestName.contains("Bazaar"))) && (lore.first().contains("Category") && lore.last().contains("§aCurrently "))) {
                        return "§a➡"
                    }
                }
            }
        }

        
        if (stackSizeConfig.contains(4)) {
            if ((chestName == "Booster Cookie" && itemName == "Fame Rank")) {
                for (line in item.getLore()) {
                    totalFamePattern.matchMatcher(line) {
                        val totalAsString = group("total").replace(",", "")
                        val usefulPartAsString = group("useful")
                        var suffix = when (totalAsString.length) {
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
                            "Ambassador" -> "Absd"
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

        if (stackSizeConfig.contains(5)) {
            if (!chestName.isEmpty() && !item.getLore().isEmpty() && !itemName.isEmpty() && ((itemName.contains("Booster Cookie")) && ((chestName.lowercase() == "skyblock menu") || (chestName == "Booster Cookie")))) {
                for (line in item.getLore()) {
                    if (line.contains("Duration:")) {
                        boosterCookieLoreLinePattern.matchMatcher(line) {
                            val yearsString = group("years")?.toInt()
                            val daysString = group("days")?.toInt()
                            val hoursString = group("hours")?.toInt()
                            val minutesString = group("minutes")?.toInt()
                            val secondsString = group("seconds")?.toInt()
                            if (yearsString != null && yearsString > 0) return "${yearsString}y"
                            if (daysString != null && daysString > 0) return "${daysString}d"
                            if (hoursString != null && hoursString > 0) return "${hoursString}h"
                            if (minutesString != null && minutesString > 0) return "${minutesString}m"
                            if (secondsString != null && secondsString > 0) return "${secondsString}s"
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(6)) {
            if (chestName.contains("Equipment and Stats") && itemName.contains("Active Effects")) {
                for (line in item.getLore()) {
                    if (line.contains("Currently Active: ")) {
                        return line.split(" ").last()
                    }
                }
            }
        }
        
        return ""
    }
}