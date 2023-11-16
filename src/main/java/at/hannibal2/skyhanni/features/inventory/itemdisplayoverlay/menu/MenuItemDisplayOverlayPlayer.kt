package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
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

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKYBLOCK_LEVEL) && chestName.lowercase() == ("skyblock menu") && itemName.endsWith(" Leveling")) {
            for (line in item.getLore()) {
                skyblockLevelPattern.matchMatcher(line) {
                    return group("sblvl").toInt().toString()
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKILL_GARDEN_DUNGEON_LEVELS)) {
            if (chestName == "Your Skills" || chestName == "Dungeoneering") {
                if (item.getLore().isNotEmpty() && item.getLore().last().equals("§eClick to view!")) {
                    if (chestName == "Your Skills") {
                        if (CollectionAPI.isCollectionTier0(item.getLore()) && (itemName != ("Dungeoneering"))) return "0"
                        if (itemName.split(" ").size < 2) return "" //thanks to watchdogshelper we had to add this hotfix line
                        (("(?<skillReal>([\\w]+(?<!Dungeoneering))) (?<level>[\\w]+)").toPattern()).matchMatcher(itemName) {
                            return "${group(" level").romanToDecimalIfNeeded()}"
                        }
                    } else if (chestName == "Dungeoneering") {
                        dungeonClassLevelPattern.matchMatcher(itemName) {
                            return group("level")
                        }
                    }
                }
            } else if (((chestName == "Farming Skill") || (chestName == "Desk"))) {
               (("Garden Level (?<level>[\\w]+)").toPattern()).matchMatcher(itemName) {
                   if (GardenAPI.getGardenLevel() != 0) return GardenAPI.getGardenLevel().toString()
                   return group("level")
               }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKILL_AVERAGE) && (chestName.lowercase() == ("skyblock menu") && (itemName == ("Your Skills")))) {
            for (line in item.getLore()) {
                skillAvgPattern.matchMatcher(line) { return group("avg").toDouble().toInt().toString() }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.COLLECTION_LEVELS_AND_PROGRESS)) {
            ((".*Collections").toPattern()).matchMatcher(chestName) {
                val lore = item.getLore()
                if (lore.last() == ("§eClick to view!")) {
                    if (CollectionAPI.isCollectionTier0(lore)) return "0"
                    item.name?.let {
                        if (it.startsWith("§e")) {
                            val text = it.split(" ").last()
                            return "${text.romanToDecimalIfNeeded()}"
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
                        (".*Collections .*: .*(§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()).matchMatcher(line) { return group("percent").replace("100", "§a✔") }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.CRAFTED_MINIONS) && chestName.startsWith("Crafted Minions")) {
            val lore = item.getLore()
            if (itemName == "Information") {
                for (line in lore) {
                    (("(§.)*Craft (§.)*(?<count>[\\w]+) (§.)*more (§.)*unique.*").toPattern()).matchMatcher(line) {
                        return group("count")
                    }
                }
            }
            (("§eClick to view .*").toPattern()).matchMatcher(lore.last()) {
                var tiersToSubtract = 0
                var totalTiers = 0
                for (line in lore) {
                    ((".* Tier .*").toPattern()).matchMatcher(line) { totalTiers++ } //§c
                    ((".* Tier .*§c.*").toPattern()).matchMatcher(line) { tiersToSubtract++ }
                }
                return "${totalTiers - tiersToSubtract}".replace("$totalTiers", "§a✔")
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.MUSEUM_PROGRESS) && chestName.endsWith("Your Museum") && hannibalInsistedOnThisList.contains(itemName)) {
            for (line in item.getLore()) {
                museumDonationPattern.matchMatcher(line) { return group("amount").toDouble().toInt().toString().replace("100", "§a✔") }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.PROFILE_ICON) && chestName == ("Profile Management") && itemName.contains("Profile: ")) {
            profileManagementPattern.matchMatcher(itemName) { return group("icon") } ?: return "©"
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.PET_SCORE_STATUS)) {
            if ((chestName.lowercase() == "skyblock menu")) {
                ((".*Pets.*").toPattern()).matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        (("(§.)*Selected Pet: (§.)*None").toPattern()).matchMatcher(line) { return "§c§l✖" }
                    }
                }
            }
            ((".* Pets").toPattern()).matchMatcher(chestName) {
                if (itemName == ("Pet Score Rewards") && item.getLore().isNotEmpty()) {
                    (("(§.)*Your Pet Score: (§.)*(?<score>[\\w]+)").toPattern()).matchMatcher(item.getLore().last()) {
                        return group("score")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.ESSENCE_COUNTS)) {
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

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.MINION_QUICK_UPGRADE) && (chestName.contains(" Minion ")) && itemName.contains("Quick") && itemName.contains("Upgrade Minion")) {
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
