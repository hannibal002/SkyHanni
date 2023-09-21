package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayer {
    private val museumDonationPattern = "§7Items Donated: §.(?<amount>[0-9.]+).*".toPattern()
    private val skyblockLevelPattern = "§7Your SkyBlock Level: §.?\\[§.?(?<sblvl>[0-9]{0,3})§.?].*".toPattern()
    private val skillAvgPattern = "§[0-9](?<avg>[0-9]{1,2}(\.[0-9])?) Skill Avg\..*".toPattern()
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\.[0-9]*)?(§.)?%".toPattern()
    private val dungeonClassLevelPattern = "(?<class>[A-z ]+)( )(?<level>[0-9]+)".toPattern()
    private val profileManagementPattern = "(?<icon>.)? (?<type>.+)?(?<profile> Profile: )(?<fruit>.+)".toPattern() // FOR THIS EXPRESSION SPECIFICALLY, FORMATTING CODES ***MUST*** BE REMOVED FIRST, OTHERWISE THIS REGEX WONT WORK!!! -ERY
    val hannibalInsistedOnThisList = listOf("Museum", "Rarities", "Armor Sets", "Weapons", "Special Items")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.menuItemNumberPlayerAsStackSize.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.menuItemNumberPlayerAsStackSize
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
            if (chestName.lowercase() == "skyblock menu") {
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

        if (stackSizeConfig.contains(1)) {
            if (chestName == "Your Skills" || chestName == "Dungeoneering") {
                if (item.getLore().any { it.contains("Click to view!") }) {
                    if (chestName == "Your Skills") {
                        if (CollectionAPI.isCollectionTier0(item.getLore())) return "0"
                        if (!itemName.contains("Dungeon")) {
                            val text = itemName.split(" ").last()
                            return "" + text.romanToDecimalIfNeeded()
                        }
                    } else if (chestName == "Dungeoneering") {
                        val noColorName = itemName.removeColor()
                        dungeonClassLevelPattern.matchMatcher(noColorName) {
                            return group("level")
                        }
                    }
                }
            } else if ((chestName == "Farming Skill") && itemName.contains("Garden Level ")) {
                if (GardenAPI.getGardenLevel() != 0) return GardenAPI.getGardenLevel().toString()
                return itemName.replace("Garden Level ", "")
            }
        }

        if (stackSizeConfig.contains(2)) {
            if (chestName.lowercase() == "skyblock menu") {
                if (itemName == "Your Skills") {
                    for (line in item.getLore()) {
                        if (line.contains(" Skill Avg. ")) {
                            return skillAvgPattern.matchMatcher(line) { group("avg").toDouble().toInt().toString() } ?: ""
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(3)) {
            if (chestName.endsWith(" Collections")) {
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
            if (chestName.lowercase() == "skyblock menu") {
                if (itemName == "Collections") {
                    for (line in item.getLore()) {
                        if (line.contains("Collections Unlocked: ")) {
                            return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
            if (chestName.contains("Collections")) {
                if (itemName.contains("Collections")) {
                    for (line in item.getLore()) {
                        if (line.contains("Collections ") && line.contains(": §")) {
                            return genericPercentPattern.matchMatcher(line) { group("percent").replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(4)) {
            if (chestName.startsWith("Crafted Minions")) {
                val lore = item.getLore()
                if (itemName == "Information") {
                    for (line in lore) {
                        if (line.contains("Craft ") && line.contains(" more")) {
                            return line.removeColor().replace("Craft ", "").replace(" more unique", "").trim()
                        }
                    }
                }
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

        if (stackSizeConfig.contains(5)) {
            if (chestName.endsWith("Your Museum")) {
                if (hannibalInsistedOnThisList.contains(itemName)) {
                    for (line in item.getLore()) {
                        if (line.contains("Items Donated")) {
                            return museumDonationPattern.matchMatcher(line) { group("amount").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(6)) {
            if (chestName == "Profile Management") {
                if (itemName.contains("Profile: ")) {
                    profileManagementPattern.matchMatcher(itemName) { return group("icon") } ?: return "©"
                }
            }
        }

        if (stackSizeConfig.contains(7)) {
            if (chestName.contains("Pets")) {
                if (itemName.contains("Pet Score Rewards") && !(item.getLore().isEmpty())) {
                    return item.getLore().last().removeColor().split(" ").last()
                }
            }
        }

        return ""
    }
}