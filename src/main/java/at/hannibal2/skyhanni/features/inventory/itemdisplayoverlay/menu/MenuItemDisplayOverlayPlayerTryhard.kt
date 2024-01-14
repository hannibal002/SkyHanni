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
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.TimeUtils.getDuration
import io.github.moulberry.notenoughupdates.miscgui.CalendarOverlay
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemEnderEye
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerTryhard : AbstractMenuStackSize() {
    private val playerTryhardSubgroup = itemStackSizeGroup.group("player.tryhard")
    
    private val ahPagePattern by playerTryhardSubgroup.pattern(
        "page.ah.loreline",
        "§7\\((?<pagenumber>[0-9]+).*"
    )
    private val notAHPagePattern by playerTryhardSubgroup.pattern(
        "page.notah.loreline",
        "§.Page (?<pagenumber>[0-9]+)"
    )
    private val rngMeterPattern by playerTryhardSubgroup.pattern(
        "rng.meter.loreline",
        "(§.)*Odds: (?<odds>(§.[\\w]){1}).*"
    )
    private val genericDurationPattern by playerTryhardSubgroup.pattern(
        "generic.duration.loreline",
        "(§.)?(([A-z ])+): (§.)?(?<fullDuration>(?<years>[0-9]+y)?[ ]?(?<days>[0-9]+d)?[ ]?(?<hours>[0-9]+h)?[ ]?(?<minutes>[0-9]+m)?[ ]?(?<seconds>[0-9]+s)?)"
    )
    private val totalFamePattern by playerTryhardSubgroup.pattern(
        "total.fame.loreline",
        "(§.)?Your total: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)+) Fame"
    )
    private val bitsAvailablePattern by playerTryhardSubgroup.pattern(
        "bits.available.loreline",
        "(§.)?Bits Available: (§.)?(?<total>(?<useful>[0-9]+)(?<useless>(,[0-9]+))*)(§.)?.*"
    )
    private val magicalPowerPattern by playerTryhardSubgroup.pattern(
        "maxwell.magicalpower.loreline",
        "(§.)?Magical Power: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)"
    )
    private val otherMagicalPowerPattern by playerTryhardSubgroup.pattern(
        "maxwell.othermagicalpower.loreline",
        ".*(§.)?Total: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*"
    )
    private val tuningPointsPattern by playerTryhardSubgroup.pattern(
        "maxwell.tuningpoints.loreline",
        "(§.)?Tuning Points: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)"
    )
    private val slotSourcePattern by playerTryhardSubgroup.pattern(
        "maxwell.slotsource.loreline",
        "(§.)(?<category>(?!Buying).*)?: (§.)?(\\+?)(?<slots>[0-9]+) (s|S)lots"
    )
    private val auctionChestPattern by playerTryhardSubgroup.pattern(
        "ah.chestname",
        "Auction.*"
    )
    private val notAHAbiphoneChestPattern by playerTryhardSubgroup.pattern(
        "not.ah.abiphone.chestname",
        "^(?:(?!Auction|A.iphone|Contacts Directory).)*\$"
    )
    private val genericSelectedFilterSortPattern by playerTryhardSubgroup.pattern(
        "generic.selectedfiltersort.loreline",
        "((?<colorCode>§.)*▶ (?<threeChars>[\\w ]{3}))([\\w ])+"
    )
    private val rngMeterChestPattern by playerTryhardSubgroup.pattern(
        "rng.meter.chestname",
        ".* RNG Meter"
    )
    private val communityEssenceChestPattern by playerTryhardSubgroup.pattern(
        "community.essence.chestname",
        "(Community Shop|.* Essence Shop)"
    )
    private val communityUpgradePattern by playerTryhardSubgroup.pattern(
        "community.isupgrade.variantone.loreline",
        "(.* to start!|.*Maxed out!|.*upgrad.*)"
    )
    private val communityUpgradeOtherPattern by playerTryhardSubgroup.pattern(
        "community.isupgrade.varianttwo.loreline",
        ".* Upgrade"
    )
    private val essenceUpgradePattern by playerTryhardSubgroup.pattern(
        "essenceshopispurchasableupgrade.loreline",
        "(§.)*(.* unlock|UNLOCK).*"
    )
    private val notRomanNumeralPattern by playerTryhardSubgroup.pattern(
        "notromannumeral",
        "^(?:(?!I|V|X|L|C|D|M).)*\$"
    )
    private val ahBZCommunityChestPattern by playerTryhardSubgroup.pattern(
        "ah.bz.community.chestname",
        "(Auction.*|Bazaar.*|Community Shop)"
    )
    private val currentTabPattern by playerTryhardSubgroup.pattern(
        "current.tab.loreline",
        "§aCurrently .*"
    )
    private val notAHBZChestPattern by playerTryhardSubgroup.pattern(
        "not.ah.bz.chestname",
        "(Auction.*|Bazaar.*)"
    )
    private val fameRankPattern by playerTryhardSubgroup.pattern(
        "cookie.famerank.loreline",
        "(§.)*Fame Rank: (§.)*(?<fameRank>[\\w ]+)"
    )
    private val boosterCookieDurationPattern by playerTryhardSubgroup.pattern(
        "cookie.boostercookieduration.loreline",
        "(§.)*Duration: (§.)*(?<fullDuration>(?<years>[0-9]+y)?[ ]?(?<days>[0-9]+d)?[ ]?(?<hours>[0-9]+h)?[ ]?(?<minutes>[0-9]+m)?[ ]?(?<seconds>[0-9]+s)?)"
    )
    private val currentlyActiveEffectsPattern by playerTryhardSubgroup.pattern(
        "cookie.currentlyactiveeffects.loreline",
        "(?:§.)*Currently Active: (?:§.)*(?<effects>[\\w]+)"
    )
    private val upgradesTuningChestPattern by playerTryhardSubgroup.pattern(
        "maxwell.upgrades.tuning.chestname",
        "(Accessory Bag Upgrades|Stats Tuning)"
    )
    private val powerStoneStatusPattern by playerTryhardSubgroup.pattern(
        "maxwell.powerstone.status.loreline",
        "(§.)*Learned: (?<colorCode>§.)*(?<status>[\\w ]+) (?<icon>.)"
    )
    private val startingInPattern by playerTryhardSubgroup.pattern(
        "startingin.loreline",
        "§7Starting in: .*"
    )
    private val startsInPattern by playerTryhardSubgroup.pattern(
        "startsin.loreline",
        "§7Starts in: .*"
    )
    private val achievementPointsPattern by playerTryhardSubgroup.pattern(
        "achievement.points.loreline",
        "(§.)*Points: (§.)*([\\w,]+)(§.)*/(§.)*([\\w,]+) (§.)*\\((?<percent>\\w+)%(§.)*\\)"
    )
    private val bookshelfPowerPattern by playerTryhardSubgroup.pattern(
        "bookshelf.power.loreline",
        "(?:§.)*Bookshelf Power: (?:§.)*(?<power>\\d+)"
    )
    private val contributionsPattern by playerTryhardSubgroup.pattern(
        "project.contributions.variantone.loreline",
        "(?:§.)*You contributed to (?:§.)*(?<contribs>[\\d,.]+)(?: (?:§.)*of(?: (§.)*those(?:(§.)* projects.)?)?)?"
    )
    private val contributionsOtherPattern by playerTryhardSubgroup.pattern(
        "project.contributions.varianttwo.loreline",
        "(?:§.)*You made (?:§.)*(?<contribs>[\\d,.]+)(?: (?:§.)*contributions(?: (?:§.)*to(?: (§.)*this(?:(§.)* project.)?)?)?)?"
    )
    private val contributionsOtherOtherPattern by playerTryhardSubgroup.pattern(
        "project.contributions.variantthree.loreline",
        "(?:§.)*You made: (?:§.)*(?<contribs>[\\d,.]+)(?: contributions)?"
    )
    private val previousProjectsChestPattern by playerTryhardSubgroup.pattern(
        "project.previous.chestname",
        "Previous(?: \\S+)? Projects"
    )
    private val cityProjectItemPattern by playerTryhardSubgroup.pattern(
        "project.contributions.city.project.itemname",
        "City [pP]roject: [\\S ]+"
    )
    private val deliveriesPattern by playerTryhardSubgroup.pattern(
        "island.management.deliveries.loreline",
        "(?:§.)*You have (?<deliveryCount>[\\d,.]+) deliver(?:y|ies) available to(?: (?:§.)*collect\\.)?"
    )
    private val deliveriesItemPattern by playerTryhardSubgroup.pattern(
        "island.management.deliveries.itemname",
        ".* Deliveries"
    )

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
                auctionChestPattern.matchMatcher(chestName) {
                    ahPagePattern.matchMatcher(line) {
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
                notAHPagePattern.matchMatcher(line) {
                    return group("pagenumber")
                }
            }
            notAHAbiphoneChestPattern.matchMatcher(chestName) {
                if (((itemName == ("Sort") && (item.item == Item.getItemFromBlock(Blocks.hopper)))) || ((itemName == ("Filter") && (item.item is ItemEnderEye)))) {
                    for (line in lore) {
                        genericSelectedFilterSortPattern.matchMatcher(line) {
                            return group("threeChars").trim() // trim() to remove spaces. removing the space from the regex causes some filter options to get skipped
                        }
                    }
                }
            }
            ahBZCommunityChestPattern.matchMatcher(chestName) {
                if (itemName.isNotEmpty() && lore.isNotEmpty()) {
                    if (chestName == "Community Shop") {
                        currentTabPattern.matchMatcher(lore.last()) {
                            return "§a⬇"
                        }
                    }
                    notAHBZChestPattern.matchMatcher(chestName) {
                        if (lore.first() == ("§8Category")) {
                            currentTabPattern.matchMatcher(lore.last()) {
                                return "§a➡"
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.RNG_METER_ODDS)) {
            rngMeterChestPattern.matchMatcher(chestName) {
                for (line in item.getLore()) {
                    rngMeterPattern.matchMatcher(line) { return group("odds") }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.COMMUNITY_ESSENCE_UPGRADES)) {
            also {
                val lore = item.getLore()
                var canDisplayTier = false
                communityEssenceChestPattern.matchMatcher(chestName) {
                    if (lore.isNotEmpty()) {
                        communityUpgradePattern.matchMatcher(lore.last()) { canDisplayTier = true }
                        communityUpgradeOtherPattern.matchMatcher(lore.first()) { canDisplayTier = true }
                        essenceUpgradePattern.matchMatcher(lore.last()) { canDisplayTier = true }
                            if (canDisplayTier) {
                                val lastWord = itemName.split(" ").last()
                                for (char in lastWord) {
                                    notRomanNumeralPattern.matchMatcher("$char") {
                                        return@also
                                    }
                                }
                                return lastWord.romanToDecimalIfNecessary().toString()
                            }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.BOOKSHELF_POWER)) {
            if (chestName == "Enchant Item" && itemName == "Bookshelf Power") {
                for (line in item.getLore()) {
                    bookshelfPowerPattern.matchMatcher(line) {
                        return group("power")
                    }
                }
            }
        }

        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.FAME_RANK_BITS)) {
            if ((chestName == "Booster Cookie" && itemName == "Fame Rank")) {
                for (line in item.getLore()) {
                    totalFamePattern.matchMatcher(line) {
                        return NumberUtil.format(group("total").formatNumber())
                    }
                }
            }
            if ((chestName == "Booster Cookie" && itemName == "Bits")) {
                for (line in item.getLore()) {
                    bitsAvailablePattern.matchMatcher(line) {
                        return NumberUtil.format(group("total").formatNumber())
                    }
                }
            }
            if ((chestName == "Community Shop" && itemName == "Community Shop")) {
                for (line in item.getLore()) {
                    fameRankPattern.matchMatcher(line) {
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

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.BOOSTER_COOKIE_POTION_EFFECTS)) {
            if (item.getLore().isNotEmpty() && (itemName == "Booster Cookie" && ((chestName.lowercase() == "skyblock menu") || (chestName == "Booster Cookie")))) {
                for (line in item.getLore()) {
                    boosterCookieDurationPattern.matchMatcher(line) {
                        return getDuration(group("fullDuration")).format(maxUnits = 1)
                    }
                }
            }
            if (itemName == ("Active Effects")) {
                for (line in item.getLore()) {
                    currentlyActiveEffectsPattern.matchMatcher(line) {
                        return group("effects")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.DELIVERIES_PROJECTS)) {
            val lore = item.getLore()
            if (chestName == "Deliveries") {
                deliveriesItemPattern.matchMatcher(itemName) {
                    for (line in lore) {
                        deliveriesPattern.matchMatcher(line) {
                            return NumberUtil.format(group("deliveryCount").formatNumber())
                        }
                    }
                }
            }
            if (chestName == "Community Shop") {
                if (itemName == "Previous Projects") {
                    for (line in lore) {
                        contributionsPattern.matchMatcher(line) {
                            return NumberUtil.format(group("contribs").formatNumber())
                        }
                    }
                }
                if (lore.first() == "§8City Project") {
                    for (line in lore) {
                        contributionsOtherOtherPattern.matchMatcher(line) {
                            return NumberUtil.format(group("contribs").formatNumber())
                        }
                    }
                }
            }
            if (previousProjectsChestPattern.matches(chestName) && cityProjectItemPattern.matches(itemName)) {
                for (line in lore) {
                    contributionsOtherPattern.matchMatcher(line) {
                        return NumberUtil.format(group("contribs").formatNumber())
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.ACCESSORY_BAG_UTILS)) {
            val lore = item.getLore()
            if (chestName == ("Your Bags") && itemName == ("Accessory Bag")) {
                for (line in lore) {
                    magicalPowerPattern.matchMatcher(line) {
                        return NumberUtil.format(group("total").formatNumber())
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
            upgradesTuningChestPattern.matchMatcher(chestName) {
                if (itemName == ("Stats Tuning")) {
                    for (line in lore) {
                        tuningPointsPattern.matchMatcher(line) {
                            return NumberUtil.format(group("total").formatNumber())
                        }
                    }
                }
            }
            if (chestName == ("Power Stones Guide")) {
                if (lore.isNotEmpty()) {
                    powerStoneStatusPattern.matchMatcher(lore.last()) {
                        return "${group("colorCode")}${group("icon")}"
                    }
                }
            }
            if (chestName == ("Accessory Bag Thaumaturgy") && itemName == ("Accessories Breakdown")) {
                for (line in lore) {
                    otherMagicalPowerPattern.matchMatcher(line) {
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
                    startingInPattern.matchMatcher(line) {
                        theStringToUse = line
                    }
                }
            }
            if (lore.isNotEmpty() && chestName == ("Calendar and Events") && !CalendarOverlay.isEnabled()) {
                startsInPattern.matchMatcher(lore.first()) {
                    theStringToUse = lore.first()
                }
            }
            genericDurationPattern.matchMatcher(theStringToUse) {
                return getDuration(group("fullDuration")).format(maxUnits = 1)
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerTryhard.SKYBLOCK_ACHIEVEMENT_POINTS) && (chestName == ("Your Equipment and Stats") && itemName.lowercase() == ("skyblock achievements"))) {
            // §7Points: §e1,995§7/§e2,835 §8(70%§8)
            for (line in item.getLore()) {
                achievementPointsPattern.matchMatcher(line) {
                    return group("percent")
                }
            }
        }
        
        return ""
    }
}
