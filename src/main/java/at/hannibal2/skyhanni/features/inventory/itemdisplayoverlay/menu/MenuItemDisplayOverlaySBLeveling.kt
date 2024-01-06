package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlaySBLeveling : AbstractMenuStackSize() {
    private val sbLevelingSubgroup = itemStackSizeGroup.group("sbleveling")
    
    private val guideTaskChestNamePattern by sbLevelingSubgroup.pattern(("guidetask.chestname"), (".*(Guide |Task).*"))
    private val progressPatternLoreLinePattern by sbLevelingSubgroup.pattern(("progresspattern.loreline"), (".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val checkmarkItemNamePattern by sbLevelingSubgroup.pattern(("checkmark.itemname"), ("✔.*"))
    private val progressToCompleteCategoryPercentLoreLinePattern by sbLevelingSubgroup.pattern(("progresstocompletecategorypercent.loreline"), (".*(§.)?Progress to Complete Category: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val rewardsSkyblockLevelingChestNamePattern by sbLevelingSubgroup.pattern(("rewardsskyblockleveling.chestname"), (".*(rewards|skyblock leveling).*"))
    private val progressToRewardsUnlockedPatternLoreLinePattern by sbLevelingSubgroup.pattern(("progresstorewardsunlockedpattern.loreline"), (".*(Progress to .*|Rewards Unlocked:) (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val greenItemNamePattern by sbLevelingSubgroup.pattern(("green.itemname"), ("^§a(\\S*)\$"))
    private val emblemsUnlockedLoreLinePattern by sbLevelingSubgroup.pattern(("emblemsunlocked.loreline"), ("(§.)?(?<emblems>[\\d]+) Unlocked"))
    private val emblemPreviewItemNamePattern by sbLevelingSubgroup.pattern(("emblempreview.itemname"), ("(?<status>§.)+(?<emblemName>[\\S ]+) (?<theEmblem>(§.)+.+)")) // https://regex101.com/r/w3A9Lm/1 -ery

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.sbLeveling.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = configMenuStackSize.sbLeveling
        val chestName = InventoryUtils.openInventoryName()
        val lore = item.getLore()

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.GUIDE_PROGRESS)) {
            guideTaskChestNamePattern.matchMatcher(chestName) {
                if (itemName.isNotEmpty()) {
                    for (line in lore) {
                        progressPatternLoreLinePattern.matchMatcher(line) {
                            return group("percent").convertPercentToGreenCheckmark()
                        }
                    }
                    checkmarkItemNamePattern.matchMatcher(itemName) {
                        return "§a✔"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.WAYS_TO_LEVEL_UP_PROGRESS)) {
            for (line in lore) {
                progressToCompleteCategoryPercentLoreLinePattern.matchMatcher(line) {
                    return group("percent").convertPercentToGreenCheckmark()
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.SB_LEVELING_REWARDS)) {
            if ((itemName.isNotEmpty())) {
                rewardsSkyblockLevelingChestNamePattern.matchMatcher(chestName.lowercase()) {
                    for (line in lore) {
                        progressToRewardsUnlockedPatternLoreLinePattern.matchMatcher(line) {
                            return group("percent").convertPercentToGreenCheckmark()
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.EMBLEMS_UNLOCKED) && (chestName == ("Emblems"))) {
            val nameWithColor = item.name ?: return ""
            greenItemNamePattern.matchMatcher(nameWithColor) {
                emblemsUnlockedLoreLinePattern.matchMatcher(lore.first()) {
                    return group("emblems")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.EMBLEM_PREVIEW) && (chestName == ("Emblems")) && !(emblemsUnlockedLoreLinePattern.matches(lore.first()))) {
            val nameWithColor = item.name ?: return ""
            emblemPreviewItemNamePattern.matchMatcher(nameWithColor) {
                return group("theEmblem")
            }
        }

        return ""
    }
}
