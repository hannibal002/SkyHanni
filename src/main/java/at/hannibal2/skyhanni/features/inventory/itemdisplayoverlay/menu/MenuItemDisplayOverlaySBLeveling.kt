package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlaySBLeveling : AbstractMenuStackSize() {
    private val guideTaskChestNamePattern by RepoPattern.pattern(("itemstacksize.sbleveling.guidetask.chestname"), (".*(Guide |Task).*"))
    private val progressPatternLoreLinePattern by RepoPattern.pattern(("itemstacksize.sbleveling.progresspattern.loreline"), (".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val checkmarkItemNamePattern by RepoPattern.pattern(("itemstacksize.sbleveling.checkmark.itemname"), ("✔.*"))
    private val progressToCompleteCategoryPercentLoreLinePattern by RepoPattern.pattern(("itemstacksize.sbleveling.progresstocompletecategorypercent.loreline"), (".*(§.)?Progress to Complete Category: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val rewardsSkyblockLevelingChestNamePattern by RepoPattern.pattern(("itemstacksize.sbleveling.rewardsskyblockleveling.chestname"), (".*(rewards|skyblock leveling).*"))
    private val progressToRewardsUnlockedPatternLoreLinePattern by RepoPattern.pattern(("itemstacksize.sbleveling.progresstorewardsunlockedpattern.loreline"), (".*(Progress to .*|Rewards Unlocked:) (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%"))
    private val greenItemNamePattern by RepoPattern.pattern(("itemstacksize.sbleveling.green.itemname"), ("^§a(\\S*)\$"))
    private val emblemsUnlockedLoreLinePattern by RepoPattern.pattern(("itemstacksize.sbleveling.emblemsunlocked.loreline"), ("(§.)?(?<emblems>[\\d]+) Unlocked"))

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.sbLeveling.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = configMenuStackSize.sbLeveling
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.GUIDE_PROGRESS)) {
            guideTaskChestNamePattern.matchMatcher(chestName) {
                if (itemName.isNotEmpty()) {
                    for (line in item.getLore()) {
                        progressPatternLoreLinePattern.matchMatcher(line) {
                            return group("percent").replace("100", "§a✔")
                        }
                    }
                    checkmarkItemNamePattern.matchMatcher(itemName) {
                        return "§a✔"
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.WAYS_TO_LEVEL_UP_PROGRESS)) {
            for (line in item.getLore()) {
                progressToCompleteCategoryPercentLoreLinePattern.matchMatcher(line) {
                    return group("percent").replace("100", "§a✔")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.SB_LEVELING_REWARDS)) {
            if ((itemName.isNotEmpty())) {
                rewardsSkyblockLevelingChestNamePattern.matchMatcher(chestName.lowercase()) {
                    for (line in item.getLore()) {
                        progressToRewardsUnlockedPatternLoreLinePattern.matchMatcher(line) {
                            return group("percent").replace("100", "§a✔")
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.EMBLEMS_UNLOCKED)) {
            val nameWithColor = item.name ?: return ""
            if ((chestName == ("Emblems"))) {
                greenItemNamePattern.matchMatcher(nameWithColor) {
                    emblemsUnlockedLoreLinePattern.matchMatcher(item.getLore().first()) {
                        return group("emblems")
                    }
                }
            }
        }

        return ""
    }
}
