package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.config.features.InventoryConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayer {
    private val museumDonationPattern = "§7Items Donated: §.(?<amount>[0-9.]+).*".toPattern()
    private val skyblockLevelPattern = "§7Your SkyBlock Level: §.?\\[§.?(?<sblvl>[0-9]{0,3})§.?].*".toPattern()
    private val skillAvgPattern = "§[0-9](?<avg>[0-9]{1,2}(\\.[0-9])?) Skill Avg\\..*".toPattern()
    private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val dungeonClassLevelPattern = "(?<class>[A-z ]+)( )(?<level>[0-9]+)".toPattern()
    private val dungeonEssenceRewardPattern = "(§.)?(?<type>[A-z]+) (Essence) (§.)?x(?<amount>[0-9]+)".toPattern()
    private val essenceCountPattern = "(§.)?Your (?<essencetype>.+) Essence: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)".toPattern()
    private val profileManagementPattern = "(?<icon>.)? (?<type>.+)?(?<profile> Profile: )(?<fruit>.+)".toPattern() // FOR THIS EXPRESSION SPECIFICALLY, FORMATTING CODES ***MUST*** BE REMOVED FIRST, OTHERWISE THIS REGEX WONT WORK!!! -ERY
    private val hannibalInsistedOnThisList = listOf("Museum", "Rarities", "Armor Sets", "Weapons", "Special Items")

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.player.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.player
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.SKYBLOCK_LEVEL) && chestName.lowercase() == ("skyblock menu") && itemName.endsWith(" Leveling")) {
            for (line in item.getLore()) {
                if (line.contains(" Level: ")) {
                    skyblockLevelPattern.matchMatcher(line) {
                        return group("sblvl").toInt().toString()
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.SKILL_GARDEN_DUNGEON_LEVELS)) {
            if (chestName == "Your Skills" || chestName == "Dungeoneering") {
                if (item.getLore().isNotEmpty() && item.getLore().last().equals("§eClick to view!")) {
                    if (chestName == "Your Skills") {
                        if (CollectionAPI.isCollectionTier0(item.getLore()) && !(itemName.contains("Dungeoneering"))) return "0"
                        if (itemName.removeColor().split(" ").size < 2) return "" //thanks to watchdogshelper we had to add this hotfix line
                        if (!itemName.contains("Dungeon")) {
                            val text = itemName.removeColor().split(" ").last()
                            return "" + text.romanToDecimalIfNeeded()
                        }
                    } else if (chestName == "Dungeoneering") {
                        dungeonClassLevelPattern.matchMatcher(itemName.removeColor()) {
                            return group("level")
                        }
                    }
                }
            } else if (((chestName == "Farming Skill") || (chestName == "Desk")) && itemName.contains("Garden Level ")) {
                if (GardenAPI.getGardenLevel() != 0) return GardenAPI.getGardenLevel().toString()
                return itemName.replace("Garden Level ", "")
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.SKILL_AVERAGE) && (chestName.lowercase() == ("skyblock menu") && (itemName == ("Your Skills")))) {
            for (line in item.getLore()) {
                if (line.contains(" Skill Avg. ")) {
                    return skillAvgPattern.matchMatcher(line) { group("avg").toDouble().toInt().toString() } ?: ""
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.COLLECTION_LEVELS_AND_PROGRESS)) {
            if (chestName.endsWith(" Collections")) {
                val lore = item.getLore()
                if (lore.last().equals("§eClick to view!")) {
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

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.CRAFTED_MINIONS) && chestName.startsWith("Crafted Minions")) {
            val lore = item.getLore()
            if (itemName == "Information") {
                for (line in lore) {
                    if (line.contains("Craft ") && line.contains("more") && line.contains("unique")) {
                        //§7Craft §b22 §7more §aunique
                        //Craft 22 more unique
                        return line.removeColor().trim().split("Craft ", " more unique")[1]
                    }
                }
            }
            if (lore.last().startsWith("§eClick to view ")) {
                var tiersToSubtract = 0
                var totalTiers = 0
                for (line in lore) {
                    if (line.contains(" Tier ")) { totalTiers++ }
                    if (line.contains(" Tier ") && line.contains("§c")) { tiersToSubtract++ }
                }
                return (totalTiers - tiersToSubtract).toString().replace(totalTiers.toString(), "§a✔")
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.MUSEUM_PROGRESS) && chestName.endsWith("Your Museum") && hannibalInsistedOnThisList.contains(itemName)) {
            for (line in item.getLore()) {
                if (line.contains("Items Donated")) {
                    return museumDonationPattern.matchMatcher(line) { group("amount").toDouble().toInt().toString().replace("100", "§a✔") } ?: ""
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.PROFILE_ICON) && chestName == ("Profile Management") && itemName.contains("Profile: ")) {
            profileManagementPattern.matchMatcher(itemName) { return group("icon") } ?: return "©"
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.PET_SCORE_STATUS)) {
            if ((chestName.lowercase() == "skyblock menu") && itemName.contains("Pets")) {
                for (line in item.getLore()) {
                    if ((line.contains("Selected pet: ")) && (line.contains("None"))) return "§c§l✖"
                }
            }
            if (chestName.contains("Pets")) {
                if (itemName.contains("Pet Score Rewards") && item.getLore().isNotEmpty()) {
                    return item.getLore().last().removeColor().split(" ").last()
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.ESSENCE_COUNTS)) {
            if (LorenzUtils.isRewardChest()) {
                dungeonEssenceRewardPattern.matchMatcher(itemName) { return group("amount") } ?: return ""
            }
            if (!(chestName.contains(" ➜ ")) && (chestName.contains("Essence Shop") && itemName.contains("Essence Shop")) || (chestName.contains("Essence Guide") && itemName.endsWith(" Essence")) || (chestName.endsWith(" Essence"))) {
                val lore = item.getLore()
                for (line in lore) {
                    if (line.contains("Your ") && line.contains(" Essence: ")) {
                        essenceCountPattern.matchMatcher(line) {
                            val usefulAsString = group("useful")
                            val totalAsString = group("total").replace(",", "")
                            val suffix = when (totalAsString.length) {
                                in 1..3 -> ""
                                in 4..6 -> "k"
                                in 7..9 -> "M"
                                in 10..12 -> "B"
                                in 13..15 -> "T"
                                else -> "§b§z:)"
                            }
                            if (suffix == "§b§z:)") return suffix
                            else return "" + usefulAsString + suffix
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.MenuConfig.PlayerGeneral.MINION_QUICK_UPGRADE) && (chestName.contains(" Minion ")) && itemName.contains("Quick") && itemName.contains("Upgrade Minion")) {
            //one day admins are going to remove that damn hyphen in "Quick-Upgrade" and it's going to break this feature
            val lore = item.getLore()
            for (line in lore) {
                if (line.contains("You need ") && line.contains("more")) {
                    return line.removeColor().split("You need ", " more")[1]
                }
            }
        }

        return ""
    }
}
