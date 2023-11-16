package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.InventoryConfig
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerAdvanced {
    private val dojoTestOfGradePattern = ".*(§[7|6])Your Rank: (§.)(?<grade>[A-Z]).*".toPattern()
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

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.UNLOCKED_RECIPES)) {
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

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.FAIRY_ENIGMA_SOULS_QUESTS)) {
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
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.TRADES_UNLOCKED) && (itemName == "Trades")) {
            val lore = item.getLore()
            for (line in lore) {
                if (line.contains("Trades Unlocked")) {
                    return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                }
            }
        }
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.WARDROBE_SLOT) && (chestName.startsWith("Wardrobe") && (itemName.startsWith("Slot ") && itemName.contains(":")))) {
            (("(§.)?Slot (?<slotNumber>[0-9]): .*").toPattern()).matchMatcher(itemName) {
                return group("slotNumber")
            }
        }
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.ABBV_STATS) && chestName.startsWith("Your Stats Breakdown")) {
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

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.CUTE_NAME) && (chestName.lowercase() == ("skyblock menu"))) {
            val nameWithColor = item.name ?: return ""
            if (nameWithColor != "§aProfile Management") return ""
            val lore = item.getLore()
            for (line in lore) {
                if (line.startsWith("§7Playing on: §a")) {
                    return when (val profileName = line.removePrefix("§7Playing on: §a")) {
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

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.DOJO_PROGRESS) && (chestName.endsWith("Challenges") && (itemName.startsWith("Test of ") || itemName == ("Rank")))) {
            for (line in item.getLore()) {
                dojoTestOfGradePattern.matchMatcher(line) {
                    return group("grade")
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.BANK_UTILS) && chestName.contains("Bank")) {
            val lore = item.getLore()
            if (chestName.equals("Bank Withdrawal") && itemName.equals("§aWithdraw 20%")) {
                for (line in lore) {
                    if (line.startsWith("§7Amount to withdraw: ")) {
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
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.MAYOR_PERKS)) {
            val lore = item.getLore()
            (("Election, Year .*").toPattern()).matchMatcher(chestName) {
                if (item.getItem() == Item.getItemFromBlock(Blocks.glass_pane) || item.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)) return ""
                if (itemName.lowercase().contains("dante")) return "§c§l✖"
                val nameWithColor = item.name ?: return ""
                if (lore.isNotEmpty()) {
                    if (lore.first().equals("§8Candidate")) {
                        val colorCode = nameWithColor.take(2)
                        var numPerks = 0
                        for (line in lore) {
                            if (line.startsWith(colorCode) &&
                                line != "${colorCode}You voted for this candidate!" &&
                                line != "${colorCode}Leading in votes!" &&
                                !(line.startsWith("${colorCode}Click to vote for ")) &&
                                line != "${colorCode}This is a SPECIAL candidate!" &&
                                !(line.startsWith("$colorCode§"))) {
                                numPerks++
                            }
                        }
                        return "$colorCode$numPerks"
                    }
                }
            }
            if (((chestName == "Calendar and Events") || (chestName.contains("Mayor ")))) {
                val nameWithColor = item.name ?: return ""
                val colorCode = nameWithColor.take(2)
                (("JERRY IS MAYOR.*").toPattern()).matchMatcher(itemName) {
                    return grabPerkpocalypseMayor(lore, colorCode)
                }
                (("Mayor .*").toPattern()).matchMatcher(itemName) {
                    ((".*dante.*").toPattern()).matchMatcher(itemName.lowercase()) { return "§c§l✖" }
                    for (line in lore) {
                        if (line == ("§7players until the closing of") || line == ("§7until the closing of the next")) {
                            if (!(itemName.endsWith(" Jerry")) || chestName.startsWith("Mayor")) {
                                var numPerks = 0
                                for (line in lore) {
                                    if (line.startsWith(colorCode) && !(line.startsWith("$colorCode§"))) {
                                        numPerks++
                                    }
                                }
                                return "$colorCode$numPerks"
                            } else {
                                for (line in lore) {
                                    (("${colorCode}Perkpocalypse").toPattern()).matchMatcher(line) {
                                        return grabPerkpocalypseMayor(lore, colorCode)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.PlayerAdvanced.AUCTION_BAZAAR_VARIOUS)) {
            if (itemName.isEmpty()) return ""
            val lore = item.getLore()
            (("Bazaar.*").toPattern()).matchMatcher(chestName) {
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
                if (itemName == "Instasell Ignore List") {
                    for (line in lore) {
                        (("(§.)*Ignored: (§.)*(?<products>\\w+) Products").toPattern()).matchMatcher(line) {
                            return group("products")
                        }
                    }
                }
                if (itemName.endsWith(" Mode")) {
                    return itemName.take(3)
                }
            }
            ((".* ➜ .*").toPattern()).matchMatcher(chestName) {
                (("Instasell Ignore").toPattern()).matchMatcher(itemName) {
                    for (line in lore) {
                        (("(§.)*Bulk Instasell: (?<display>(§.)*A)llowed!").toPattern()).matchMatcher(line) {
                            return group("display")
                        }
                    }
                }
            }
            if ((chestName == "Auction View")) {
                if (itemName == "Bid History") {
                    (("(§.)*Total bids: (§.)*(?<bids>[\\S]+) .*").toPattern()).matchMatcher(lore.first()) {
                        return group("bids")
                    }
                }
            }
            ((".*Auction House").toPattern()).matchMatcher(chestName) {
                if (itemName != "View Bids" && itemName != "Manage Auctions") return ""
                if ((itemName == "View Bids")) {
                    ((".*top bid .*").toPattern()).matchMatcher(lore.first()) {
                        return "1"
                    }
                    (("(§.)?You placed (§.)?(?<bids>[0-9]+) bid(.{0,3}) (§.)?on pending").toPattern()).matchMatcher(lore.first()) {
                        return group("bids")
                    }
                }
                if ((itemName == "Manage Auctions")) {
                    for (line in lore) {
                        (("(§.)*Your (§.)*auction(s?) (§.)*ha(ve|s) (§.)*(?<bidcount>[\\S]+) (§.)*bid(s?)").toPattern()).matchMatcher(line) {
                            return group("bidcount")
                        }
                    }
                }
            }
            ((".*Auction").toPattern()).matchMatcher(chestName) {
                if (itemName != "Item Tier" && itemName != "BIN Filter" && itemName != "Sort") return ""
                for (line in lore) {
                    (("(§.)*▶ (?<text>[\\w ]+)").toPattern()).matchMatcher(line) {
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
                            (("((?<colorCode>§.)*▶ (?<singleChar>[\\w ]))([\\w ])+").toPattern()).matchMatcher(line) {
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

    private fun grabPerkpocalypseMayor(lore: List<String>, colorCode: String): String {
        if (lore.any { it == ("${colorCode}SLASHED Pricing") } || lore.any { it == ("${colorCode}Slayer XP Buff") } || lore.any { it == ("${colorCode}Pathfinder") }) return "§bAtx"
        if (lore.any { it == ("${colorCode}Prospection") } || lore.any { it == ("${colorCode}Mining XP Buff") } || lore.any { it == ("${colorCode}Mining Fiesta") }) return "§bCle"
        if (lore.any { it == ("${colorCode}Lucky!") } || lore.any { it == ("${colorCode}Pet XP Buff") } || lore.any { it == ("${colorCode}Mythological Ritual") }) return "§bDna"
        if (lore.any { it == ("${colorCode}Farming Simulator") } || lore.any { it == ("${colorCode}Pelt-pocalypse") } || lore.any { it == ("${colorCode}GOATed") }) return "§bFng"
        if (lore.any { it == ("${colorCode}Sweet Tooth") } || lore.any { it == ("${colorCode}Benevolence") } || lore.any { it == ("${colorCode}Extra Event") }) return "§bFxy"
        if (lore.any { it == ("${colorCode}Luck of the Sea 2.0") } || lore.any { it == ("${colorCode}Fishing XP Buff") } || lore.any { it == ("${colorCode}Fishing Festival") }) return "§bMrn"
        if (lore.any { it == ("${colorCode}Marauder") } || lore.any { it == ("${colorCode}EZPZ") } || lore.any { it == ("${colorCode}Benediction") }) return "§bPul"
        if (lore.any { it == ("${colorCode}Barrier Street") } || lore.any { it == ("${colorCode}Shopping Spree") }) return "§c§l✖" //diaz gets an automatic X.
        else return "§c?"
    }
}
