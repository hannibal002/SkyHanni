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
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNeeded
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayer {
    // private val genericPercentPattern = ".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()
    private val museumDonationLoreLinePattern = "§7Items Donated: §.(?<amount>[0-9.]+).*".toPattern()
    private val skyblockLevelLoreLinePattern = "§7Your SkyBlock Level: §.?\\[§.?(?<sblvl>[0-9]{0,3})§.?].*".toPattern()
    private val skillAvgLoreLinePattern = "§[0-9](?<avg>[0-9]{1,2}(\\.[0-9])?) Skill Avg\\..*".toPattern()
    private val dungeonClassLevelItemNamePattern = "(?<class>[A-z ]+)( )(?<level>[0-9]+)".toPattern()
    private val dungeonEssenceRewardItemNamePattern = "(§.)?(?<type>[A-z]+) (Essence) (§.)?x(?<amount>[0-9]+)".toPattern()
    private val essenceCountLoreLinePattern = "(§.)?Your (?<essencetype>.+) Essence: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)".toPattern()
    private val essenceCountOtherLoreLinePattern = ".*(§.)You currently own (§.)(?<total>(?<useful>[0-9]+)(,[0-9]+)*)(§.(?<essencetype>[\\w]+))?.*".toPattern()
    private val profileManagementLoreLinePattern = "(?<icon>.)? (?<type>.+)?(?<profile> Profile: )(?<fruit>.+)".toPattern() // FOR THIS EXPRESSION SPECIFICALLY, FORMATTING CODES ***MUST*** BE REMOVED FIRST, OTHERWISE THIS REGEX WONT WORK!!! -ERY
    private val skyblockLevelingItemNamePattern = ((".* Leveling").toPattern())
    private val skillLevelItemNamePattern = (("(?<skillReal>([\\w]+(?<!Dungeoneering))) (?<level>[\\w]+)").toPattern())
    private val gardenLevelSkillLevelItemNamePattern = (("Garden Level (?<level>[\\w]+)").toPattern())
    private val collectionsChestNameItemNamePattern = ((".*Collections").toPattern())
    private val collectionLevelItemNamePattern = ((".*(§.)+(?<collection>[\\w ]+) (?<tier>[MDCLXVI]+)").toPattern())
    private val collectionsPercentLoreLinePattern = ((".*Collections .*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()))
    private val craftMoreMinionsLoreLinePattern = (("(§.)*Craft (§.)*(?<count>[\\w]+) (§.)*more (§.)*unique.*").toPattern())
    private val clickToViewLoreLinePattern = (("§eClick to view .*").toPattern())
    private val minionTierCraftProgressLoreLinePattern = ((".* Tier .*").toPattern())
    private val minionTierNotYetCraftedLoreLinePattern = ((".*§c.* Tier .*").toPattern())
    private val petsItemNamePattern = ((".*Pets.*").toPattern())
    private val noPetLoreLinePattern = (("(§.)*Selected (p|P)et: (§.)*None").toPattern())
    private val petsChestNamePattern = (("Pets.*").toPattern())
    private val yourPetScoreLoreLinePattern = ((".*(§.)*Your Pet Score: (§.)*(?<score>[\\w]+).*").toPattern())
    private val minionMenuChestNamePattern = ((".* Minion .*").toPattern())
    private val quickUpgradeItemNamePattern = (("Quick.Upgrade Minion").toPattern())
    private val youNeedXMoreMaterialsLoreLinePattern = ((".*(§.)+You need (§.)*(?<needed>[\\w]+).*").toPattern())
    private val doesNotContainArrowsChestNamePattern = (("^((?! ➜ ).)*\$").toPattern())
    private val canDisplayEssenceChestNameItemNamePattern = ((".*Essence( Guide.*| Shop)?").toPattern())
    private val museumItemNamesList = listOf(
        "Museum",
        "Rarities",
        "Armor Sets",
        "Weapons",
        "Special Items"
    )

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.player.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.player
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKYBLOCK_LEVEL) && chestName.lowercase() == ("skyblock menu")) {
            //itemName.endsWith(" Leveling")
            skyblockLevelingItemNamePattern.matchMatcher(itemName) {
                for (line in item.getLore()) {
                    skyblockLevelLoreLinePattern.matchMatcher(line) {
                        return "${group("sblvl")}"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKILL_GARDEN_DUNGEON_LEVELS)) {
            if (chestName == "Your Skills" || chestName == "Dungeoneering") {
                if (item.getLore().isNotEmpty() && item.getLore().last().equals("§eClick to view!")) {
                    if (chestName == "Your Skills") {
                        if (CollectionAPI.isCollectionTier0(item.getLore()) && (itemName != ("Dungeoneering"))) return "0"
                        if (itemName.split(" ").size < 2) return "" //thanks to watchdogshelper we had to add this hotfix line
                        skillLevelItemNamePattern.matchMatcher(itemName) {
                            return "${group("level").romanToDecimalIfNeeded()}"
                        }
                    } else if (chestName == "Dungeoneering") {
                        dungeonClassLevelItemNamePattern.matchMatcher(itemName) {
                            return group("level")
                        }
                    }
                }
            } else if (((chestName == "Farming Skill") || (chestName == "Desk"))) {
               gardenLevelSkillLevelItemNamePattern.matchMatcher(itemName) {
                   if (GardenAPI.getGardenLevel() != 0) return GardenAPI.getGardenLevel().toString()
                   return group("level")
               }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKILL_AVERAGE) && (chestName.lowercase() == ("skyblock menu") && (itemName == ("Your Skills")))) {
            for (line in item.getLore()) {
                skillAvgLoreLinePattern.matchMatcher(line) { return group("avg").toDouble().toInt().toString() }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.COLLECTION_LEVELS_AND_PROGRESS)) {
            collectionsChestNameItemNamePattern.matchMatcher(chestName) {
                val lore = item.getLore()
                if (lore.isNotEmpty() && lore.last() == ("§eClick to view!")) {
                    if (CollectionAPI.isCollectionTier0(lore)) return "0"
                    item.name?.let {
                        collectionLevelItemNamePattern.matchMatcher(it) {
                            return "${group("tier").romanToDecimalIfNeeded()}"
                        }
                    }
                }
            }
            if (chestName.lowercase() == "skyblock menu") {
                if (itemName == "Collections") {
                    for (line in item.getLore()) {
                        collectionsPercentLoreLinePattern.matchMatcher(line) { return group("percent").replace("100", "§a✔") }
                    }
                }
            }
            collectionsChestNameItemNamePattern.matchMatcher(chestName) {
                collectionsChestNameItemNamePattern.matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        collectionsPercentLoreLinePattern.matchMatcher(line) { return group("percent").replace("100", "§a✔") }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.CRAFTED_MINIONS) && chestName == ("Crafted Minions")) {
            val lore = item.getLore()
            if (itemName == "Information") {
                for (line in lore) {
                    craftMoreMinionsLoreLinePattern.matchMatcher(line) {
                        return group("count")
                    }
                }
            }
            if (lore.isNotEmpty()) {
                clickToViewLoreLinePattern.matchMatcher(lore.last()) {
                    var tiersToSubtract = 0
                    var totalTiers = 0
                    for (line in lore) {
                        minionTierCraftProgressLoreLinePattern.matchMatcher(line) { totalTiers++ } //§c
                        minionTierNotYetCraftedLoreLinePattern.matchMatcher(line) { tiersToSubtract++ }
                    }
                    return "${totalTiers - tiersToSubtract}".replace("$totalTiers", "§a✔")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.MUSEUM_PROGRESS) && chestName == ("Your Museum") && museumItemNamesList.contains(itemName)) {
            for (line in item.getLore()) {
                museumDonationLoreLinePattern.matchMatcher(line) { return group("amount").toDouble().toInt().toString().replace("100", "§a✔") }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.PROFILE_ICON) && chestName == ("Profile Management")) {
            profileManagementLoreLinePattern.matchMatcher(itemName) { return group("icon") } ?: return "©"
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.PET_SCORE_STATUS)) {
            if ((chestName.lowercase() == "skyblock menu")) {
                petsItemNamePattern.matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        noPetLoreLinePattern.matchMatcher(line) { return "§c§l✖" }
                    }
                }
            }
            petsChestNamePattern.matchMatcher(chestName) {
                if (itemName == ("Pet Score Rewards") && item.getLore().isNotEmpty()) {
                    yourPetScoreLoreLinePattern.matchMatcher(item.getLore().last()) {
                        return group("score")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.MINION_QUICK_UPGRADE)) {
            //one day admins are going to remove that damn hyphen in "Quick-Upgrade" and it's going to break this feature
            /*
             chestName.contains(" Minion ")
             itemName.contains("Quick") && itemName.contains("Upgrade Minion")
             */
            minionMenuChestNamePattern.matchMatcher(chestName) {
                quickUpgradeItemNamePattern.matchMatcher(itemName) {
                    val lore = item.getLore()
                    for (line in lore) {
                        youNeedXMoreMaterialsLoreLinePattern.matchMatcher(line) {
                            return group("needed")
                        }
                    }
                }
            }
        }
        // friendly note to future contribs: if you want to add more stack sizes to this category, you have to do it above this line or else it wont work. look, i don't make the rules, aight? -ery
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.ESSENCE_COUNTS)) {
            if (item.item != Items.skull) return ""
            if (LorenzUtils.isRewardChest()) {
                dungeonEssenceRewardItemNamePattern.matchMatcher(itemName) { return group("amount") } ?: return ""
            }
            var canDisplayEssence = false
            doesNotContainArrowsChestNamePattern.matchMatcher(chestName) {
                canDisplayEssenceChestNameItemNamePattern.matchMatcher(chestName) {
                    canDisplayEssenceChestNameItemNamePattern.matchMatcher(itemName) {
                        canDisplayEssence = true
                    }
                }
            }
            // !(chestName.contains(" ➜ ")) &&
            // (chestName.contains("Essence Shop") && itemName.contains("Essence Shop")) ||
            // (chestName.contains("Essence Guide") && itemName.endsWith(" Essence")) ||
            // (chestName.endsWith(" Essence"))
            if (canDisplayEssence) {
                val lore = item.getLore()
                var total = 0L
                loop@for (line in lore) {
                    essenceCountOtherLoreLinePattern.matchMatcher(line) {
                        total = group("total").formatNumber()
                        break@loop
                    }
                    essenceCountLoreLinePattern.matchMatcher(line) {
                        total = group("total").formatNumber()
                        break@loop
                    }
                }
                return NumberUtil.format(total)
            }
        }

        return ""
    }
}
