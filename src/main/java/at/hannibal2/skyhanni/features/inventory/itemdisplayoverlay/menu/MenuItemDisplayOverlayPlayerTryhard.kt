package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
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
    private val rngMeterPattern = "(§.)*Odds: (?<odds>(§.[\\w]){1}).*".toPattern()
    private val generalPurposeNotBoosterCookieDurationLoreLinePattern = "(§.)?(([A-z ])+): (§.)?(?<years>[0-9]+y)?[ ]?(?<days>[0-9]+d)?[ ]?(?<hours>[0-9]+h)?[ ]?(?<minutes>[0-9]+m)?[ ]?(?<seconds>[0-9]+s)?".toPattern()
    private val totalFamePattern = "(§.)?Your total: (§.)?(?<total>(?<useful>[0-9]+)((,[0-9]+))+) Fame".toPattern()
    private val bitsAvailablePattern = "(§.)?Bits Available: (§.)?(?<total>(?<useful>[0-9]+)(?<useless>(,[0-9]+))*)(§.)?.*".toPattern()
    private val magicalPowerPattern = "(§.)?Magical Power: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)".toPattern()
    private val magicalPowerSecondPattern = ".*(§.)?Total: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*".toPattern()
    private val tuningPointsPattern = "(§.)?Tuning Points: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)".toPattern()
    private val slotSourcePattern = "(§.)(?<category>(?!Buying).*)?: (§.)?(\\+?)(?<slots>[0-9]+) (s|S)lots".toPattern()
    private val auctionChestNamePattern = (("Auction.*").toPattern())
    private val isNotAuctionAbiphoneContactsDirectoryChestNamePattern = (("^(?:(?!Auction|A.iphone|Contacts Directory).)*\$").toPattern())
    private val generalPurposeSelectedFilterSortLoreLinePattern = (("((?<colorCode>§.)*▶ (?<threeChars>[\\w ]{3}))([\\w ])+").toPattern())
    private val rngMeterOddsChestNamePattern = ((".* RNG Meter").toPattern())
    private val communityShopEssenceShopChestNamePattern = (("(Community Shop|.* Essence Shop)").toPattern())
    private val communityShopIsUpgradeLoreLinePattern = (("(.* to start!|.*Maxed out!|.*upgrad.*)").toPattern())
    private val communityShopIsAlsoUpgradeLoreLinePattern = ((".* Upgrade").toPattern())
    private val essenceShopIsPurchasableUpgradeLoreLinePattern = (("(§.)*(.* unlock|UNLOCK).*").toPattern())
    private val isNotRomanNumeralGeneralPattern = (("^(?:(?!I|V|X|L|C|D|M).)*\$").toPattern())
    private val auctionBazaarCommunityShopIsValidForSelectedTabStackSizeChestNamePattern = (("(Auction.*|Bazaar.*|Community Shop)").toPattern())
    private val currentlySelectedBrowsingViewingTabLoreLinePattern = (("§aCurrently .*").toPattern())
    private val isAuctionOrBazaarChestNamePattern = (("(Auction.*|Bazaar.*)").toPattern())
    private val fameRankLoreLinePattern = (("(§.)*Fame Rank: (§.)*(?<fameRank>[\\w ]+)").toPattern())
    private val boosterCookieDurationLoreLinePattern = (("(§.)*Duration: (§.)*(?<years>[0-9]+y)?[ ]?(?<days>[0-9]+d)?[ ]?(?<hours>[0-9]+h)?[ ]?(?<minutes>[0-9]+m)?[ ]?(?<seconds>[0-9]+s)?").toPattern())
    private val currentlyActiveEffectsLoreLinePattern = (("(§.)*Currently Active: (§.)*(?<effects>[\\w]+)").toPattern())
    private val accessoryBagUpgradesStatsTuningChestNamePattern = (("(Accessory Bag Upgrades|Stats Tuning)").toPattern())
    private val powerStoneLearnedStatusLoreLinePattern = (("(§.)*Learned: (?<colorCode>§.)*(?<status>[\\w ]+) (?<icon>.)").toPattern())
    private val startingInLoreLinePattern = (("§7Starting in: .*").toPattern())
    private val startsInLoreLinePattern = (("§7Starts in: .*").toPattern())
    private val achievementPointsLoreLinePattern = (("(§.)*Points: (§.)*([\\w,]+)(§.)*\\/(§.)*([\\w,]+) (§.)*\\((?<percent>[\\w]+)%(§.)*\\)").toPattern())

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.playerTryhard.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.playerTryhard
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.MENU_NAVIGATION)) {
            val lore = item.getLore()
            if ((itemName == "Previous Page" || itemName == "Next Page")) {
                val line = lore.first()
                auctionChestNamePattern.matchMatcher(chestName) {
                    auctionHousePagePattern.matchMatcher(line) {
                        var pageNum = group("pagenumber").formatNumber()
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
            isNotAuctionAbiphoneContactsDirectoryChestNamePattern.matchMatcher(chestName) {
                if (((itemName == ("Sort") && (item.getItem() == Item.getItemFromBlock(Blocks.hopper)))) || ((itemName == ("Filter") && (item.getItem() is ItemEnderEye)))) {
                    for (line in lore) {
                        generalPurposeSelectedFilterSortLoreLinePattern.matchMatcher(line) {
                            return group("threeChars").trim() //trim() to remove spaces. removing the space from the regex causes some filter options to get skipped
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.RNG_METER_ODDS)) {
            rngMeterOddsChestNamePattern.matchMatcher(chestName) {
                for (line in item.getLore()) {
                    rngMeterPattern.matchMatcher(line) { return group("odds") }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.COMMUNITY_ESSENCE_UPGRADES)) {
            also {
                val lore = item.getLore()
                var canDisplayTier = false
                //(("Community Shop")) || ((" Essence Shop"))
                communityShopEssenceShopChestNamePattern.matchMatcher(chestName) {
                    if (lore.isNotEmpty()) {
                        communityShopIsUpgradeLoreLinePattern.matchMatcher(lore.last()) { canDisplayTier = true }
                        communityShopIsAlsoUpgradeLoreLinePattern.matchMatcher(lore.first()) { canDisplayTier = true }
                        essenceShopIsPurchasableUpgradeLoreLinePattern.matchMatcher(lore.last()) { canDisplayTier = true }
                        /* ((lore.first().contains(" Upgrade")) ||
                                (lore.last().contains(" to start!")) ||
                                (lore.last().contains("Maxed out")) ||
                                (lore.last().contains("upgrad")))) ||
                        ((chestName.contains(" Essence Shop")) &&
                                (lore.last().lowercase().contains("unlock")))) */
                            if (canDisplayTier) {
                                //the .lowercase() here is to match both "click to unlock" and "unlocked" in one fell swoop
                                val lastWord = itemName.split(" ").last()
                                for (char in lastWord) {
                                //if (!(("IVXLCDM").contains(char))) {
                                    isNotRomanNumeralGeneralPattern.matchMatcher("$char") {
                                        return@also
                                    }
                                }
                                return lastWord.romanToDecimalIfNeeded().toString()
                            }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.SELECTED_TAB)) {
            //("(Auction.*|Bazaar.*)")
            //(("(Auction.*|Bazaar.*)")).toPattern()).matchMatcher(chestName) {
            val lore = item.getLore()
            auctionBazaarCommunityShopIsValidForSelectedTabStackSizeChestNamePattern.matchMatcher(chestName) {
                if (itemName.isNotEmpty() && lore.isNotEmpty()) {
                    if (chestName == "Community Shop") {
                        currentlySelectedBrowsingViewingTabLoreLinePattern.matchMatcher(lore.last()) {
                            return "§a⬇"
                        }
                    }
                    isAuctionOrBazaarChestNamePattern.matchMatcher(chestName) {
                        if (lore.first() == ("§8Category")) {
                            currentlySelectedBrowsingViewingTabLoreLinePattern.matchMatcher(lore.last()) {
                                return "§a➡"
                            }
                        }
                    }
                }
            }
        }

        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.FAME_RANK_BITS)) {
            if ((chestName == "Booster Cookie" && itemName == "Fame Rank")) {
                for (line in item.getLore()) {
                    totalFamePattern.matchMatcher(line) {
                        val totalAsString = "${group("total").formatNumber()}"
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
                        val totalAsString = "${group("total").formatNumber()}"
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
                    fameRankLoreLinePattern.matchMatcher(line) {
                        return when (group("fameRank")) {
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

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.BOOSTER_COOKIE_DURATION) && (item.getLore().isNotEmpty() && ((itemName == ("Booster Cookie")) && ((chestName.lowercase() == "skyblock menu") || (chestName == "Booster Cookie"))))) {
            for (line in item.getLore()) {
                boosterCookieDurationLoreLinePattern.matchMatcher(line) {
                    val yString = group("years") ?: ""
                    val dString = group("days") ?: ""
                    val hString = group("hours") ?: ""
                    val mString = group("minutes") ?: ""
                    val sString = group("seconds") ?: ""
                    if (yString.length > 3 || dString.length > 3 || hString.length > 3 || mString.length > 3 || sString.length > 3) return "§b§z:)"
                    if (yString.isNotEmpty() && !(yString.startsWith("0"))) return yString
                    if (dString.isNotEmpty() && !(dString.startsWith("0"))) return dString
                    if (hString.isNotEmpty() && !(hString.startsWith("0"))) return hString
                    if (mString.isNotEmpty() && !(mString.startsWith("0"))) return mString
                    if (sString.isNotEmpty() && !(sString.startsWith("0"))) return sString
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.ACTIVE_POTION_COUNT) && (chestName == ("Your Equipment and Stats") && itemName == ("Active Effects"))) {
            for (line in item.getLore()) {
                currentlyActiveEffectsLoreLinePattern.matchMatcher(line) {
                    return group("effects")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.ACCESSORY_BAG_UTILS)) {
            val lore = item.getLore()
            if (chestName == ("Your Bags") && itemName == ("Accessory Bag")) {
                for (line in lore) {
                    magicalPowerPattern.matchMatcher(line) {
                        val usefulAsString = group("useful")
                        val totalAsString = "${group("total").formatNumber()}"
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
            if (chestName == ("Accessory Bag Upgrades") && itemName == ("Accessory Bag Upgrades")) {
                var totalSlotsResult = 0
                for (line in lore) {
                    slotSourcePattern.matchMatcher(line){
                        totalSlotsResult += group("slots").toInt()
                    }
                }
                return "$totalSlotsResult"
            }
            accessoryBagUpgradesStatsTuningChestNamePattern.matchMatcher(chestName) {
                if (itemName == ("Stats Tuning")) {
                    for (line in lore) {
                        tuningPointsPattern.matchMatcher(line) {
                            val usefulAsString = group("useful")
                            val totalAsString = "${group("total").formatNumber()}"
                            val suffix = when (totalAsString.length) {
                                in 1..3 -> ""
                                in 4..6 -> "k"
                                else -> "§b§z:)"
                            }
                            if (suffix == "§b§z:)") return suffix
                            else return "$usefulAsString$suffix"
                        }
                    }
                }
            }
            if (chestName == ("Power Stones Guide")) {
                if (lore.isNotEmpty()) {
                    powerStoneLearnedStatusLoreLinePattern.matchMatcher(lore.last()) {
                        return "${group("colorCode")}${group("icon")}"
                    }
                }
            }
            if (chestName == ("Accessory Bag Thaumaturgy") && itemName == ("Accessories Breakdown")) {
                for (line in lore) {
                    magicalPowerSecondPattern.matchMatcher(line) {
                        val usefulString = group("useful")
                        val totalString = "${group("total").formatNumber()}"
                        val suffix = when (totalString.length) {
                            in 1..3 -> ""
                            in 4..6 -> "k"
                            in 7..9 -> "M"
                            else -> "§b§z:)"
                        }
                        if (suffix == "§b§z:)") return suffix
                        else return "$usefulString$suffix"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.EVENT_COUNTDOWN_ABBV)) {
            val lore = item.getLore()
            var theStringToUse = ""
            if (lore.isNotEmpty() && (chestName.lowercase() == ("skyblock menu") && itemName == ("Calendar and Events"))) {
                for (line in lore) {
                    startingInLoreLinePattern.matchMatcher(line) {
                        theStringToUse = line
                    }
                }
            }
            if (lore.isNotEmpty() && chestName == ("Calendar and Events") && !CalendarOverlay.isEnabled()) {
                startsInLoreLinePattern.matchMatcher(lore.first()) {
                    theStringToUse = lore.first()
                }
            }
            generalPurposeNotBoosterCookieDurationLoreLinePattern.matchMatcher(theStringToUse) {
                val yString = group("years") ?: ""
                val dString = group("days") ?: ""
                val hString = group("hours") ?: ""
                val mString = group("minutes") ?: ""
                val sString = group("seconds") ?: ""
                if (yString.length > 3 || dString.length > 3 || hString.length > 3 || mString.length > 3 || sString.length > 3) return "§b§z:)"
                if (yString.isNotEmpty() && !(yString.startsWith("0"))) return yString
                if (dString.isNotEmpty() && !(dString.startsWith("0"))) return dString
                if (hString.isNotEmpty() && !(hString.startsWith("0"))) return hString
                if (mString.isNotEmpty() && !(mString.startsWith("0"))) return mString
                if (sString.isNotEmpty() && !(sString.startsWith("0"))) return sString
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.SKYBLOCK_ACHIEVEMENT_POINTS) && (chestName == ("Your Equipment and Stats") && itemName.lowercase() == ("skyblock achievements"))) {
            //§7Points: §e1,995§7/§e2,835 §8(70%§8)
            for (line in item.getLore()) {
                achievementPointsLoreLinePattern.matchMatcher(line) {
                    return group("percent")
                }
            }
        }
        
        return ""
    }
}
