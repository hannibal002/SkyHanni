package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.data.MayorElection
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerAdvanced : AbstractMenuStackSize() {
    private val playerAdvancedSubgroup = itemStackSizeGroup.group("player.advanced")
    
    // private val genericPercentPattern = ((".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val dojoTestOfGradeLoreLinePattern by playerAdvancedSubgroup.pattern(("dojotestofgrade.loreline"), (".*(§[7|6])Your Rank: (§.)(?<grade>[A-Z]).*"))
    private val skyblockStatBreakdownItemNamePattern by playerAdvancedSubgroup.pattern(("skyblockstatbreakdown.itemname"), ("§(?<color>[0-9a-f])(?<icon>.) (?<name>.*) §f(?<useless>.+)"))
    private val enigmaSoulsLoreLinePattern by playerAdvancedSubgroup.pattern(("enigmasouls.loreline"), ("(§.)?Enigma Souls: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?.*"))
    private val bankBalanceLoreLinePattern by playerAdvancedSubgroup.pattern(("bankbalance.loreline"), ("(§.)?Current balance: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*"))
    private val amtToWithdrawLoreLinePattern by playerAdvancedSubgroup.pattern(("amttowithdraw.loreline"), ("(§.)?Amount to withdraw: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*"))
    private val isNotBankAccountMenuChestNamePattern by playerAdvancedSubgroup.pattern(("isnotbankaccountmenu.chestname"), ("^((?!Bank Account).)*\$"))
    private val isBankBalanceStackTipPattern by playerAdvancedSubgroup.pattern(("isbankbalance.stacktip"), (".*Balance: .*"))
    private val isDepositCoinsItemNamePattern by playerAdvancedSubgroup.pattern(("isdepositcoins.itemname"), (".*Deposit Coins.*"))
    private val mayorAatroxPerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayoraatroxperksforjerryperkapocalypse.loreline"), ("(§.)(SLASHED Pricing|Slayer XP Buff|Pathfinder)"))
    private val mayorColePerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayorcoleperksforjerryperkapocalypse.loreline"), ("(§.)(Prospection|Mining XP Buff|Mining Fiesta)"))
    private val mayorDianaPerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayordianaperksforjerryperkapocalypse.loreline"), ("(§.)(Lucky\\!|Pet XP Buff|Mythological Ritual)"))
    private val mayorFinneganPerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayorfinneganperksforjerryperkapocalypse.loreline"), ("(§.)(Farming Simulator|Pelt-pocalypse|GOATed)"))
    private val mayorFoxyPerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayorfoxyperksforjerryperkapocalypse.loreline"), ("(§.)(Sweet Tooth|Benevolence|Extra Event)"))
    private val mayorMarinaPerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayormarinaperksforjerryperkapocalypse.loreline"), ("(§.)(Luck of the Sea 2.0|Fishing XP Buff|Fishing Festival)"))
    private val mayorPaulPerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayorpaulperksforjerryperkapocalypse.loreline"), ("(§.)(Marauder|EZPZ|Benediction)"))
    private val mayorDiazPerksForJerryPerkapocalypseLoreLinePattern by playerAdvancedSubgroup.pattern(("mayordiazperksforjerryperkapocalypse.loreline"), ("(§.)(Barrier Street|Shopping Spree)"))
    private val recipeBookUnlockedPercentLoreLinePattern by playerAdvancedSubgroup.pattern(("recipebookunlockedpercent.loreline"), (".* Book Unlocked: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val recipeBookOrRecipesChestNamePattern by playerAdvancedSubgroup.pattern(("recipebookorrecipes.chestname"), ("(Recipe Book|.* Recipes)"))
    private val recipesCategoryItemNamePattern by playerAdvancedSubgroup.pattern(("recipescategory.itemname"), (".* Recipes"))
    private val recipesUnlockedPercentProgressLoreLinePattern by playerAdvancedSubgroup.pattern(("recipesunlockedpercentprogress.loreline"), (".*Recipes Unlocked: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val fairySoulsFoundLoreLinePattern by playerAdvancedSubgroup.pattern(("fairysoulsfound.loreline"), (".*(§.)*. (§.)*Found: (§.)*(?<foundSouls>[\\w]+)(§.)*\\/(§.)*(?<maxSouls>[\\w]+).*"))
    private val completedQuestsLoreLinePattern by playerAdvancedSubgroup.pattern(("completedquests.loreline"), ("(§.)*Completed: (§.)*(?<completed>[\\w]+)"))
    private val tradesUnlockedPercentLoreLinePattern by playerAdvancedSubgroup.pattern(("tradesunlockedpercent.loreline"), (".*Trades Unlocked.* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val wardrobeChestNamePattern by playerAdvancedSubgroup.pattern(("wardrobe.chestname"), ("Wardrobe.*"))
    private val wardrobeSlotNumberItemNamePattern by playerAdvancedSubgroup.pattern(("wardrobeslotnumber.itemname"), ("(§.)?Slot (?<slotNumber>[0-9]): .*"))
    private val yourStatsBreakdownChestNamePattern by playerAdvancedSubgroup.pattern(("yourstatsbreakdown.chestname"), ("Your Stats Breakdown.*"))
    private val playingOnCuteNameProfileNameLoreLinePattern by playerAdvancedSubgroup.pattern(("playingoncutenameprofilename.loreline"), (".*(§.)*Playing on: (§.)*(?<cuteName>[\\w]+).*"))
    private val dojoRankTestOfBlankItemNamePattern by playerAdvancedSubgroup.pattern(("dojoranktestofblank.itemname"), ("(Rank|Test of .*)"))
    private val isBankMenuChestNamePattern by playerAdvancedSubgroup.pattern(("isbankmenu.chestname"), (".*Bank.*"))
    private val bankAccountChestNamePattern by playerAdvancedSubgroup.pattern(("bankaccount.chestname"), (".*Bank Account"))
    private val isElectionMenuChestNamePattern by playerAdvancedSubgroup.pattern(("iselectionmenu.chestname"), ("(Election|Election, Year .*)"))
    private val isDictatorDanteItemNamePattern by playerAdvancedSubgroup.pattern(("isdictatordante.itemname"), (".*dante.*"))
    private val calendarAndEventsOrMayorBlankChestNamePattern by playerAdvancedSubgroup.pattern(("calendarandeventsormayorblank.chestname"), ("(Calendar and Events|Mayor .*)"))
    private val holyHeckJerryIsMayorItemNamePattern by playerAdvancedSubgroup.pattern(("holyheckjerryismayor.itemname"), ("JERRY IS MAYOR.*"))
    private val mayorBlankItemNamePattern by playerAdvancedSubgroup.pattern(("mayorblank.itemname"), ("Mayor .*"))
    private val perkpocalypsePerksLoreLinePattern by playerAdvancedSubgroup.pattern(("perkpocalypseperks.loreline"), ("^(§.)Perkpocalypse Perks:\$"))
    private val thesePerksAreAvailableToAllPlayersUntilNextElectionLoremIpsumLoreLinePattern by playerAdvancedSubgroup.pattern(("theseperksareavailabletoallplayersuntilnextelectionloremipsum.loreline"), ("(§.)*(?<apparentlyINeedThis>[A-z]+ )?players until the closing of(?<apparentlyINeedThisToo> [A-z]+)?"))
    private val isNotMayorJerryItemNamePattern by playerAdvancedSubgroup.pattern(("isnotmayorjerry.itemname"), (".*(?<! Jerry)\$"))
    private val isBazaarChestNamePattern by playerAdvancedSubgroup.pattern(("isbazaar.chestname"), ("Bazaar.*"))
    private val thereIsStuffToClaimFromBazaarLoreLinePattern by playerAdvancedSubgroup.pattern(("thereisstufftoclaimfrombazaar.loreline"), (".*to claim!"))
    private val pickUpItemsFromBazaarLoreLinePattern by playerAdvancedSubgroup.pattern(("pickupitemsfrombazaar.loreline"), (".*(§2|§a).*"))
    private val pickUpCoinsFromBazaarLoreLinePattern by playerAdvancedSubgroup.pattern(("pickupcoinsfrombazaar.loreline"), (".*(§6|§e).*"))
    private val instasellIgnoredProductsCountLoreLinePattern by playerAdvancedSubgroup.pattern(("instasellignoredproductscount.loreline"), ("(§.)*Ignored: (§.)*(?<products>\\w+) Products"))
    private val bazaarViewModeItemNamePattern by playerAdvancedSubgroup.pattern(("bazaarviewmode.itemname"), (".* Mode"))
    private val containsArrowChestNamePattern by playerAdvancedSubgroup.pattern(("containsarrow.chestname"), (".* ➜ .*"))
    private val instasellIgnoreItemNamePattern by playerAdvancedSubgroup.pattern(("instasellignore.itemname"), ("Instasell Ignore"))
    private val instasellIgnoreStatusLoreLinePattern by playerAdvancedSubgroup.pattern(("instasellignorestatus.loreline"), ("(§.)*Bulk Instasell: (?<display>(§.)*.)[\\w]+!"))
    private val totalBidsOnItemLoreLinePattern by playerAdvancedSubgroup.pattern(("totalbidsonitem.loreline"), ("(§.)*Total bids: (§.)*(?<bids>[\\S]+) .*"))
    private val isAuctionHouseMainMenuChestNamePattern by playerAdvancedSubgroup.pattern(("isauctionhousemainmenu.chestname"), (".*Auction House"))
    private val isOneBidAndTopBidLoreLinePattern by playerAdvancedSubgroup.pattern(("isonebidandtopbid.loreline"), (".*top bid .*"))
    private val multipleBidsPendingLoreLinePattern by playerAdvancedSubgroup.pattern(("multiplebidspending.loreline"), ("(§.)?You placed (§.)?(?<bids>[0-9]+) bid(.{0,3}) (§.)?on pending"))
    private val yourAuctionsHaveHowManyBidsLoreLinePattern by playerAdvancedSubgroup.pattern(("yourauctionshavehowmanybids.loreline"), ("(§.)*Your (§.)*auction(s?) (§.)*ha(ve|s) (§.)*(?<bidcount>[\\S]+) (§.)*bid(s?)"))
    private val isBrowsingAuctionsChestNamePattern by playerAdvancedSubgroup.pattern(("isbrowsingauctions.chestname"), (".*Auctions(:| ).*"))
    private val auctionHouseSpecificSelectedFilterLoreLinePattern by playerAdvancedSubgroup.pattern(("auctionhousespecificselectedfilter.loreline"), ("(§.)*▶ (?<text>[\\w ]+)"))
    private val auctionHouseSpecificSelectedRarityFilterLoreLinePattern by playerAdvancedSubgroup.pattern(("auctionhousespecificselectedrarityfilter.loreline"), ("((?<colorCode>§.)*▶ (?<singleChar>[\\w ]))([\\w ])+"))
    private val bankAccountTierLoreLinePattern by playerAdvancedSubgroup.pattern(("bankaccounttier.loreline"), ("(§.)*Current account: (?<colorCode>§.)*(?<tier>(?<tierFirstLetter>[\\w])[\\w]+)"))

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
    override fun onRenderItemTip(event: RenderItemTipEvent) {
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
        super.onRenderItemTip(event)
    }


    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.playerAdvanced.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = configMenuStackSize.playerAdvanced
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.UNLOCKED_RECIPES)) {
            if (chestName.lowercase() == ("skyblock menu") && (itemName == "Recipe Book")) {
                for (line in item.getLore()) {
                    recipeBookUnlockedPercentLoreLinePattern.matchMatcher(line) { return group("percent").convertPercentToGreenCheckmark() }
                }
            }
            recipeBookOrRecipesChestNamePattern.matchMatcher(chestName) {
                recipesCategoryItemNamePattern.matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        recipesUnlockedPercentProgressLoreLinePattern.matchMatcher(
                            line
                        ) { return group("percent").convertPercentToGreenCheckmark() }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.FAIRY_ENIGMA_SOULS_QUESTS)) {
            val lore = item.getLore()
            if (chestName == "Quest Log") {
                if (itemName == "Find all Fairy Souls") {
                    for (line in lore) {
                        val totalFairySouls = "242" // change this whenever hypixel adds more fairy souls
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
                tradesUnlockedPercentLoreLinePattern.matchMatcher(line) { return group("percent").convertPercentToGreenCheckmark() }
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
                playingOnCuteNameProfileNameLoreLinePattern.matchMatcher(line) {
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
                val candidates = MayorElection.rawMayorData?.current?.candidates ?: return ""
                for (candidate in candidates) {
                    if (candidate.name == itemName) {
                        return "$colorCode${candidate.perks.size}"
                    }
                }
                /*
                    if (lore.isNotEmpty()) {
                        (("(§8)+.*Candidate").toPattern()).matchMatcher(lore.first()) {
                            val colorCode = nameWithColor.take(2)
                            var numPerks = 0
                            for (line in lore) {
                                line.startsWith(colorCode) &&
                                line != "${colorCode}You voted for this candidate!" &&
                                line != "${colorCode}Leading in votes!" &&
                                !(line.startsWith("${colorCode}Click to vote for ")) &&
                                line != "${colorCode}This is a SPECIAL candidate!" &&
                                !(line.startsWith("$colorCode§"))
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
        // if (lore.any { it == ("SLASHED Pricing") } || lore.any { it == ("Slayer XP Buff") } || lore.any { it == ("Pathfinder") }) return "§bAtx"
        // if (lore.any { it == ("Prospection") } || lore.any { it == ("Mining XP Buff") } || lore.any { it == ("Mining Fiesta") }) return "§bCle"
        // if (lore.any { it == ("Lucky!") } || lore.any { it == ("Pet XP Buff") } || lore.any { it == ("Mythological Ritual") }) return "§bDna"
        // if (lore.any { it == ("Farming Simulator") } || lore.any { it == ("Pelt-pocalypse") } || lore.any { it == ("GOATed") }) return "§bFng"
        // if (lore.any { it == ("Sweet Tooth") } || lore.any { it == ("Benevolence") } || lore.any { it == ("Extra Event") }) return "§bFxy"
        // if (lore.any { it == ("Luck of the Sea 2.0") } || lore.any { it == ("Fishing XP Buff") } || lore.any { it == ("Fishing Festival") }) return "§bMrn"
        // if (lore.any { it == ("Marauder") } || lore.any { it == ("EZPZ") } || lore.any { it == ("Benediction") }) return "§bPul"
        // if (lore.any { it == ("Barrier Street") } || lore.any { it == ("Shopping Spree") }) return "§c§l✖"
        // else return "§c?"
        for (line in lore) {
            mayorAatroxPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bAtx" }
            mayorColePerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bCle" }
            mayorDianaPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bDna" }
            mayorFinneganPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bFng" }
            mayorFoxyPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bFxy" }
            mayorMarinaPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bMrn" }
            mayorPaulPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§bPul" }
            mayorDiazPerksForJerryPerkapocalypseLoreLinePattern.matchMatcher(line) { return "§c§l✖" } //diaz gets an automatic X.
        }
        return "§c?"
    }
}
