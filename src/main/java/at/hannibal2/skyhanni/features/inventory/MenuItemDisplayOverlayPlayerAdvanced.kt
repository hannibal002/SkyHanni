package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerAdvanced {
    private val dojoTestOfGradePattern = "§7(§6)?Your Rank: (?<grade>§.[A-Z]).*".toPattern()
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\.[0-9]*)?(§.)?%".toPattern()
    private val skyblockStatBreakdownPattern = "§(?<color>[0-9a-f])(?<icon>.) (?<name>.*) §f(?<useless>.+)".toPattern()
    private val enigmaSoulsPattern = "(§.)?Enigma Souls: (§.)?(?<useful>[0-9]+)(§.)?\/(§.)?.*".toPattern()

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun lazilyGetPercent(original: String, thingToExtract: String = ""): String {
        return original.removeColor().replace(thingToExtract, "").replace("100%", "a✔").take(2).replace(".", "").replace("a✔", "§a✔").replace("%", "")
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.menuItemNumberPlayerAdvancedAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.menuItemNumberPlayerAdvancedAsStackSize
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

        if (stackSizeConfig.contains(1)) {
            val lore = item.getLore()
            if (chestName == "Quest Log") {
                if (itemName == "Find all Fairy Souls") {
                    for (line in lore) {
                        val newLine = line.removeColor()
                        val totalFairySouls = "242" //change this whenever hypixel adds more fairy souls
                        // §a✔ §eFound: §d242§7/§d242 (TY COBBLE8 FOR THIS SAMPLE)
                        // ✔ Found: 242/242
                        if (newLine.contains("Found: ")) {
                            return "§d" + newLine.removeColor().replace(" ✖ Found: ", "").replace(" ✔ Found: ", "").replace(("/" + totalFairySouls), "").replace(totalFairySouls, "§a${totalFairySouls}")
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
                if (!(itemName.isEmpty()) && !(lore.isEmpty())) {
                    if (lore.any { it.contains("Enigma Souls: ") }) {
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
        
        if (stackSizeConfig.contains(2) && (itemName == "Trades")) {
            val lore = item.getLore()
            for (line in lore) {
                if (line.contains("Trades Unlocked")) {
                    return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                }
            }
        }
        
        if (stackSizeConfig.contains(3) && (chestName.startsWith("Wardrobe") && (itemName.startsWith("Slot ") && itemName.contains(":")))) {
            return itemName.replace("Slot ", "").substring(0,2).trim().replace(":", "")
        }
        
        if (stackSizeConfig.contains(4) && chestName.startsWith("Your Stats Breakdown")) {
            val statName = item.name ?: return ""
            if (!(statName.isEmpty())) {
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

        if (stackSizeConfig.contains(5) && (chestName.lowercase() == ("skyblock menu"))) {
            val nameWithColor = item.name ?: return ""
            if (nameWithColor != "§aProfile Management") return ""
            val lore = item.getLore()
            for (line in lore) {
                if (line.contains("§7Playing on: §a")) {
                    val profileName = line.replace("§7Playing on: §a", "").removeColor().trim()
                    return when (profileName) {
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

        if (stackSizeConfig.contains(6) && (chestName.endsWith("Challenges") && (itemName.startsWith("Test of ") || itemName == ("Rank")))) {
            for (line in item.getLore()) {
                if (line.contains("Your Rank:")) {
                    dojoTestOfGradePattern.matchMatcher(line) {
                        return group("grade").removeColor()
                    }
                }
            }
        }

        if (stackSizeConfig.contains(7) && (chestName == "Bank") && (itemName == "Bank Upgrades")) {
            for (line in item.getLore()) {
                if (line.startsWith("§7Current account: ")) {
                    return line.removeColor().replace("Current account: ", "").substring(0,1)
                }
            }
        }
        
        if (stackSizeConfig.contains(8)) {
            val lore = item.getLore()
            if ((chestName.contains("Election"))) {
                if (itemName.lowercase().contains("dante")) return "§c§l✖"
                val nameWithColor = item.name ?: return ""
                if (item.getItem() == Item.getItemFromBlock(Blocks.glass_pane) || item.getItem() == Item.getItemFromBlock(Blocks.stained_glass_pane)) return ""
                if (!(lore.isEmpty())) {
                    if (lore.any { it.contains("Candidate") }) {
                        val colorCode = nameWithColor.take(2)
                        var numPerks = 0
                        for (line in item.getLore()) {
                            if (line.startsWith(colorCode) &&
                                !(line.contains("You voted for this candidate!")) &&
                                !(line.contains("Leading in votes!")) &&
                                !(line.contains("Click to vote for ")) && 
                                !(line.contains("SPECIAL ")) && 
                                !(line.startsWith(colorCode + "§"))) {
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
                    if (lore.any { it.contains(" the closing of") }) {
                        if (!(itemName.contains("Jerry")) || chestName.contains("Mayor")) {
                            val colorCode = nameWithColor.take(2)
                            var numPerks = 0
                            for (line in lore) {
                                if (line.startsWith(colorCode) && !(line.startsWith(colorCode + "§"))) {
                                    numPerks++
                                }
                            }
                            return "" + colorCode + numPerks
                        } else {
                            for (line in lore) {
                                if (lore. any { it.contains("Perkpocalypse") }) {
                                    return grabPerkpocalypseMayor(lore)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(9)) {
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
                if (itemName == "Instasell Ignore List" && lore.any { it.contains("Ignored: ") }) {
                    for (line in lore) {
                        if (line.contains("Ignored: ")) { //§7Ignored: §c47 Products --> Ignored: 47 Products --> Ignored: 47 Product --> 47
                            return line.removeColor().replace("Products", "Product").between("Ignored: ", " Product")
                        }
                    }
                }
                if (itemName.endsWith(" Mode")) {
                    return itemName.take(3)
                }
            }
            if (chestName.contains(" ➜ ") && itemName.contains("Instasell Ignore") && lore.any { it.contains("Bulk Instasell: ") }) {
                for (line in lore) {
                    if (line.contains("Bulk Instasell: ")) {
                        //§cIgnored!
                        //§aAllowed!
                        return line.replace("Bulk Instasell: ", "").take(5)
                    }
                }
            }
            if ((chestName == "Auction View")) {
                if (!(itemName == "Bid History")) return ""
                if (!(lore.first().contains("Total "))) return ""
                return lore.first().removeColor().replace("Total bids: ", "").replace(" bids", "").replace(" bid", "")
            }
            if ((chestName.contains("Auction House"))) {
                if (!(itemName == "View Bids") && !(itemName == "Manage Auctions")) return ""
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
                if (!(itemName == "Item Tier") && !(itemName == "BIN Filter") && !(itemName == "Sort")) return ""
                for (line in lore) {
                    if (line.contains("▶ ")) {
                        val betterLine = line.removeColor().replace("▶ ", "")
                        if (itemName == "Sort") {
                            return when (betterLine) {
                                betterLine.contains("Highest ") -> "§c⬆"
                                betterLine.contains("Lowest") -> "§a⬇"
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
        if (lore.any { it.contains("SLASHED Pricing") } || lore.any { it.contains("Slayer XP Buff") } || lore.any { it.contains("Pathfinder") }) return "§bAtx"
        if (lore.any { it.contains("Prospection") } || lore.any { it.contains("Mining XP Buff") } || lore.any { it.contains("Mining Fiesta") }) return "§bCle"
        if (lore.any { it.contains("Lucky!") } || lore.any { it.contains("Pet XP Buff") } || lore.any { it.contains("Mythological Ritual") }) return "§bDna"
        if (lore.any { it.contains("Barrier Street") } || lore.any { it.contains("Shopping Spree") }) return "§c§l✖" //fuck diaz, all of my homies hate diaz. diaz is a rather common hypixel admin L and she must be removed from the game like candidate barry was.
        if (lore.any { it.contains("Farming Simulator") } || lore.any { it.contains("Pelt-pocalypse") } || lore.any { it.contains("GOATed") }) return "§bFng"
        if (lore.any { it.contains("Sweet Tooth") } || lore.any { it.contains("Benevolence") } || lore.any { it.contains("Extra Event") }) return "§bFxy"
        if (lore.any { it.contains("Luck of the Sea 2.0") } || lore.any { it.contains("Fishing XP Buff") } || lore.any { it.contains("Fishing Festival") }) return "§bMrn"
        if (lore.any { it.contains("Marauder") } || lore.any { it.contains("EZPZ") } || lore.any { it.contains("Benediction") }) return "§bPul"
        else return "§c?"
    }
}