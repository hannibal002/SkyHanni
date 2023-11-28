package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerAdvanced {
    // private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val dojoTestOfGradeLoreLinePattern = ".*(§[7|6])Your Rank: (§.)(?<grade>[A-Z]).*".toPattern()
    private val skyblockStatBreakdownItemNamePattern = "§(?<color>[0-9a-f])(?<icon>.) (?<name>.*) §f(?<useless>.+)".toPattern()
    private val enigmaSoulsLoreLinePattern = "(§.)?Enigma Souls: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?.*".toPattern()
    private val bankBalanceLoreLinePattern = "(§.)?Current balance: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*".toPattern()
    private val amtToWithdrawLoreLinePattern = "(§.)?Amount to withdraw: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*".toPattern()
    private val isNotBankAccountMenuChestNamePattern = (("^((?!Bank Account).)*\$").toPattern())
    private val isBankBalanceStackTipPattern = ((".*Balance: .*").toPattern())
    private val isDepositCoinsItemNamePattern = ((".*Deposit Coins.*").toPattern())
    private val mayorAatroxPerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(SLASHED Pricing|Slayer XP Buff|Pathfinder)").toPattern())
    private val mayorColePerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(Prospection|Mining XP Buff|Mining Fiesta)").toPattern())
    private val mayorDianaPerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(Lucky\\!|Pet XP Buff|Mythological Ritual)").toPattern())
    private val mayorFinneganPerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(Farming Simulator|Pelt-pocalypse|GOATed)").toPattern())
    private val mayorFoxyPerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(Sweet Tooth|Benevolence|Extra Event)").toPattern())
    private val mayorMarinaPerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(Luck of the Sea 2.0|Fishing XP Buff|Fishing Festival)").toPattern())
    private val mayorPaulPerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(Marauder|EZPZ|Benediction)").toPattern())
    private val mayorDiazPerksForJerryPerkapocalypseLoreLinePattern = (("(§.)(Barrier Street|Shopping Spree)").toPattern())
    private val recipeBookUnlockedPercentLoreLinePattern = ((".* Book Unlocked: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val recipeBookOrRecipesChestNamePattern = (("(Recipe Book|.* Recipes)").toPattern())
    private val recipesCategoryItemNamePattern = ((".* Recipes").toPattern())
    private val recipesUnlockedPercentProgressLoreLinePattern = ((".*Recipes Unlocked: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val fairySoulsFoundLoreLinePattern = ((".*(§.)*. (§.)*Found: (§.)*(?<foundSouls>[\\w]+)(§.)*\\/(§.)*(?<maxSouls>[\\w]+).*").toPattern())
    private val completedQuestsLoreLinePattern = (("(§.)*Completed: (§.)*(?<completed>[\\w]+)").toPattern())
    private val tradesUnlockedPercentLoreLinePattern = ((".*Trades Unlocked.* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val wardrobeChestNamePattern = (("Wardrobe.*").toPattern())
    private val wardrobeSlotNumberItemNamePattern = (("(§.)?Slot (?<slotNumber>[0-9]): .*").toPattern())
    private val yourStatsBreakdownChestNamePattern = (("Your Stats Breakdown.*").toPattern())
    private val playingOnCuteNameProfileNamePattern = ((".*(§.)*Playing on: (§.)*(?<cuteName>[\\w]+).*").toPattern())
    private val dojoRankTestOfBlankItemNamePattern = (("(Rank|Test of .*)").toPattern())
    private val isBankMenuChestNamePattern = ((".*Bank.*").toPattern())
    private val bankAccountChestNamePattern = ((".*Bank Account").toPattern())
    private val isElectionMenuChestNamePattern = (("(Election|Election, Year .*)").toPattern())
    private val isDictatorDanteItemNamePattern = ((".*dante.*").toPattern())
    private val calendarAndEventsOrMayorBlankChestNamePattern = (("(Calendar and Events|Mayor .*)").toPattern())
    private val holyHeckJerryIsMayorItemNamePattern = (("JERRY IS MAYOR.*").toPattern())
    private val mayorBlankItemNamePattern = (("Mayor .*").toPattern())
    private val perkpocalypsePerksLoreLinePattern = (("^(§.)Perkpocalypse Perks:\$").toPattern())
    private val thesePerksAreAvailableToAllPlayersUntilNextElectionLoremIpsumLoreLinePattern = (("§7(players until the closing of|until the closing of the next)").toPattern())
    private val isNotMayorJerryItemNamePattern = ((".*(?<! Jerry)\$").toPattern())
    private val isBazaarChestNamePattern = (("Bazaar.*").toPattern())
    private val thereIsStuffToClaimFromBazaarLoreLinePattern = ((".*to claim!").toPattern())
    private val pickUpItemsFromBazaarLoreLinePattern = ((".*(§2|§a).*").toPattern())
    private val pickUpCoinsFromBazaarLoreLinePattern = ((".*(§6|§e).*").toPattern())
    private val instasellIgnoredProductsCountLoreLinePattern = (("(§.)*Ignored: (§.)*(?<products>\\w+) Products").toPattern())
    private val bazaarViewModeItemNamePattern = ((".* Mode").toPattern())
    private val containsArrowChestNamePattern = ((".* ➜ .*").toPattern())
    private val instasellIgnoreItemNamePattern = (("Instasell Ignore").toPattern())
    private val instasellIgnoreStatusLoreLinePattern = (("(§.)*Bulk Instasell: (?<display>(§.)*A)llowed!").toPattern())
    private val totalBidsOnItemLoreLinePattern = (("(§.)*Total bids: (§.)*(?<bids>[\\S]+) .*").toPattern())
    private val isAuctionHouseMainMenuChestNamePattern = ((".*Auction House").toPattern())
    private val isOneBidAndTopBidLoreLinePattern = ((".*top bid .*").toPattern())
    private val multipleBidsPendingLoreLinePattern = (("(§.)?You placed (§.)?(?<bids>[0-9]+) bid(.{0,3}) (§.)?on pending").toPattern())
    private val yourAuctionsHaveHowManyBidsLoreLinePattern = (("(§.)*Your (§.)*auction(s?) (§.)*ha(ve|s) (§.)*(?<bidcount>[\\S]+) (§.)*bid(s?)").toPattern())
    private val isBrowsingAuctionsChestNamePattern = ((".*Auctions(:| ).*").toPattern())
    private val auctionHouseSpecificSelectedFilterLoreLinePattern = (("(§.)*▶ (?<text>[\\w ]+)").toPattern())
    private val auctionHouseSpecificSelectedRarityFilterLoreLinePattern = (("((?<colorCode>§.)*▶ (?<singleChar>[\\w ]))([\\w ])+").toPattern())
    private val bankAccountTierLoreLinePattern = (("(§.)*Current account: (?<colorCode>§.)*(?<tier>(?<tierFirstLetter>[\\w])[\\w]+)").toPattern())

    /*
            see the next comment block for context of
            this oddball RenderInventoryItemTipEvent function
            DO NOT CHANGE THE BELOW RenderInventoryItemTipEvent FUNCTION
            UNLESS IF YOU KNOW WHAT YOU'RE DOING!!
            - ery
            */
    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        isNotBankAccountMenuChestNamePattern.matchMatcher(event.inventoryName) {
            return
        }
        event.stackTip = getStackTip(event.stack)
        isBankBalanceStackTipPattern.matchMatcher(event.stackTip) {
            event.offsetY = -23
            event.offsetX = 0
            event.alignLeft = false
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        isDepositCoinsItemNamePattern.matchMatcher(event.stack.cleanName()) { return }
        /*
        so apparently i have to make a whole separate event for the bank balance display.
        also having everything in this class rely on RenderInventoryItemTipEvent would
        cause weird results in auction house sorting item (and probably other places too,
        but i noticed it first in auction house and immediately sought to fix the issue
        here.

        the above line was the quickest fix without having to write a whole separate function
        specifically for grabbing the bank balance.

        therefore:

        DO NOT CHANGE ANYTHING INSIDE THIS RenderItemTipEvent FUNCTION
        UNLESS IF YOU KNOW WHAT YOU'RE DOING!!

        - ery
        */
        event.stackTip = getStackTip(event.stack)
    }


    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.playerAdvanced.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.playerAdvanced
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.UNLOCKED_RECIPES)) {
            if (chestName.lowercase() == ("skyblock menu") && (itemName == "Recipe Book")) {
                for (line in item.getLore()) {
                    recipeBookUnlockedPercentLoreLinePattern.matchMatcher(line) { return group("percent").replace("100", "§a✔") }
                }
            }
            recipeBookOrRecipesChestNamePattern.matchMatcher(chestName) {
                recipesCategoryItemNamePattern.matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        recipesUnlockedPercentProgressLoreLinePattern.matchMatcher(
                            line
                        ) { return group("percent").replace("100", "§a✔") }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.FAIRY_ENIGMA_SOULS_QUESTS)) {
            val lore = item.getLore()
            if (chestName == "Quest Log") {
                if (itemName == "Find all Fairy Souls") {
                    for (line in lore) {
                        val totalFairySouls = "242" //change this whenever hypixel adds more fairy souls
                        // §a✔ §eFound: §d242§7/§d242 (TY COBBLE8 FOR THIS SAMPLE)
                        // ✔ Found: 242/242
                        fairySoulsFoundLoreLinePattern.matchMatcher(line) {
                            return "§d${group("foundSouls").replace(totalFairySouls, "§a${totalFairySouls}")}"
                        }
                    }
                }
                if (itemName == "Completed Quests") {
                    for (line in lore) {
                        completedQuestsLoreLinePattern.matchMatcher(line) { return group("completed") }
                    }
                }
            }
            if (chestName == "Rift Guide") {
                if (itemName.isNotEmpty() && lore.isNotEmpty()) {
                    for (line in lore) {
                        enigmaSoulsLoreLinePattern.matchMatcher(line) { return group("useful") }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.TRADES_UNLOCKED) && (itemName == "Trades")) {
            val lore = item.getLore()
            for (line in lore) {
                tradesUnlockedPercentLoreLinePattern.matchMatcher(line) { return group("percent").replace("100", "§a✔") }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.WARDROBE_SLOT)) {
            wardrobeChestNamePattern.matchMatcher(chestName) {
                wardrobeSlotNumberItemNamePattern.matchMatcher(itemName) { return group("slotNumber") }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.ABBV_STATS)) {
            yourStatsBreakdownChestNamePattern.matchMatcher(chestName) {
                val statName = item.name ?: return ""
                if (statName.isNotEmpty()) {
                    skyblockStatBreakdownItemNamePattern.matchMatcher(statName) {
                        val name = group("name")
                        val color = group("color")
                        val icon = group("icon")
                        val abbv = when (name) {
                            "Health" -> "HP"
                            "Defense" -> "Def"
                            "Strength" -> "Str"
                            "Intelligence" -> "Int"
                            "Crit Damage" -> "CD"
                            "Crit Chance" -> "CC"
                            "Ferocity" -> "Fer"
                            "Vitality" -> "Vit"
                            "Mending" -> "Mnd"
                            "Speed" -> "Spd"
                            "Sea Creature Chance" -> "SCC"
                            "Magic Find" -> "MF"
                            "Fishing Speed" -> "FiS"
                            "Combat Wisdom" -> "CoW"
                            "Mining Wisdom" -> "MiW"
                            "Farming Wisdom" -> "FaW"
                            "Foraging Wisdom" -> "FoW"
                            "Fishing Wisdom" -> "FiW"
                            "Enchanting Wisdom" -> "EnW"
                            "Alchemy Wisdom" -> "AlW"
                            "Carpentry Wisdom" -> "CaW"
                            "Runecrafting Wisdom" -> "RuW"
                            "Social Wisdom" -> "SoW"
                            "Mining Speed" -> "MiS"
                            "Breaking Power" -> "BP"
                            "Foraging Fortune" -> "FoF"
                            "Farming Fortune" -> "FaF"
                            "Mining Fortune" -> "MiF"
                            else -> icon
                        }
                        return "§$color$abbv"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.CUTE_NAME) && (chestName.lowercase() == ("skyblock menu"))) {
            if (itemName != "Profile Management") return ""
            val lore = item.getLore()
            for (line in lore) {
                playingOnCuteNameProfileNamePattern.matchMatcher(line) {
                    return when (val profileName = group("cuteName")) {
                        "Apple" -> "Apl"
                        "Banana" -> "Bna"
                        "Blueberry" -> "Blu"
                        "Coconut" -> "Ccn"
                        "Cucumber" -> "Ccb"
                        "Grapes" -> "Grp"
                        "Kiwi" -> "Kwi"
                        "Lemon" -> "Lmn"
                        "Lime" -> "Lim"
                        "Mango" -> "Mng"
                        "Not Allowed To Quit Skyblock Ever Again" -> "Akin"
                        "Orange" -> "Org"
                        "Papaya" -> "Ppy"
                        "Pear" -> "Pr"
                        "Peach" -> "Pch"
                        "Pineapple" -> "Pnp"
                        "Pomegranate" -> "Pom"
                        "Raspberry" -> "Rsp"
                        "Strawberry" -> "Stb"
                        "Tomato" -> "Tmt"
                        "Watermelon" -> "Wlm"
                        "Zucchini" -> "Zch"
                        else -> profileName.take(3)
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.DOJO_PROGRESS) && (chestName == ("Challenges"))) {
            dojoRankTestOfBlankItemNamePattern.matchMatcher(itemName) {
                for (line in item.getLore()) {
                    dojoTestOfGradeLoreLinePattern.matchMatcher(line) { return group("grade") }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.BANK_UTILS)) {
            val lore = item.getLore()
            isBankMenuChestNamePattern.matchMatcher(chestName) {
                if (chestName == ("Bank Withdrawal") && itemName == ("Withdraw 20%")) {
                    for (line in lore) {
                        amtToWithdrawLoreLinePattern.matchMatcher(line) {
                            return NumberUtil.format(group("total").formatNumber())
                        }
                    }
                }
            }
            if ((itemName == "Bank Upgrades")) {
                for (line in lore) {
                    bankAccountTierLoreLinePattern.matchMatcher(line) {
                        return "${group("colorCode")}${group("tierFirstLetter")}"
                    }
                }
            }
            bankAccountChestNamePattern.matchMatcher(chestName) {
                isDepositCoinsItemNamePattern.matchMatcher(itemName) {
                    bankBalanceLoreLinePattern.matchMatcher(lore.first()) {
                        val totalAsString = NumberUtil.format(group("total").formatNumber())
                        return "§6Balance: $totalAsString"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.MAYOR_PERKS)) {
            val lore = item.getLore()
            isElectionMenuChestNamePattern.matchMatcher(chestName) {
                if (item.item == Item.getItemFromBlock(Blocks.glass_pane) || item.item == Item.getItemFromBlock(Blocks.stained_glass_pane)) return ""
                isDictatorDanteItemNamePattern.matchMatcher(itemName.lowercase()) { return "§c§l✖" } //all of my homies possess a strong dislike towards dante
                val colorCode = item.name?.take(2)
                val candidates = MayorElection.rawMayorData!!.current!!.candidates!!
                for (candidate in candidates) {
                    if (candidate.name == itemName) {
                        return "$colorCode${candidate.perks.size}"
                    }
                }
/*          if (lore.isNotEmpty()) {
                    (("(§8)+.*Candidate").toPattern()).matchMatcher(lore.first()) {
                        val colorCode = nameWithColor.take(2)
                        var numPerks = 0
                        for (line in lore) {
                            //line.startsWith(colorCode) &&
                            //line != "${colorCode}You voted for this candidate!" &&
                            //line != "${colorCode}Leading in votes!" &&
                            //!(line.startsWith("${colorCode}Click to vote for ")) &&
                            //line != "${colorCode}This is a SPECIAL candidate!" &&
                            //!(line.startsWith("$colorCode§"))
                            (("^${colorCode}(?:(?!\\b(This is a SPECIAL candidate|Click to vote for|You voted for this candidate|Leading in votes)\\b)\\S)*\$").toPattern()).matchMatcher((line)) {
                                numPerks++
                            }
                        }
                        return "$colorCode$numPerks"
                    }
                 }
 */
            }
            calendarAndEventsOrMayorBlankChestNamePattern.matchMatcher(chestName) {
                val nameWithColor = item.name ?: return ""
                val colorCode = nameWithColor.take(2)
                holyHeckJerryIsMayorItemNamePattern.matchMatcher(itemName) {
                    return grabPerkpocalypseMayor(lore)
                }
                mayorBlankItemNamePattern.matchMatcher(itemName) {
                    isDictatorDanteItemNamePattern.matchMatcher(itemName.lowercase()) { return "§c§l✖" }
                    for (line in lore) {
                        perkpocalypsePerksLoreLinePattern.matchMatcher(line) {
                            return grabPerkpocalypseMayor(lore)
                        }
                        thesePerksAreAvailableToAllPlayersUntilNextElectionLoremIpsumLoreLinePattern.matchMatcher(line) {
                            isNotMayorJerryItemNamePattern.matchMatcher(itemName) {
                                /*
                                var numPerks = 0
                                for (line in lore) {
                                    if (line.startsWith(colorCode) && !(line.startsWith("$colorCode§"))) {
                                        numPerks++
                                    }
                                }
                                */
                                val numPerks = MayorElection.currentCandidate?.perks?.size ?: -1
                                return "$colorCode$numPerks"
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.AUCTION_BAZAAR_VARIOUS)) {
            if (itemName.isEmpty()) return ""
            val lore = item.getLore()
            isBazaarChestNamePattern.matchMatcher(chestName) {
                if (itemName == "Manage Orders") {
                    var result = ""
                    for (line in lore) {
                        thereIsStuffToClaimFromBazaarLoreLinePattern.matchMatcher(line) {
                            pickUpItemsFromBazaarLoreLinePattern.matchMatcher(line) { result += "§2☺" }
                            pickUpCoinsFromBazaarLoreLinePattern.matchMatcher(line) { result += "§6☺" }
                        }
                    }
                    return result
                }
                if (itemName == "Instasell Ignore List") {
                    for (line in lore) {
                        instasellIgnoredProductsCountLoreLinePattern.matchMatcher(line) {
                            return group("products")
                        }
                    }
                }
                bazaarViewModeItemNamePattern.matchMatcher(itemName) {
                    return itemName.take(3)
                }
            }
            containsArrowChestNamePattern.matchMatcher(chestName) {
                instasellIgnoreItemNamePattern.matchMatcher(itemName) {
                    for (line in lore) {
                        instasellIgnoreStatusLoreLinePattern.matchMatcher(line) {
                            return group("display")
                        }
                    }
                }
            }
            if ((chestName == "Auction View")) {
                if (itemName == "Bid History") {
                    totalBidsOnItemLoreLinePattern.matchMatcher(lore.first()) {
                        return group("bids")
                    }
                }
            }
            isAuctionHouseMainMenuChestNamePattern.matchMatcher(chestName) {
                if (itemName != "View Bids" && itemName != "Manage Auctions") return ""
                if ((itemName == "View Bids")) {
                    isOneBidAndTopBidLoreLinePattern.matchMatcher(lore.first()) {
                        return "1"
                    }
                    multipleBidsPendingLoreLinePattern.matchMatcher(lore.first()) {
                        return group("bids")
                    }
                }
                if ((itemName == "Manage Auctions")) {
                    for (line in lore) {
                        yourAuctionsHaveHowManyBidsLoreLinePattern.matchMatcher(line) {
                            return group("bidcount")
                        }
                    }
                }
            }
            isBrowsingAuctionsChestNamePattern.matchMatcher(chestName) {
                if (itemName != "Item Tier" && itemName != "BIN Filter" && itemName != "Sort") return ""
                for (line in lore) {
                    auctionHouseSpecificSelectedFilterLoreLinePattern.matchMatcher(line) {
                        val theText = group("text")
                        if (itemName == "Sort") {
                            return when (theText) {
                                "Highest Price" -> "§c⬆"
                                "Lowest Price" -> "§a⬇"
                                "Highest Bid" -> "§c⬆"
                                "Lowest Bid" -> "§a⬇"
                                "Ending soon" -> "§e☉"
                                "Random" -> "R"
                                else -> ""
                            }
                        }
                        if (itemName == "Item Tier") {
                            auctionHouseSpecificSelectedRarityFilterLoreLinePattern.matchMatcher(line) {
                                return "${group("colorCode")}${group("singleChar")}"
                            }
                        }
                        if (itemName == "BIN Filter") {
                            return when (theText) {
                                "Show All" -> "All"
                                "BIN Only" -> "§2BIN"
                                "Auctions Only" -> "§6Auc"
                                else -> ""
                            }
                        }
                    }
                }
            }
        }
        
        return ""
    }

    private fun grabPerkpocalypseMayor(lore: List<String>): String {
//         if (lore.any { it == ("SLASHED Pricing") } || lore.any { it == ("Slayer XP Buff") } || lore.any { it == ("Pathfinder") }) return "§bAtx"
//         if (lore.any { it == ("Prospection") } || lore.any { it == ("Mining XP Buff") } || lore.any { it == ("Mining Fiesta") }) return "§bCle"
//         if (lore.any { it == ("Lucky!") } || lore.any { it == ("Pet XP Buff") } || lore.any { it == ("Mythological Ritual") }) return "§bDna"
//         if (lore.any { it == ("Farming Simulator") } || lore.any { it == ("Pelt-pocalypse") } || lore.any { it == ("GOATed") }) return "§bFng"
//         if (lore.any { it == ("Sweet Tooth") } || lore.any { it == ("Benevolence") } || lore.any { it == ("Extra Event") }) return "§bFxy"
//         if (lore.any { it == ("Luck of the Sea 2.0") } || lore.any { it == ("Fishing XP Buff") } || lore.any { it == ("Fishing Festival") }) return "§bMrn"
//         if (lore.any { it == ("Marauder") } || lore.any { it == ("EZPZ") } || lore.any { it == ("Benediction") }) return "§bPul"
//         if (lore.any { it == ("Barrier Street") } || lore.any { it == ("Shopping Spree") }) return "§c§l✖" //diaz gets an automatic X.
//         else return "§c?"
        for (line in lore) {
            mayorAatroxPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bAtx" }
            mayorColePerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bCle" }
            mayorDianaPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bDna" }
            mayorFinneganPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bFng" }
            mayorFoxyPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bFxy" }
            mayorMarinaPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bMrn" }
            mayorPaulPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bPul" }
            mayorDiazPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§c§l✖" }
        }
        return "§c?"
    }
}
