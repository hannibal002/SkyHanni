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
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerAdvanced : AbstractMenuStackSize() {
    private val playerAdvancedSubgroup = itemStackSizeGroup.group("player.advanced")
    
    private val dojoGradePattern by playerAdvancedSubgroup.pattern(
        "dojo.grade.loreline",
        ".*(§[7|6])Your Rank: (§.)(?<grade>[A-Z]).*"
    )
    private val statBreakdownPattern by playerAdvancedSubgroup.pattern(
        "stat.breakdown.itemname",
        "§(?<color>[0-9a-f])(?<icon>.) (?<name>.*) §f(?<useless>.+)"
    )
    private val enigmaSoulsPattern by playerAdvancedSubgroup.pattern(
        "enigma.souls.loreline",
        "(§.)?Enigma Souls: (§.)?(?<useful>[0-9]+)(§.)?/(§.)?.*"
    )
    private val bankBalancePattern by playerAdvancedSubgroup.pattern(
        "bank.balance.loreline",
        "(§.)?Current balance: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*"
    )
    private val withdrawPattern by playerAdvancedSubgroup.pattern(
        "bank.withdraw.loreline",
        "(§.)?Amount to withdraw: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*"
    )
    private val notBankAccountChestPattern by playerAdvancedSubgroup.pattern(
        "bank.notbankaccount.chestname",
        "^((?!Bank Account).)*\$"
    )
    private val balanceStackTipPattern by playerAdvancedSubgroup.pattern(
        "bank.balance.stacktip",
        ".*Balance: .*"
    )
    private val depositCoinsPattern by playerAdvancedSubgroup.pattern(
        "bank.deposit.itemname",
        ".*Deposit Coins.*"
    )
    private val aatroxPerksJerryPattern by playerAdvancedSubgroup.pattern(
        "aatrox.perks.jerry.loreline",
        "(§.)(SLASHED Pricing|Slayer XP Buff|Pathfinder)"
    )
    private val colePerksJerryPattern by playerAdvancedSubgroup.pattern(
        "cole.perks.jerry.loreline",
        "(§.)(Prospection|Mining XP Buff|Mining Fiesta)"
    )
    private val dianaPerksJerryPattern by playerAdvancedSubgroup.pattern(
        "diana.perks.jerry.loreline",
        "(§.)(Lucky\\!|Pet XP Buff|Mythological Ritual)"
    )
    private val finneganPerksJerryPattern by playerAdvancedSubgroup.pattern(
        "finnegan.perks.jerry.loreline",
        "(§.)(Farming Simulator|Pelt-pocalypse|GOATed)"
    )
    private val foxyPerksJerryPattern by playerAdvancedSubgroup.pattern(
        "foxy.perks.jerry.loreline",
        "(§.)(Sweet Tooth|Benevolence|Extra Event)"
    )
    private val marinaPerksJerryPattern by playerAdvancedSubgroup.pattern(
        "marina.perks.jerry.loreline",
        "(§.)(Luck of the Sea 2.0|Fishing XP Buff|Fishing Festival)"
    )
    private val paulPerksJerryPattern by playerAdvancedSubgroup.pattern(
        "paul.perks.jerry.loreline",
        "(§.)(Marauder|EZPZ|Benediction)"
    )
    private val diazPerksJerryPattern by playerAdvancedSubgroup.pattern(
        "diaz.perks.jerry.loreline",
        "(§.)(Barrier Street|Shopping Spree)"
    )
    private val recipeBookUnlockedPattern by playerAdvancedSubgroup.pattern(
        "recipebook.unlocked.percent.loreline",
        ".* Book Unlocked: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val recipesChestPattern by playerAdvancedSubgroup.pattern(
        "recipebook.recipes.chestname",
        "(Recipe Book|.* Recipes)"
    )
    private val recipesCategoryPattern by playerAdvancedSubgroup.pattern(
        "recipes.category.itemname",
        ".* Recipes"
    )
    private val recipesUnlockedPattern by playerAdvancedSubgroup.pattern(
        "recipes.unlocked.percent.loreline",
        ".*Recipes Unlocked: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val fairySoulsFoundPattern by playerAdvancedSubgroup.pattern(
        "fairysouls.found.loreline",
        ".*(§.)*. (§.)*Found: (§.)*(?<foundSouls>[\\w]+)(§.)*\\/(§.)*(?<maxSouls>[\\w]+).*"
    )
    private val completedQuestsPattern by playerAdvancedSubgroup.pattern(
        "quests.completed.loreline",
        "(§.)*Completed: (§.)*(?<completed>[\\w]+)"
    )
    private val tradesUnlockedPattern by playerAdvancedSubgroup.pattern(
        "trades.unlocked.percent.loreline",
        ".*Trades Unlocked.* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val wardrobeChestPattern by playerAdvancedSubgroup.pattern(
        "wardrobe.chestname",
        "Wardrobe.*"
    )
    private val wardrobeSlotNumberPattern by playerAdvancedSubgroup.pattern(
        "wardrobe.slotnumber.itemname",
        "(§.)?Slot (?<slotNumber>[0-9]): .*"
    )
    private val statsBreakdownChestPattern by playerAdvancedSubgroup.pattern(
        "stats.breakdown.chestname",
        "Your Stats Breakdown.*"
    )
    private val playingOnPattern by playerAdvancedSubgroup.pattern(
        "playingon.loreline",
        ".*(§.)*Playing on: (§.)*(?<cuteName>[\\w]+).*"
    )
    private val dojoRankTestOfBlankPattern by playerAdvancedSubgroup.pattern(
        "dojo.ranktestofblank.itemname",
        "(Rank|Test of .*)"
    )
    private val isBankMenuChestPattern by playerAdvancedSubgroup.pattern(
        "isbankmenu.chestname",
        ".*Bank.*"
    )
    private val bankAccountChestPattern by playerAdvancedSubgroup.pattern(
        "bankaccount.chestname",
        ".*Bank Account"
    )
    private val isElectionMenuChestPattern by playerAdvancedSubgroup.pattern(
        "iselectionmenu.chestname",
        "(Election|Election, Year .*)"
    )
    private val isDictatorDantePattern by playerAdvancedSubgroup.pattern(
        "isdictatordante.itemname",
        ".*dante.*"
    )
    private val calendarAndEventsOrMayorBlankChestPattern by playerAdvancedSubgroup.pattern(
        "calendarandeventsormayorblank.chestname",
        "(Calendar and Events|Mayor .*)"
    )
    private val holyHeckJerryIsMayorPattern by playerAdvancedSubgroup.pattern(
        "holyheckjerryismayor.itemname",
        "JERRY IS MAYOR.*"
    )
    private val mayorBlankPattern by playerAdvancedSubgroup.pattern(
        "mayorblank.itemname",
        "Mayor .*"
    )
    private val perkpocalypsePerksPattern by playerAdvancedSubgroup.pattern(
        "perkpocalypseperks.loreline",
        "^(§.)Perkpocalypse Perks:\$"
    )
    private val thesePerksAreAvailableToAllPlayersUntilNextElectionLoremIpsumPattern by playerAdvancedSubgroup.pattern(
        "theseperksareavailabletoallplayersuntilnextelectionloremipsum.loreline",
        "(§.)*(?<apparentlyINeedThis>[A-z]+ )?players until the closing of(?<apparentlyINeedThisToo> [A-z]+)?"
    )
    private val isNotMayorJerryPattern by playerAdvancedSubgroup.pattern(
        "isnotmayorjerry.itemname",
        ".*(?<! Jerry)\$"
    )
    private val bzChestPattern by playerAdvancedSubgroup.pattern(
        "bz.chestname",
        "Bz.*"
    )
    private val pickupFromBzPattern by playerAdvancedSubgroup.pattern(
        "bz.pickup.loreline",
        ".*to claim!"
    )
    private val itemsFromBzPattern by playerAdvancedSubgroup.pattern(
        "bz.pickup.items.loreline",
        ".*(§2|§a).*"
    )
    private val coinsFromBzPattern by playerAdvancedSubgroup.pattern(
        "bz.pickup.coins.loreline",
        ".*(§6|§e).*"
    )
    private val instasellIgnoredPattern by playerAdvancedSubgroup.pattern(
        "bz.instasell.loreline",
        "(§.)*Ignored: (§.)*(?<products>\\w+) Products"
    )
    private val bzViewModePattern by playerAdvancedSubgroup.pattern(
        "bz.viewmode.itemname",
        ".* Mode"
    )
    private val hasArrowChestPattern by playerAdvancedSubgroup.pattern(
        "bz.hasarrow.chestname",
        ".* ➜ .*"
    )
    private val instasellIgnorePattern by playerAdvancedSubgroup.pattern(
        "bz.instasell.itemname",
        "Instasell Ignore"
    )
    private val instasellStatusPattern by playerAdvancedSubgroup.pattern(
        "bz.instasell.status.loreline",
        "(§.)*Bulk Instasell: (?<display>(§.)*.)[\\w]+!"
    )
    private val totalBidsPattern by playerAdvancedSubgroup.pattern(
        "ah.totalbids.loreline",
        "(§.)*Total bids: (§.)*(?<bids>[\\S]+) .*"
    )
    private val ahMainMenuChestPattern by playerAdvancedSubgroup.pattern(
        "ah.mainmenu.chestname",
        ".*Auction House"
    )
    private val soleTopBidPattern by playerAdvancedSubgroup.pattern(
        "ah.sole.topbid.loreline",
        ".*top bid .*"
    )
    private val multipleBidsPattern by playerAdvancedSubgroup.pattern(
        "ah.multiplebids.loreline",
        "(§.)?You placed (§.)?(?<bids>[0-9]+) bid(.{0,3}) (§.)?on pending"
    )
    private val yourAuctionsBidsPattern by playerAdvancedSubgroup.pattern(
        "ah.yourauctionshavehowmanybids.loreline",
        "(§.)*Your (§.)*auction(s?) (§.)*ha(ve|s) (§.)*(?<bidcount>[\\S]+) (§.)*bid(s?)"
    )
    private val browsingAuctionsChestPattern by playerAdvancedSubgroup.pattern(
        "ah.browsingauctions.chestname",
        ".*Auctions(:| ).*"
    )
    private val specificSelectedFilterPattern by playerAdvancedSubgroup.pattern(
        "ah.specificselectedfilter.loreline",
        "(§.)*▶ (?<text>[\\w ]+)"
    )
    private val selectedRarityFilterPattern by playerAdvancedSubgroup.pattern(
        "ah.selectedrarityfilter.loreline",
        "((?<colorCode>§.)*▶ (?<singleChar>[\\w ]))([\\w ])+"
    )
    private val bankAccountTierPattern by playerAdvancedSubgroup.pattern(
        "bank.account.tier.loreline",
        "(§.)*Current account: (?<colorCode>§.)*(?<tier>(?<tierFirstLetter>[\\w])[\\w]+)"
    )

    /*
            see the next comment block for context of
            this oddball RenderInventoryItemTipEvent function
            DO NOT CHANGE THE BELOW RenderInventoryItemTipEvent FUNCTION
            UNLESS IF YOU KNOW WHAT YOU'RE DOING!!
            - ery
    */
    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        notBankAccountChestPattern.matchMatcher(event.inventoryName) {
            return
        }
        event.stackTip = getStackTip(event.stack)
        balanceStackTipPattern.matchMatcher(event.stackTip) {
            event.offsetY = -23
            event.offsetX = 0
            event.alignLeft = false
        }
    }

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        depositCoinsPattern.matchMatcher(event.stack.cleanName()) { return }
        /*
            so apparently i have to make a whole separate event for the bank balance display.
            also having everything in this class rely on RenderInventoryItemTipEvent would
            cause weird results in auction house sorting item (and probably other places too,
            but i noticed it first in auction house and immediately sought to fix the issue
            here.

            the above line was the quickest fix without having to write a whole separate function
            specifically for grabbing the bank balance.
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
                    recipeBookUnlockedPattern.matchMatcher(line) { return group("percent").convertPercentToGreenCheckmark() }
                }
            }
            recipesChestPattern.matchMatcher(chestName) {
                recipesCategoryPattern.matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        recipesUnlockedPattern.matchMatcher(
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
                        val totalFairySouls = "242"
                        fairySoulsFoundPattern.matchMatcher(line) {
                            return "§d${group("foundSouls").replace(totalFairySouls, "§a${totalFairySouls}")}"
                        }
                    }
                }
                if (itemName == "Completed Quests") {
                    for (line in lore) {
                        completedQuestsPattern.matchMatcher(line) { return group("completed") }
                    }
                }
            }
            if (chestName == "Rift Guide") {
                if (itemName.isNotEmpty() && lore.isNotEmpty()) {
                    for (line in lore) {
                        enigmaSoulsPattern.matchMatcher(line) { return group("useful") }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.TRADES_UNLOCKED) && (itemName == "Trades")) {
            val lore = item.getLore()
            for (line in lore) {
                tradesUnlockedPattern.matchMatcher(line) { return group("percent").convertPercentToGreenCheckmark() }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.WARDROBE_SLOT)) {
            wardrobeChestPattern.matchMatcher(chestName) {
                wardrobeSlotNumberPattern.matchMatcher(itemName) { return group("slotNumber") }
            }
        }
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.ABBV_STATS)) {
            statsBreakdownChestPattern.matchMatcher(chestName) {
                val statName = item.name ?: return ""
                if (statName.isNotEmpty()) {
                    statBreakdownPattern.matchMatcher(statName) {
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
                playingOnPattern.matchMatcher(line) {
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
            dojoRankTestOfBlankPattern.matchMatcher(itemName) {
                for (line in item.getLore()) {
                    dojoGradePattern.matchMatcher(line) { return group("grade") }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.BANK_UTILS)) {
            val lore = item.getLore()
            isBankMenuChestPattern.matchMatcher(chestName) {
                if (chestName == ("Bank Withdrawal") && itemName == ("Withdraw 20%")) {
                    for (line in lore) {
                        withdrawPattern.matchMatcher(line) {
                            return NumberUtil.format(group("total").formatNumber())
                        }
                    }
                }
            }
            if ((itemName == "Bank Upgrades")) {
                for (line in lore) {
                    bankAccountTierPattern.matchMatcher(line) {
                        return "${group("colorCode")}${group("tierFirstLetter")}"
                    }
                }
            }
            bankAccountChestPattern.matchMatcher(chestName) {
                depositCoinsPattern.matchMatcher(itemName) {
                    bankBalancePattern.matchMatcher(lore.first()) {
                        val totalAsString = NumberUtil.format(group("total").formatNumber())
                        return "§6Balance: $totalAsString"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerAdvanced.MAYOR_PERKS)) {
            val lore = item.getLore()
            isElectionMenuChestPattern.matchMatcher(chestName) {
                if (item.item == Item.getItemFromBlock(Blocks.glass_pane) || item.item == Item.getItemFromBlock(Blocks.stained_glass_pane)) return ""
                isDictatorDantePattern.matchMatcher(itemName.lowercase()) { return bigRedCross }
                val colorCode = item.name?.take(2)
                val candidates = MayorElection.rawMayorData?.current?.candidates ?: return ""
                for (candidate in candidates) {
                    if (candidate.name == itemName) {
                        return "$colorCode${candidate.perks.size}"
                    }
                }
            }
            calendarAndEventsOrMayorBlankChestPattern.matchMatcher(chestName) {
                val nameWithColor = item.name ?: return ""
                val colorCode = nameWithColor.take(2)
                holyHeckJerryIsMayorPattern.matchMatcher(itemName) {
                    return grabPerkpocalypseMayor(lore)
                }
                mayorBlankPattern.matchMatcher(itemName) {
                    isDictatorDantePattern.matchMatcher(itemName.lowercase()) { return bigRedCross }
                    for (line in lore) {
                        perkpocalypsePerksPattern.matchMatcher(line) {
                            return grabPerkpocalypseMayor(lore)
                        }
                        thesePerksAreAvailableToAllPlayersUntilNextElectionLoremIpsumPattern.matchMatcher(line) {
                            isNotMayorJerryPattern.matchMatcher(itemName) {
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
            bzChestPattern.matchMatcher(chestName) {
                if (itemName == "Manage Orders") {
                    var result = ""
                    for (line in lore) {
                        pickupFromBzPattern.matchMatcher(line) {
                            itemsFromBzPattern.matchMatcher(line) { result += "§2☺" }
                            coinsFromBzPattern.matchMatcher(line) { result += "§6☺" }
                        }
                    }
                    return result
                }
                if (itemName == "Instasell Ignore List") {
                    for (line in lore) {
                        instasellIgnoredPattern.matchMatcher(line) {
                            return group("products")
                        }
                    }
                }
                bzViewModePattern.matchMatcher(itemName) {
                    return itemName.take(3)
                }
            }
            hasArrowChestPattern.matchMatcher(chestName) {
                instasellIgnorePattern.matchMatcher(itemName) {
                    for (line in lore) {
                        instasellStatusPattern.matchMatcher(line) {
                            return group("display")
                        }
                    }
                }
            }
            if ((chestName == "Auction View")) {
                if (itemName == "Bid History") {
                    totalBidsPattern.matchMatcher(lore.first()) {
                        return group("bids")
                    }
                }
            }
            ahMainMenuChestPattern.matchMatcher(chestName) {
                if (itemName != "View Bids" && itemName != "Manage Auctions") return ""
                if ((itemName == "View Bids")) {
                    soleTopBidPattern.matchMatcher(lore.first()) {
                        return "1"
                    }
                    multipleBidsPattern.matchMatcher(lore.first()) {
                        return group("bids")
                    }
                }
                if ((itemName == "Manage Auctions")) {
                    for (line in lore) {
                        yourAuctionsBidsPattern.matchMatcher(line) {
                            return group("bidcount")
                        }
                    }
                }
            }
            browsingAuctionsChestPattern.matchMatcher(chestName) {
                if (itemName != "Item Tier" && itemName != "BIN Filter" && itemName != "Sort") return ""
                for (line in lore) {
                    specificSelectedFilterPattern.matchMatcher(line) {
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
                            selectedRarityFilterPattern.matchMatcher(line) {
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
        for (line in lore) {
            aatroxPerksJerryPattern.matchMatcher(line) { return "§bAtx" }
            colePerksJerryPattern.matchMatcher(line) { return "§bCle" }
            dianaPerksJerryPattern.matchMatcher(line) { return "§bDna" }
            finneganPerksJerryPattern.matchMatcher(line) { return "§bFng" }
            foxyPerksJerryPattern.matchMatcher(line) { return "§bFxy" }
            marinaPerksJerryPattern.matchMatcher(line) { return "§bMrn" }
            paulPerksJerryPattern.matchMatcher(line) { return "§bPul" }
            diazPerksJerryPattern.matchMatcher(line) { return bigRedCross }
        }
        return "§c?"
    }
}
