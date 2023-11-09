package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.InventoryConfig
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.anyContains
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerAdvanced {
    private val dojoTestOfGradePattern = "§7(§6)?Your Rank: (?<grade>§.[A-Z]).*".toPattern()
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val skyblockStatBreakdownPattern = "§(?<color>[0-9a-f])(?<icon>.) (?<name>.*) §f(?<useless>.+)".toPattern()
    private val enigmaSoulsPattern = "(§.)?Enigma Souls: (§.)?(?<useful>[0-9]+)(§.)?\\/(§.)?.*".toPattern()
    private val bankBalancePattern = "(§.)?Current balance: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*".toPattern()
    private val amtToWithdrawPattern = "(§.)?Amount to withdraw: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*).*".toPattern()

    /*
    see the next comment block for context of
    this oddball RenderInventoryItemTipEvent function
    DO NOT CHANGE THE BELOW RenderInventoryItemTipEvent FUNCTION
    UNLESS IF YOU KNOW WHAT YOU'RE DOING!!
    - ery
    */
    @SubscribeEvent
    fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!(event.inventoryName.contains("Bank Account"))) return
        event.stackTip = getStackTip(event.stack)
        if (event.stackTip.contains("Balance: ")) {
            event.offsetY = -23
            event.offsetX = 0
            event.alignLeft = false
        }
    }

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        if (event.stack.cleanName().contains("Deposit Coins")) return
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

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.UNLOCKED)) {
            if (chestName.lowercase() == ("skyblock menu") && (itemName == "Recipe Book")) {
                for (line in item.getLore()) {
                    if (line.contains(" Book Unlocked: ")) {
                        return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                    }
                }
            }
            if (((chestName == "Recipe Book") || (chestName.contains(" Recipes"))) && (itemName.contains(" Recipes"))) {
                for (line in item.getLore()) {
                    if (line.contains("Recipes Unlocked: ")) {
                        return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.FAIRY_ENIGMA)) {
            val lore = item.getLore()
            if (chestName == "Quest Log") {
                if (itemName == "Find all Fairy Souls") {
                    for (line in lore) {
                        val newLine = line.removeColor()
                        val totalFairySouls = "242" //change this whenever hypixel adds more fairy souls
                        // §a✔ §eFound: §d242§7/§d242 (TY COBBLE8 FOR THIS SAMPLE)
                        // ✔ Found: 242/242
                        if (newLine.contains("Found: ")) {
                            return "§d" + newLine.removeColor().replace(" ✖ Found: ", "").replace(" ✔ Found: ", "").replace(("/$totalFairySouls"), "").replace(totalFairySouls, "§a${totalFairySouls}")
                        }
                    }
                }
                if (itemName == "Completed Quests") {
                    for (line in lore) {
                        if (line.contains("§7Completed: §a")) {
                            return "§a" + line.removeColor().replace("Completed: ", "")
                        }
                    }
                }
            }
            if (chestName == "Rift Guide") {
                if (itemName.isNotEmpty() && lore.isNotEmpty()) {
                    if (lore.anyContains("Enigma Souls: ")) {
                        for (line in lore) {
                            if (line.contains("Enigma Souls: ")) {
                                enigmaSoulsPattern.matchMatcher(line) {
                                    return group("useful")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.TRADES) && (itemName == "Trades")) {
            val lore = item.getLore()
            for (line in lore) {
                if (line.contains("Trades Unlocked")) {
                    return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                }
            }
        }
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.WARDROBE) && (chestName.startsWith("Wardrobe") && (itemName.startsWith("Slot ") && itemName.contains(":")))) {
            return itemName.replace("Slot ", "").substring(0,2).trim().replace(":", "")
        }
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.STATS) && chestName.startsWith("Your Stats Breakdown")) {
            val statName = item.name ?: return ""
            if (statName.isNotEmpty()) {
                skyblockStatBreakdownPattern.matchMatcher(statName) {
                    val name = group("name")
                    val color = group("color")
                    val icon = group("icon")
                    val me = when (name) {
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
                        else -> "[icon]"
                    }
                    return "" + "§" + color + me.replace("[icon]", icon)
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.FRUITS) && (chestName.lowercase() == ("skyblock menu"))) {
            val nameWithColor = item.name ?: return ""
            if (nameWithColor != "§aProfile Management") return ""
            val lore = item.getLore()
            for (line in lore) {
                if (line.contains("§7Playing on: §a")) {
                    return when (val profileName = line.replace("§7Playing on: §a", "").removeColor().trim()) {
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

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.DOJO) && (chestName.endsWith("Challenges") && (itemName.startsWith("Test of ") || itemName == ("Rank")))) {
            for (line in item.getLore()) {
                if (line.contains("Your Rank:")) {
                    dojoTestOfGradePattern.matchMatcher(line) {
                        return group("grade").removeColor()
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.BANK) && chestName.contains("Bank")) {
            val lore = item.getLore()
            if (chestName.contains("Withdrawal") && itemName.contains("Withdraw 20%") && lore.anyContains("Amount to withdraw: ")) {
                for (line in lore) {
                    if (line.contains("Amount to withdraw: ")) {
                        amtToWithdrawPattern.matchMatcher(line) {
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
                            else return "§6$usefulPartAsString$suffix"
                        }
                    }
                }
            }
            if ((itemName == "Bank Upgrades")) {
                for (line in lore) {
                    if (line.startsWith("§7Current account: ")) {
                        return line.removeColor().replace("Current account: ", "").substring(0,1)
                    }
                }
            }
            if (chestName.endsWith("Bank Account") && itemName.contains("Deposit Coins")) {
                bankBalancePattern.matchMatcher(lore.first()) {
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
                    if (suffix == "§b§z:)") return "§6Balance: $suffix"
                    else return "§6Balance: $usefulPartAsString$suffix"
                }
            }
        }
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.MAYOR_PERKS)) {
            val lore = item.getLore()
            if ((chestName.contains("Election"))) {
                if (itemName.lowercase().contains("dante")) return "§c§l✖"
                val nameWithColor = item.name ?: return ""
                if (item.getItem() == Item.getItemFromBlock(Blocks.glass_pane) || item.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)) return ""
                if (lore.isNotEmpty()) {
                    if (lore.anyContains("Candidate")) {
                        val colorCode = nameWithColor.take(2)
                        var numPerks = 0
                        for (line in item.getLore()) {
                            if (line.startsWith(colorCode) &&
                                !(line.contains("You voted for this candidate!")) &&
                                !(line.contains("Leading in votes!")) &&
                                !(line.contains("Click to vote for ")) && 
                                !(line.contains("SPECIAL ")) && 
                                !(line.startsWith("$colorCode§"))) {
                                    numPerks++
                            }
                        }
                        return "" + colorCode + numPerks
                    }
                }
            }
            if (((chestName == "Calendar and Events") || (chestName.contains("Mayor ")))) {
                if (itemName.startsWith("JERRY IS MAYOR")) {
                    return grabPerkpocalypseMayor(lore)
                }
                if (itemName.contains("Mayor ")) {
                    if (itemName.lowercase().contains("dante")) return "§c§l✖"
                    val nameWithColor = item.name ?: return ""
                    if (lore.anyContains(" the closing of")) {
                        if (!(itemName.contains("Jerry")) || chestName.contains("Mayor")) {
                            val colorCode = nameWithColor.take(2)
                            var numPerks = 0
                            for (line in lore) {
                                if (line.startsWith(colorCode) && !(line.startsWith("$colorCode§"))) {
                                    numPerks++
                                }
                            }
                            return "" + colorCode + numPerks
                        } else {
                            for (line in lore) {
                                if (lore.anyContains("Perkpocalypse")) {
                                    return grabPerkpocalypseMayor(lore)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerAdvanced.AH_BZ)) {
            if (itemName.isEmpty()) return ""
            val lore = item.getLore()
            if (chestName.contains("Bazaar")) {
                if (itemName == "Manage Orders") {
                    var result = ""
                    for (line in lore) {
                        if (line.endsWith("to claim!")) {
                            if (line.contains("§2") || line.contains("§a")) result += "§2☺"
                            if (line.contains("§6") || line.contains("§e")) result += "§6☺"
                        }
                    }
                    return result
                }
                if (itemName == "Instasell Ignore List" && lore.anyContains("Ignored: ")) {
                    for (line in lore) {
                        if (line.contains("Ignored: ")) { //§7Ignored: §c47 Products --> Ignored: 47 Products --> Ignored: 47 Product --> 47
                            return line.removeColor().replace("Products", "Product").split("Ignored: ", " Product")[1]
                        }
                    }
                }
                if (itemName.endsWith(" Mode")) {
                    return itemName.take(3)
                }
            }
            if (chestName.contains(" ➜ ") && itemName.contains("Instasell Ignore") && lore.anyContains("Bulk Instasell: ")) {
                for (line in lore) {
                    if (line.contains("Bulk Instasell: ")) {
                        //§cIgnored!
                        //§aAllowed!
                        return line.replace("Bulk Instasell: ", "").take(5)
                    }
                }
            }
            if ((chestName == "Auction View")) {
                if (itemName != "Bid History") return ""
                if (!(lore.first().contains("Total "))) return ""
                return lore.first().removeColor().replace("Total bids: ", "").replace(" bids", "").replace(" bid", "")
            }
            if ((chestName.contains("Auction House"))) {
                if (itemName != "View Bids" && itemName != "Manage Auctions") return ""
                if ((itemName == "View Bids") && (lore.first().removeColor().contains(" top bid ") || lore.first().removeColor().contains("a bid "))) return "1"
                if ((itemName == "View Bids") && lore.first().contains("You placed ")) "(§.)?You placed (§.)?(?<bids>[0-9]+) bid(.{0,3}) (§.)?on pending".toPattern().matchMatcher(lore.first()) { return group("bids") }
                if ((itemName == "Manage Auctions")) {
                    for (line in lore) {
                        if (line.contains("Your auction")) {
                            return line.removeColor().replace("Your auctions have ", "").replace("Your auction has ", "").replace(" bids", "").replace(" bid", "")
                        }
                    }
                }
            }
            if (chestName.contains("Auction")) {
                if (itemName != "Item Tier" && itemName != "BIN Filter" && itemName != "Sort") return ""
                for (line in lore) {
                    if (line.contains("▶ ")) {
                        val betterLine = line.removeColor().replace("▶ ", "")
                        if (itemName == "Sort") {
                            return when (betterLine.replace(" Price", "").replace(" Bid", "")) {
                                "Highest" -> "§c⬆"
                                "Lowest" -> "§a⬇"
                                "Ending soon" -> "§e☉"
                                "Random" -> "R"
                                else -> ""
                            }
                        }
                        if (itemName == "Item Tier") {
                            return line.take(5).replace("▶ ", "")
                        }
                        if (itemName == "BIN Filter") {
                            return when (betterLine) {
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
        if (lore.anyContains("SLASHED Pricing") || lore.anyContains("Slayer XP Buff") || lore.anyContains("Pathfinder")) return "§bAtx"
        if (lore.anyContains("Prospection") || lore.anyContains("Mining XP Buff") || lore.anyContains("Mining Fiesta")) return "§bCle"
        if (lore.anyContains("Lucky!") || lore.anyContains("Pet XP Buff") || lore.anyContains("Mythological Ritual")) return "§bDna"
        if (lore.anyContains("Barrier Street") || lore.anyContains("Shopping Spree")) return "§c§l✖" //diaz gets an automatic X.
        if (lore.anyContains("Farming Simulator") || lore.anyContains("Pelt-pocalypse") || lore.anyContains("GOATed")) return "§bFng"
        if (lore.anyContains("Sweet Tooth") || lore.anyContains("Benevolence") || lore.anyContains("Extra Event")) return "§bFxy"
        if (lore.anyContains("Luck of the Sea 2.0") || lore.anyContains("Fishing XP Buff") || lore.anyContains("Fishing Festival")) return "§bMrn"
        if (lore.anyContains("Marauder") || lore.anyContains("EZPZ") || lore.anyContains("Benediction")) return "§bPul"
        else return "§c?"
    }
}
