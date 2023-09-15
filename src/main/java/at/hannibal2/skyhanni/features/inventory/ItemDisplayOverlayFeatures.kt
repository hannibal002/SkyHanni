package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName_old
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.between
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPrehistoricEggBlocksWalked
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ItemDisplayOverlayFeatures {
    private val rancherBootsSpeedCapPattern = "§7Current Speed Cap: §a(?<cap>.*)".toPattern()
    private val petLevelPattern = "\\[Lvl (?<level>.*)] .*".toPattern()
    private val museumDonationPattern = "§7Items Donated: §.(?<amount>[0-9.]+).*".toPattern()
    private val dojoTestOfGradePattern = "§7(§6)?Your Rank: (?<grade>§.[A-Z]).*".toPattern()
    private val skyblockLevelPattern = "§7Your SkyBlock Level: §.?\\[§.?(?<sblvl>[0-9]{0,3})§.?].*".toPattern()
    private val skillAvgPattern = "§[0-9](?<avg>[0-9]{1,2}(\.[0-9])?) Skill Avg\..*".toPattern()
    private val collUnlockPattern = "..Collections Unlocked: §.(?<coll>[0-9]{1,2}(\.[0-9])?)§.%".toPattern()
    private val collMenuUnlockPattern = ".*Collections .*: §.(?<collMenu>[0-9]{1,2}(\.[0-9])?)§.%".toPattern()
    private val recipeBookPattern = "..Recipe Book Unlocked: §.(?<recipe>[0-9]{1,2}(\.[0-9])?)§.%".toPattern()
    private val recipeMenuPattern = ".*Recipes Unlocked: §.(?<specific>[0-9]{1,2}(\.[0-9])?)§.%".toPattern()
    private val skyblockStatBreakdownPattern = "§(?<color>[0-9a-f])(?<icon>.) (?<name>.*) §f(?<useless>.+)".toPattern()
    private val profileManagementPattern = "(?<icon>.)? (?<type>.+)?(?<profile> Profile: )(?<fruit>.+)".toPattern() // FOR THIS EXPRESSION SPECIFICALLY, FORMATTING CODES ***MUST*** BE REMOVED FIRST, OTHERWISE THIS REGEX WONT WORK!!! -ERY
    private val hannibalInsistedOnThisList = listOf("Museum", "Rarities", "Armor Sets", "Weapons", "Special Items")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        val itemName = item.cleanName()

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(0)) {
            when (itemName) {
                "First Master Star" -> return "1"
                "Second Master Star" -> return "2"
                "Third Master Star" -> return "3"
                "Fourth Master Star" -> return "4"
                "Fifth Master Star" -> return "5"
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(1)) {
            if (itemName.matchRegex("(.*)Master Skull - Tier .")) {
                return itemName.substring(itemName.length - 1)
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(2)) {
            if (itemName.contains("Golden ") || itemName.contains("Diamond ")) {
                when {
                    itemName.contains("Bonzo") -> return "1"
                    itemName.contains("Scarf") -> return "2"
                    itemName.contains("Professor") -> return "3"
                    itemName.contains("Thorn") -> return "4"
                    itemName.contains("Livid") -> return "5"
                    itemName.contains("Sadan") -> return "6"
                    itemName.contains("Necron") -> return "7"
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(3)) {
            if (itemName.startsWith("New Year Cake (")) {
                return "§b" + itemName.between("(Year ", ")")
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(4)) {
            val chestName = InventoryUtils.openInventoryName()
            if (!chestName.endsWith("Sea Creature Guide")) {
                if (ItemUtils.isPet(itemName)) {
                    petLevelPattern.matchMatcher(itemName) {
                        val level = group("level").toInt()
                        if (level != ItemUtils.maxPetLevel(itemName)) {
                            return "$level"
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(5)) {
            if (itemName.contains(" Minion ")) {
                if (item.getLore().any { it.contains("Place this minion") }) {
                    val array = itemName.split(" ")
                    val last = array[array.size - 1]
                    return last.romanToDecimal().toString()
                }
            }
        }

        if (SkyHanniMod.feature.inventory.displaySackName) {
            if (ItemUtils.isSack(itemName)) {
                val sackName = grabSackName(itemName)
                return (if (itemName.contains("Enchanted")) "§5" else "") + sackName.substring(0, 2)
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(7)) {
            if (InventoryUtils.openInventoryName().toLowerCase() == "skyblock menu") {
                if (itemName.endsWith(" Leveling")) {
                    for (line in item.getLore()) {
                        if (line.contains(" Level: ")) {
                            skyblockLevelPattern.matchMatcher(line) {
                                return group("sblvl").toInt().toString()
                            }
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(8)) {
            if (itemName.contains("Kuudra Key")) {
                return when (itemName) {
                    "Kuudra Key" -> "§a1"
                    "Hot Kuudra Key" -> "§22"
                    "Burning Kuudra Key" -> "§e3"
                    "Fiery Kuudra Key" -> "§64"
                    "Infernal Kuudra Key" -> "§c5"
                    else -> "§4?"
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(9)) {
            if (InventoryUtils.openInventoryName() == "Your Skills") {
                if (item.getLore().any { it.contains("Click to view!") }) {
                    if (CollectionAPI.isCollectionTier0(item.getLore())) return "0"
                    if (!itemName.contains("Dungeon")) {
                        val text = itemName.split(" ").last()
                        return "" + text.romanToDecimalIfNeeded()
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(10)) {
            if (InventoryUtils.openInventoryName().endsWith(" Collections")) {
                val lore = item.getLore()
                if (lore.any { it.contains("Click to view!") }) {
                    if (CollectionAPI.isCollectionTier0(lore)) return "0"
                    item.name?.let {
                        if (it.startsWith("§e")) {
                            val text = it.split(" ").last()
                            return "" + text.romanToDecimalIfNeeded()
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(11)) {
            if (itemName.contains("Rancher's Boots")) {
                for (line in item.getLore()) {
                    rancherBootsSpeedCapPattern.matchMatcher(line) {
                        return group("cap")
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(12)) {
            if (item.getInternalName_old() == "LARVA_HOOK") {
                for (line in item.getLore()) {
                    "§7§7You may harvest §6(?<amount>.).*".toPattern().matchMatcher(line) {
                        val amount = group("amount").toInt()
                        return when {
                            amount > 4 -> "§a$amount"
                            amount > 2 -> "§e$amount"
                            else -> "§c$amount"
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(13)) {
            if (item.getInternalName_old() == "POTION") {
                item.name?.let {
                    "Dungeon (?<level>.*) Potion".toPattern().matchMatcher(it.removeColor()) {
                        return when (val level = group("level").romanToDecimal()) {
                            in 1..2 -> "§f$level"
                            in 3..4 -> "§a$level"
                            in 5..6 -> "§9$level"
                            else -> "§5$level"
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(14)) {
            if (InventoryUtils.openInventoryName().endsWith("Your Museum")) {
                if (hannibalInsistedOnThisList.contains(itemName)) {
                    for (line in item.getLore()) {
                        if (line.contains("Items Donated")) {
                            return museumDonationPattern.matchMatcher(line) { group("amount").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(15)) {
            if (InventoryUtils.openInventoryName().endsWith("Challenges")) {
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
        
        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(16)) {
            if (InventoryUtils.openInventoryName().toLowerCase() == "skyblock menu") {
                if (itemName == "Collections") {
                    for (line in item.getLore()) {
                        if (line.contains("Collections Unlocked: ")) {
                            return collUnlockPattern.matchMatcher(line) { group("coll").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
            if (InventoryUtils.openInventoryName() == "Collections") {
                if (itemName.contains(" Collections")) {
                    for (line in item.getLore()) {
                        if (line.contains("Collections ") && line.contains(": §")) {
                            return collMenuUnlockPattern.matchMatcher(line) { group("collMenu").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
        }
        
        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(17)) {
            if (InventoryUtils.openInventoryName().toLowerCase() == "skyblock menu") {
                if (itemName == "Recipe Book") {
                    for (line in item.getLore()) {
                        if (line.contains(" Book Unlocked: ")) {
                            return recipeBookPattern.matchMatcher(line) { group("recipe").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
            if (InventoryUtils.openInventoryName() == "Recipe Book") {
                if (itemName.contains(" Recipes")) {
                    for (line in item.getLore()) {
                        if (line.contains("Recipes Unlocked: ")) {
                            return recipeMenuPattern.matchMatcher(line) { group("specific").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(18)) {
            if (InventoryUtils.openInventoryName().toLowerCase() == "skyblock menu") {
                if (itemName == "Your Skills") {
                    for (line in item.getLore()) {
                        if (line.removeColor().contains(" Skill Avg. ")) {
                            return skillAvgPattern.matchMatcher(line) { group("avg").toDouble().toInt().toString() } ?: ""
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(19)) {
            if (InventoryUtils.openInventoryName().startsWith("Wardrobe")) {
                if (itemName.startsWith("Slot ") && itemName.contains(":")) {
                    return itemName.replace("Slot ", "").substring(0,2).trim().replace(":", "")
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(20)) {
            if (InventoryUtils.openInventoryName() == "Bank") {
                if (itemName == "Bank Upgrades") {
                    for (line in item.getLore()) {
                        if (line.startsWith("§7Current account: ")) {
                            return line.removeColor().replace("Current account: ", "").substring(0,1)
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(21)) {
            if (InventoryUtils.openInventoryName().startsWith("Crafted Minions")) {
                val lore = item.getLore()
                if (lore.any { it.contains("Click to view ") }) {
                    var tiersToSubtract = 0
                    var totalTiers = 0
                    for (line in lore) {
                        if (line.contains(" Tier ")) { totalTiers++ }
                        if (line.contains(" Tier ") && line.contains("§c")) { tiersToSubtract++ }
                    }
                    return (totalTiers - tiersToSubtract).toString().replace(totalTiers.toString(), "§a✔")
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(22)) {
            if (InventoryUtils.openInventoryName().startsWith("Crafted Minions")) {
                if (itemName == "Information") {
                    for (line in item.getLore()) {
                        if (line.removeColor().contains("Craft ") && line.removeColor().contains(" more unique")) {
                            return line.removeColor().replace("Craft ", "").replace(" more unique", "").trim()
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(23)) {
            if (InventoryUtils.openInventoryName().startsWith("Your Stats Breakdown")) {
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

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(24)) {
            if (InventoryUtils.openInventoryName() == "Quest Log") {
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
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(25)) {
            if (InventoryUtils.openInventoryName() == "Quest Log") {
                if (itemName == "Completed Quests") {
                    for (line in item.getLore()) {
                        if (line.contains("§7Completed: §a")) {
                            return "§a" + line.removeColor().replace("Completed: ", "")
                        }
                    }
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(26)) {
            if (itemName == "Prehistoric Egg") {
                val lore = item.getLore()
                if (lore.lastOrNull() == null) return ""
                val rarity = lore.last().removeColor().trim()
                val blocksWalked = item.getPrehistoricEggBlocksWalked() ?: return ""
                val threshold = when (rarity) {
                    "COMMMON" -> 4000
                    "UNCOMMON" -> 10000
                    "RARE" -> 20000
                    "EPIC" -> 40000
                    "LEGENDARY" -> 100000
                    else -> 1
                }
                if (threshold != 1) { return (((blocksWalked.toFloat()) / (threshold.toFloat())) * 100).toInt().toString() }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(27)) {
            if (InventoryUtils.openInventoryName() == "Heart of the Mountain") {
                val nameWithColor = item.name ?: return ""
                if ((nameWithColor.startsWith("§a")) || (nameWithColor.startsWith("§e")) || (nameWithColor.startsWith("§c"))) {
                    val lore = item.getLore()
                    if ((lore.firstOrNull() == null) || (lore.lastOrNull() == null)) return ""
                    if (!lore.first().contains("Level ") && !lore.last().contains("Right click to ")) return ""
                    if (lore.last().contains("the Mountain!") || lore.last().contains("Requires ")) return ""
                    var level = lore.first().removeColor().replace("Level ", "")
                    var colorCode = ""
                    if (level.contains("/")) level = level.split("/")[0]
                    if (nameWithColor.startsWith("§a")) level = "✔"
                    if (lore.last().removeColor().replace("Right click to ","").contains("enable")) colorCode = "§c"
                    return "" + colorCode + level
                }
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(28)) {
            if (InventoryUtils.openInventoryName() == "Heart of the Mountain") {
                val nameWithColor = item.name ?: return ""
                if (nameWithColor != "§5Crystal Hollows Crystals") return ""
                val lore = item.getLore()
                var crystalsPlaced = 0
                var crystalsNotPlaced = 0
                var crystalsNotFound = 0
                val totalCrystals = 5 //change "5" to whatever new value Hypixel does if this value ever changes
                for (line in lore) {
                    if (line.contains(" §e✖ Not Placed")) crystalsNotPlaced++
                    else if (line.contains(" §c✖ Not Found")) crystalsNotFound++
                }
                crystalsPlaced = totalCrystals - crystalsNotPlaced - crystalsNotFound
                return "§a${crystalsPlaced}§r|§e${crystalsNotPlaced}§r|§c${crystalsNotFound}"
            }
        }
        
        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(29)) {
            if (InventoryUtils.openInventoryName().toLowerCase() == "skyblock menu") {
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

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(30)) {
            if (InventoryUtils.openInventoryName() == "Profile Management") {
                if (!itemName.contains("Profile: ")) return ""
                profileManagementPattern.matchMatcher(itemName) { return group("icon") } ?: return "©"
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(31)) {
            if (!(InventoryUtils.openInventoryName() == "Visitor's Logbook")) return ""
            var timesVisited = 0
            var timesAccepted = 0
            if (item.getLore().any { it.contains("Times Visited:") }) {
                for (line in item.getLore()) {
                    if (line.contains("Times Visited:")) {
                        timesVisited = line.removeColor().replace("Times Visited:", "").trim().toInt()
                    } else if (line.contains("Offers Accepted:")) {
                        timesAccepted = line.removeColor().replace("Offers Accepted:", "").trim().toInt()
                    }
                }
                if (timesVisited == 0) return ""
                return ((((timesAccepted / timesVisited) * 100).toInt()) / 100).toString()
            }
        }

        if (SkyHanniMod.feature.inventory.itemNumberAsStackSize.contains(32)) {
            val nameWithColor = item.name ?: return "FUCK"
            if (nameWithColor.contains("Garden Level ")) return itemName.replace("Garden Level ", "")
        }

        return ""
    }

    var done = false

    private fun grabSackName(name: String): String {
        val split = name.split(" ")
        val text = split[0]
        for (line in arrayOf("Large", "Medium", "Small", "Enchanted")) {
            if (text == line) return grabSackName(name.substring(text.length + 1))
        }
        return text
    }
}