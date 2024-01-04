package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.getDuration
import io.github.moulberry.notenoughupdates.miscgui.CalendarOverlay
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemEnderEye
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerTryhard : AbstractMenuStackSize() {
    // private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val auctionHousePageLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.auctionhousepage.loreline"), ("§7\\((?<pagenumber>[0-9]+).*"))
    private val otherMenusPageLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.othermenuspage.loreline"), ("§.Page (?<pagenumber>[0-9]+)"))
    private val rngMeterLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.rngmeter.loreline"), ("(§.)*Odds: (?<odds>(§.[\\w]){1}).*"))
    private val generalPurposeNotBoosterCookieDurationLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.generalpurposenotboostercookieduration.loreline"), ("(§.)?(([A-z ])+): (§.)?(?<fullDuration>(?<years>[0-9]+y)?[ ]?(?<days>[0-9]+d)?[ ]?(?<hours>[0-9]+h)?[ ]?(?<minutes>[0-9]+m)?[ ]?(?<seconds>[0-9]+s)?)"))
    private val totalFameLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.totalfame.loreline"), ("(§.)?Your total: (§.)?(?<total>(?<useful>[0-9]+)((,[0-9]+))+) Fame"))
    private val bitsAvailableLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.bitsavailable.loreline"), ("(§.)?Bits Available: (§.)?(?<total>(?<useful>[0-9]+)(?<useless>(,[0-9]+))*)(§.)?.*"))
    private val magicalPowerLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.magicalpower.loreline"), ("(§.)?Magical Power: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)"))
    private val otherMagicalPowerLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.othermagicalpower.loreline"), (".*(§.)?Total: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*"))
    private val tuningPointsLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.tuningpoints.loreline"), ("(§.)?Tuning Points: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)"))
    private val slotSourceLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.slotsource.loreline"), ("(§.)(?<category>(?!Buying).*)?: (§.)?(\\+?)(?<slots>[0-9]+) (s|S)lots"))
    private val auctionChestNamePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.auction.chestname"), ("Auction.*"))
    private val isNotAuctionAbiphoneContactsDirectoryChestNamePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.isnotauctionabiphonecontactsdirectory.chestname"), ("^(?:(?!Auction|A.iphone|Contacts Directory).)*\$"))
    private val generalPurposeSelectedFilterSortLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.generalpurposeselectedfiltersort.loreline"), ("((?<colorCode>§.)*▶ (?<threeChars>[\\w ]{3}))([\\w ])+"))
    private val rngMeterOddsChestNamePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.rngmeterodds.chestname"), (".* RNG Meter"))
    private val communityShopEssenceShopChestNamePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.communityshopessenceshop.chestname"), ("(Community Shop|.* Essence Shop)"))
    private val communityShopIsUpgradeLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.communityshopisupgrade.loreline"), ("(.* to start!|.*Maxed out!|.*upgrad.*)"))
    private val communityShopIsAlsoUpgradeLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.communityshopisalsoupgrade.loreline"), (".* Upgrade"))
    private val essenceShopIsPurchasableUpgradeLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.essenceshopispurchasableupgrade.loreline"), ("(§.)*(.* unlock|UNLOCK).*"))
    private val isNotRomanNumeralNotForMinecraftOrSkyblockPattern by RepoPattern.pattern(("itemstacksize.player.tryhard.isnotromannumeral.notforminecraftorskyblock"), ("^(?:(?!I|V|X|L|C|D|M).)*\$"))
    private val auctionBazaarCommunityShopIsValidForSelectedTabStackSizeChestNamePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.auctionbazaarcommunityshopisvalidforselectedtabstacksize.chestname"), ("(Auction.*|Bazaar.*|Community Shop)"))
    private val currentlySelectedBrowsingViewingTabLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.currentlyselectedbrowsingviewingtab.loreline"), ("§aCurrently .*"))
    private val isAuctionOrBazaarChestNamePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.isauctionorbazaar.chestname"), ("(Auction.*|Bazaar.*)"))
    private val fameRankLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.famerank.loreline"), ("(§.)*Fame Rank: (§.)*(?<fameRank>[\\w ]+)"))
    private val boosterCookieDurationLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.boostercookieduration.loreline"), ("(§.)*Duration: (§.)*(?<fullDuration>(?<years>[0-9]+y)?[ ]?(?<days>[0-9]+d)?[ ]?(?<hours>[0-9]+h)?[ ]?(?<minutes>[0-9]+m)?[ ]?(?<seconds>[0-9]+s)?)"))
    private val currentlyActiveEffectsLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.currentlyactiveeffects.loreline"), ("(§.)*Currently Active: (§.)*(?<effects>[\\w]+)"))
    private val accessoryBagUpgradesStatsTuningChestNamePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.accessorybagupgradesstatstuning.chestname"), ("(Accessory Bag Upgrades|Stats Tuning)"))
    private val powerStoneLearnedStatusLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.powerstonelearnedstatus.loreline"), ("(§.)*Learned: (?<colorCode>§.)*(?<status>[\\w ]+) (?<icon>.)"))
    private val startingInLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.startingin.loreline"), ("§7Starting in: .*"))
    private val startsInLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.startsin.loreline"), ("§7Starts in: .*"))
    private val achievementPointsLoreLinePattern by RepoPattern.pattern(("itemstacksize.player.tryhard.achievementpoints.loreline"), ("(§.)*Points: (§.)*([\\w,]+)(§.)*\\/(§.)*([\\w,]+) (§.)*\\((?<percent>[\\w]+)%(§.)*\\)"))

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.playerTryhard.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = configMenuStackSize.playerTryhard
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.MENU_NAVIGATION)) {
            val lore = item.getLore()
            if ((itemName == "Previous Page" || itemName == "Next Page")) {
                val line = lore.first()
                auctionChestNamePattern.matchMatcher(chestName) {
                    auctionHousePageLoreLinePattern.matchMatcher(line) {
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
                return otherMenusPageLoreLinePattern.matchMatcher(line) { group("pagenumber") } ?: ""
            }
            isNotAuctionAbiphoneContactsDirectoryChestNamePattern.matchMatcher(chestName) {
                if (((itemName == ("Sort") && (item.getItem() == Item.getItemFromBlock(Blocks.hopper)))) || ((itemName == ("Filter") && (item.getItem() is ItemEnderEye)))) {
                    for (line in lore) {
                        generalPurposeSelectedFilterSortLoreLinePattern.matchMatcher(line) {
                            return group("threeChars").trim() // trim() to remove spaces. removing the space from the regex causes some filter options to get skipped
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.RNG_METER_ODDS)) {
            rngMeterOddsChestNamePattern.matchMatcher(chestName) {
                for (line in item.getLore()) {
                    rngMeterLoreLinePattern.matchMatcher(line) { return group("odds") }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.COMMUNITY_ESSENCE_UPGRADES)) {
            also {
                val lore = item.getLore()
                var canDisplayTier = false
                // (("Community Shop")) || ((" Essence Shop"))
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
                                // the .lowercase() here is to match both "click to unlock" and "unlocked" in one fell swoop
                                val lastWord = itemName.split(" ").last()
                                for (char in lastWord) {
                                // if (!(("IVXLCDM").contains(char))) {
                                    isNotRomanNumeralNotForMinecraftOrSkyblockPattern.matchMatcher("$char") {
                                        return@also
                                    }
                                }
                                return lastWord.romanToDecimalIfNecessary().toString()
                            }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.SELECTED_TAB)) {
            // ("(Auction.*|Bazaar.*)")
            // (("(Auction.*|Bazaar.*)")).toPattern()).matchMatcher(chestName) {
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
                    totalFameLoreLinePattern.matchMatcher(line) {
                        return NumberUtil.format(group("total").formatNumber())
                    }
                }
            }
            if ((chestName == "Booster Cookie" && itemName == "Bits")) {
                for (line in item.getLore()) {
                    bitsAvailableLoreLinePattern.matchMatcher(line) {
                        return NumberUtil.format(group("total").formatNumber())
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
                    return getDuration(group("fullDuration")).format(maxUnits = 1)
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
                    magicalPowerLoreLinePattern.matchMatcher(line) {
                        return NumberUtil.format(group("total").formatNumber())
                    }
                }
            }
            if (chestName == ("Accessory Bag Upgrades") && itemName == ("Accessory Bag Upgrades")) {
                var totalSlotsResult = 0
                for (line in lore) {
                    slotSourceLoreLinePattern.matchMatcher(line){
                        totalSlotsResult += group("slots").toInt()
                    }
                }
                return "$totalSlotsResult"
            }
            accessoryBagUpgradesStatsTuningChestNamePattern.matchMatcher(chestName) {
                if (itemName == ("Stats Tuning")) {
                    for (line in lore) {
                        tuningPointsLoreLinePattern.matchMatcher(line) {
                            return NumberUtil.format(group("total").formatNumber())
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
                    otherMagicalPowerLoreLinePattern.matchMatcher(line) {
                        return NumberUtil.format(group("total").formatNumber())
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
                return getDuration(group("fullDuration")).format(maxUnits = 1)
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.SKYBLOCK_ACHIEVEMENT_POINTS) && (chestName == ("Your Equipment and Stats") && itemName.lowercase() == ("skyblock achievements"))) {
            // §7Points: §e1,995§7/§e2,835 §8(70%§8)
            for (line in item.getLore()) {
                achievementPointsLoreLinePattern.matchMatcher(line) {
                    return group("percent")
                }
            }
        }
        
        return ""
    }
}
