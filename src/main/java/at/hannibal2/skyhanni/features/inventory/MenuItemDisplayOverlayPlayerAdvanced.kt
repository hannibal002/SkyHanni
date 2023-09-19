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
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayerAdvanced {
    private val dojoTestOfGradePattern = "§7(§6)?Your Rank: (?<grade>§.[A-Z]).*".toPattern()
    private val recipeBookPattern = "..Recipe Book Unlocked: §.(?<recipe>[0-9]{1,2}(\.[0-9])?)§.%".toPattern()
    private val recipeMenuPattern = ".*Recipes Unlocked: §.(?<specific>[0-9]{1,2}(\.[0-9])?)§.%".toPattern()
    private val skyblockStatBreakdownPattern = "§(?<color>[0-9a-f])(?<icon>.) (?<name>.*) §f(?<useless>.+)".toPattern()
    private val hannibalInsistedOnThisList = listOf("Museum", "Rarities", "Armor Sets", "Weapons", "Special Items")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun lazilyGetPercent(original: String, thingToExtract: String): String {
        return original.removeColor().replace(thingToExtract, "").replace("100%", "a✔").take(2).replace(".","").replace("a✔", "§a✔")
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
        ` if (!(InventoryUtils.openInventoryName() == "Visitor's Logbook")) return "" `
        and you *should* be fine for the most part.
        ----------------------------------------------------------------------------
        */

        //NOTE: IT'S String.length, NOT String.length()!
        
        if (stackSizeConfig.contains(0)) {
            if (chestName.lowercase() == "skyblock menu") {
                if (itemName == "Recipe Book") {
                    for (line in item.getLore()) {
                        if (line.contains(" Book Unlocked: ")) {
                            return recipeBookPattern.matchMatcher(line) { group("recipe").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
            if (chestName == "Recipe Book") {
                if (itemName.contains(" Recipes")) {
                    for (line in item.getLore()) {
                        if (line.contains("Recipes Unlocked: ")) {
                            return recipeMenuPattern.matchMatcher(line) { group("specific").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(1)) {
            if (chestName == "Quest Log") {
                if (itemName == "Find all Fairy Souls") {
                    for (line in item.getLore()) {
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
                    for (line in item.getLore()) {
                        if (line.contains("§7Completed: §a")) {
                            return "§a" + line.removeColor().replace("Completed: ", "")
                        }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(2)) {
            if (itemName == "Trades") {
                val lore = item.getLore()
                if (lore.any { it.removeColor().contains("lock Menu") }) {
                    for (line in lore) {
                        if (line.removeColor().contains("Trades Unlocked")) {
                            return lazilyGetPercent(line, "Trades Unlocked: ")
                        }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(3)) {
            if (chestName.startsWith("Wardrobe")) {
                if (itemName.startsWith("Slot ") && itemName.contains(":")) {
                    return itemName.replace("Slot ", "").substring(0,2).trim().replace(":", "")
                }
            }
        }

        
        if (stackSizeConfig.contains(4)) {
            if (chestName.startsWith("Your Stats Breakdown")) {
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
        }

        if (stackSizeConfig.contains(5)) {
            if (chestName.lowercase() == "skyblock menu") {
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
        }

        if (stackSizeConfig.contains(6)) {
            if (chestName.endsWith("Challenges")) {
                if (itemName.startsWith("Test of ") || itemName == "Rank") {
                    for (line in item.getLore()) {
                        if (line.contains("Your Rank:")) {
                            dojoTestOfGradePattern.matchMatcher(line) {
                                return group("grade").removeColor()
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(7)) {
            if (chestName == "Bank") {
                if (itemName == "Bank Upgrades") {
                    for (line in item.getLore()) {
                        if (line.startsWith("§7Current account: ")) {
                            return line.removeColor().replace("Current account: ", "").substring(0,1)
                        }
                    }
                }
            }
        }
        
        if (stackSizeConfig.contains(8)) {
            if ((chestName == "Election") && (itemName != (""))) {
                if (itemName.lowercase().contains("dante")) return "§c§l✖"
                val nameWithColor = item.name ?: return ""
                val lore = item.getLore()
                if (lore.any { it.contains("Candidate") }) {
                    val colorCode = nameWithColor.take(2)
                    var numPerks = 0
                    for (line in item.getLore()) {
                        if (line.startsWith(colorCode) &&
                            !(line.contains("You voted for this candidate!")) &&
                            !(line.contains("Leading in votes!")) &&
                            !(line.contains("Click to vote for ")) && 
                            !(line.startsWith(colorCode + "§"))) {
                                numPerks++
                        }
                    }
                    return "" + colorCode + numPerks
                }
            }
            if ((chestName == "Calendar and Events") && (itemName.contains("Mayor "))) {
                if (itemName.lowercase().contains("dante")) return "§c§l✖"
                val nameWithColor = item.name ?: return ""
                val lore = item.getLore()
                if (lore.any { it.contains(" mayor ") }) {
                    val colorCode = nameWithColor.take(2)
                    var numPerks = 0
                    for (line in item.getLore()) {
                        if (line.startsWith(colorCode) && !(line.startsWith(colorCode + "§"))) {
                            numPerks++
                        }
                    }
                    return "" + colorCode + numPerks
                }
            }
        }
        
        return ""
    }
}