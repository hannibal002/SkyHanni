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
    
    private val guideTaskChestPattern by sbLevelingSubgroup.pattern(
        "guide.task.chestname",
        ".*(Guide |Task).*"
    )
    private val progressPattern by sbLevelingSubgroup.pattern(
        "progress.loreline",
        ".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val checkmarkPattern by sbLevelingSubgroup.pattern(
        "checkmark.itemname",
        "✔.*"
    )
    private val categoryPercentPattern by sbLevelingSubgroup.pattern(
        "category.percent.loreline",
        ".*(§.)?Progress to Complete Category: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val rewardsSBLevelingChestPattern by sbLevelingSubgroup.pattern(
        "rewards.skyblockleveling.chestname",
        ".*(rewards|skyblock leveling).*"
    )
    private val progressRewardsPattern by sbLevelingSubgroup.pattern(
        "progres.rewards.loreline",
        ".*(Progress to .*|Rewards Unlocked:) (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"
    )
    private val greenPattern by sbLevelingSubgroup.pattern(
        "green.itemname",
        "^§a(\\S*)\$"
    )
    private val emblemsUnlockedPattern by sbLevelingSubgroup.pattern(
        "emblemsunlocked.loreline",
        "(§.)?(?<emblems>\\d+) Unlocked"
    )
    private val emblemPreviewPattern by sbLevelingSubgroup.pattern(
        "emblem.preview.itemname",
        "(?<status>§.)+(?<emblemName>[\\S ]+) (?<theEmblem>(§.)+.+)"
    )
    private val emblemPattern by sbLevelingSubgroup.pattern(
        "emblem.loreline",
        "((§.)§8(\\S[^:][\\S ]+)|§8Locked)"
    )

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

        if (lore.isNotEmpty()) {
            if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.GUIDE_PROGRESS)) {
                guideTaskChestPattern.matchMatcher(chestName) {
                    if (itemName.isNotEmpty()) {
                        for (line in lore) {
                            progressPattern.matchMatcher(line) {
                                return group("percent").convertPercentToGreenCheckmark()
                            }
                        }
                        checkmarkPattern.matchMatcher(itemName) {
                            return greenCheckmark
                        }
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.WAYS_TO_LEVEL_UP_PROGRESS)) {
                for (line in lore) {
                    categoryPercentPattern.matchMatcher(line) {
                        return group("percent").convertPercentToGreenCheckmark()
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.SB_LEVELING_REWARDS)) {
                if ((itemName.isNotEmpty())) {
                    rewardsSBLevelingChestPattern.matchMatcher(chestName.lowercase()) {
                        for (line in lore) {
                            progressRewardsPattern.matchMatcher(line) {
                                return group("percent").convertPercentToGreenCheckmark()
                            }
                        }
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.EMBLEMS_UNLOCKED) && (chestName == ("Emblems"))) {
                val nameWithColor = item.name ?: return ""
                greenPattern.matchMatcher(nameWithColor) {
                    emblemsUnlockedPattern.matchMatcher(lore.first()) {
                        return group("emblems")
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.EMBLEM_PREVIEW) && (chestName == ("Emblems")) && !(emblemsUnlockedPattern.matches(lore.first())) && (emblemPattern.matches(lore.first()))) {
                val nameWithColor = item.name ?: return ""
                emblemPreviewPattern.matchMatcher(nameWithColor) {
                    return group("theEmblem")
                }
            }
        }

        return ""
    }
}
