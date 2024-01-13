package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.api.CollectionAPI
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayPlayer : AbstractMenuStackSize() {
    private val playerGeneralSubgroup = itemStackSizeGroup.group("player.general")
    
    private val museumDonationPattern by playerGeneralSubgroup.pattern(
        "museum.donation.loreline",
        "§7Items Donated: §.(?<amount>[0-9.]+).*"
    )
    private val skyblockLevelPattern by playerGeneralSubgroup.pattern(
        "skyblock.level.loreline",
        "§7Your SkyBlock Level: §.?\\[§.?(?<sblvl>[0-9]{0,3})§.?].*"
    )
    private val skillAvgPattern by playerGeneralSubgroup.pattern(
        "skill.avg.loreline",
        "§[0-9](?<avg>[0-9]{1,2}(\\.[0-9])?) Skill Avg\\..*"
    )
    private val dungeonClassLevelPattern by playerGeneralSubgroup.pattern(
        "dungeon.class.level.itemname",
        "(?<class>[A-z ]+)( )(?<level>[0-9]+)"
    )
    private val dungeonEssenceRewardPattern by playerGeneralSubgroup.pattern(
        "dungeon.essence.reward.itemname",
        "(§.)?(?<type>[A-z]+) (Essence) (§.)?x(?<amount>[0-9]+)"
    )
    private val essenceCountPattern by playerGeneralSubgroup.pattern(
        "essence.count.one.loreline",
        "(§.)?Your (?<essencetype>.+) Essence: (§.)?(?<total>(?<useful>[0-9]+)(,[0-9]+)*)"
    )
    private val essenceCountOtherPattern by playerGeneralSubgroup.pattern(
        "essence.count.two.loreline",
        ".*(§.)You currently own (§.)(?<total>(?<useful>[0-9]+)(,[0-9]+)*)(§.(?<essencetype>[\\w]+))?.*"
    )
    private val profileIconPattern by playerGeneralSubgroup.pattern(
        "profile.icon.one.displayname",
        "(((§.)*(?<icon>[^A-z])? (§.)*(?<type>.+)?) ?(§.)*(?<profile>Profile: )(§.)*(?<fruit>.+))"
    )
    private val profileIconOtherPattern by playerGeneralSubgroup.pattern(
        "profile.icon.two.displayname",
        "(§.)*(Profile: )(§.)*(?<fruit>[\\w]+)"
    )
    private val sbLevelingPattern by playerGeneralSubgroup.pattern(
        "skyblock.leveling.itemname",
        ".* Leveling"
    )
    private val skillLevelPattern by playerGeneralSubgroup.pattern(
        "skill.level.itemname",
        "(?<skillReal>([\\w]+(?<!Dungeoneering))) (?<level>[\\w]+)"
    )
    private val gardenLevelPattern by playerGeneralSubgroup.pattern(
        "skill.gardenlevel.itemname",
        "Garden Level (?<level>[\\w]+)"
    )
    private val collectionsChestItemPattern by playerGeneralSubgroup.pattern(
        "collections.chestnameitemname",
        ".*Collections"
    )
    private val collectionLevelPattern by playerGeneralSubgroup.pattern(
        "collection.level.itemname",
        ".*(§.)+(?<collection>[\\w ]+) (?<tier>[MDCLXVI]+)"
    )
    private val collectionsPercentPattern by playerGeneralSubgroup.pattern(
        "collections.percent.loreline",
        ".*Collections .*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val craftMoreMinionsPattern by playerGeneralSubgroup.pattern(
        "craftmoreminions.loreline",
        "(§.)*Craft (§.)*(?<count>\\w+) (§.)*more (§.)*unique.*"
    )
    private val clickToViewPattern by playerGeneralSubgroup.pattern(
        "clicktoview.loreline",
        "§eClick to view .*"
    )
    private val minionTierCraftProgressPattern by playerGeneralSubgroup.pattern(
        "miniontiercraftprogress.loreline",
        ".* Tier .*"
    )
    private val minionNotCraftedPattern by playerGeneralSubgroup.pattern(
        "minion.notcrafted.loreline",
        ".*§c.* Tier .*"
    )
    private val petsPattern by playerGeneralSubgroup.pattern(
        "pets.itemname",
        ".*Pets.*"
    )
    private val petsNoPetPattern by playerGeneralSubgroup.pattern(
        "pets.nopet.loreline",
        "(§.)*Selected ([pP])et: (§.)*None"
    )
    private val petsChestPattern by playerGeneralSubgroup.pattern(
        "pets.chestname",
        "Pets.*"
    )
    private val petScorePattern by playerGeneralSubgroup.pattern(
        "pets.petscore.loreline",
        ".*(§.)*Your Pet Score: (§.)*(?<score>[\\w]+).*"
    )
    private val minionChestPattern by playerGeneralSubgroup.pattern(
        "minion.chestname",
        ".* Minion .*"
    )
    private val quickUpgradePattern by playerGeneralSubgroup.pattern(
        "minion.quickupgrade.itemname",
        "Quick.Upgrade Minion"
    )
    private val moreMaterialsPattern by playerGeneralSubgroup.pattern(
        "minion.morematerials.loreline",
        ".*(§.)+You need (§.)*(?<needed>[\\w]+).*"
    )
    private val noArrowsChestPattern by playerGeneralSubgroup.pattern(
        "essence.noarrows.chestname",
        "^((?! ➜ ).)*\$"
    )
    private val displayEssenceChestItemPattern by playerGeneralSubgroup.pattern(
        "essence.display.chestnameitemname",
        ".*Essence( Guide.*| Shop)?"
    )
    private val museumItemNamesList = listOf(
        "Museum",
        "Rarities",
        "Armor Sets",
        "Weapons",
        "Special Items"
    )

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.player.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = configMenuStackSize.player
        val chestName = InventoryUtils.openInventoryName()

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKYBLOCK_LEVEL) && chestName.lowercase() == ("skyblock menu")) {
            sbLevelingPattern.matchMatcher(itemName) {
                for (line in item.getLore()) {
                    skyblockLevelPattern.matchMatcher(line) {
                        return group("sblvl")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.SKILL_GARDEN_DUNGEON_LEVELS)) {
            if (chestName == "Your Skills" || chestName == "Dungeoneering") {
                if (item.getLore().isNotEmpty() && item.getLore().last() == ("§eClick to view!")) {
                    if (chestName == "Your Skills") {
                        if (CollectionAPI.isCollectionTier0(item.getLore()) && (itemName != ("Dungeoneering"))) return "0"
                        if (itemName.split(" ").size < 2) return ""
                        skillLevelPattern.matchMatcher(itemName) {
                            return "${group("level").romanToDecimalIfNecessary()}"
                        }
                    } else if (chestName == "Dungeoneering") {
                        dungeonClassLevelPattern.matchMatcher(itemName) {
                            return group("level")
                        }
                    }
                }
            } else if (((chestName == "Farming Skill") || (chestName == "Desk"))) {
               gardenLevelPattern.matchMatcher(itemName) {
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
            collectionsChestItemPattern.matchMatcher(chestName) {
                val lore = item.getLore()
                if (lore.isNotEmpty() && lore.last() == ("§eClick to view!")) {
                    if (CollectionAPI.isCollectionTier0(lore)) return "0"
                    item.name?.let {
                        collectionLevelPattern.matchMatcher(it) {
                            return "${group("tier").romanToDecimalIfNecessary()}"
                        }
                    }
                }
            }
            if (chestName.lowercase() == "skyblock menu") {
                if (itemName == "Collections") {
                    for (line in item.getLore()) {
                        collectionsPercentPattern.matchMatcher(line) { return group("percent").convertPercentToGreenCheckmark() }
                    }
                }
            }
            collectionsChestItemPattern.matchMatcher(chestName) {
                collectionsChestItemPattern.matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        collectionsPercentPattern.matchMatcher(line) { return group("percent").convertPercentToGreenCheckmark() }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.CRAFTED_MINIONS) && chestName == ("Crafted Minions")) {
            val lore = item.getLore()
            if (itemName == "Information") {
                for (line in lore) {
                    craftMoreMinionsPattern.matchMatcher(line) {
                        return group("count")
                    }
                }
            }
            if (lore.isNotEmpty()) {
                clickToViewPattern.matchMatcher(lore.last()) {
                    var tiersToSubtract = 0
                    var totalTiers = 0
                    for (line in lore) {
                        minionTierCraftProgressPattern.matchMatcher(line) { totalTiers++ }
                        minionNotCraftedPattern.matchMatcher(line) { tiersToSubtract++ }
                    }
                    return "${totalTiers - tiersToSubtract}".replace("$totalTiers", greenCheckmark)
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.MUSEUM_PROGRESS) && chestName == ("Your Museum") && museumItemNamesList.contains(itemName)) {
            for (line in item.getLore()) {
                museumDonationPattern.matchMatcher(line) { return group("amount").toDouble().toInt().toString().convertPercentToGreenCheckmark() }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.PROFILE_ICON) && chestName == ("Profile Management")) {
            profileIconPattern.matchMatcher(itemName) { return group("icon") }
            profileIconOtherPattern.matchMatcher(itemName) { return "©" }
            if (itemName == "Locked profile slot") {
                return "§c┏┓"
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.PET_SCORE_STATUS)) {
            if ((chestName.lowercase() == "skyblock menu")) {
                petsPattern.matchMatcher(itemName) {
                    for (line in item.getLore()) {
                        petsNoPetPattern.matchMatcher(line) { return bigRedCross }
                    }
                }
            }
            petsChestPattern.matchMatcher(chestName) {
                if (itemName == ("Pet Score Rewards") && item.getLore().isNotEmpty()) {
                    petScorePattern.matchMatcher(item.getLore().last()) {
                        return group("score")
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.MINION_QUICK_UPGRADE)) {
            minionChestPattern.matchMatcher(chestName) {
                quickUpgradePattern.matchMatcher(itemName) {
                    val lore = item.getLore()
                    for (line in lore) {
                        moreMaterialsPattern.matchMatcher(line) {
                            return group("needed")
                        }
                    }
                }
            }
        }
        // if adding more stack sizes to this category, do it above this line or else it wont work
        if (stackSizeConfig.contains(StackSizeMenuConfig.PlayerGeneral.ESSENCE_COUNTS)) {
            if (item.item != Items.skull) return ""
            if (LorenzUtils.isRewardChest()) {
                dungeonEssenceRewardPattern.matchMatcher(itemName) { return group("amount") } ?: return ""
            }
            var canDisplayEssence = false
            noArrowsChestPattern.matchMatcher(chestName) {
                displayEssenceChestItemPattern.matchMatcher(chestName) {
                    displayEssenceChestItemPattern.matchMatcher(itemName) {
                        canDisplayEssence = true
                    }
                }
            }
            if (canDisplayEssence) {
                val lore = item.getLore()
                var total = 0L
                loop@for (line in lore) {
                    essenceCountOtherPattern.matchMatcher(line) {
                        total = group("total").formatNumber()
                        break@loop
                    }
                    essenceCountPattern.matchMatcher(line) {
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
