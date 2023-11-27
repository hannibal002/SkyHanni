package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlaySBLeveling {
    private val guideTaskChestNamePattern = ((".*(Guide |Task).*").toPattern())
    private val progressPatternLoreLinePattern = ((".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val checkmarkItemNamePattern = (("✔.*").toPattern())
    private val progressToCompleteCategoryPercentLorePattern = ((".*(§.)?Progress to Complete Category: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val rewardsSkyblockLevelingChestNamePattern = ((".*(rewards|skyblock leveling).*").toPattern())
    private val progressToRewardsUnlockedPatternLorePattern = ((".*(Progress to .*|Rewards Unlocked:) (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%").toPattern())
    private val greenItemNamePattern = (("^§a(\\S*)\$").toPattern())
    private val emblemsUnlockedLorePattern = (("(§.)?(?<emblems>[\\d]+) Unlocked").toPattern())

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.sbLeveling.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.sbLeveling
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
                progressToCompleteCategoryPercentLorePattern.matchMatcher(line) {
                    return group("percent").replace("100", "§a✔")
                }
            }
        }

        if (stackSizeConfig.contains(StackSizeMenuConfig.SBLeveling.SB_LEVELING_REWARDS)) {
            if ((itemName.isNotEmpty())) {
                rewardsSkyblockLevelingChestNamePattern.matchMatcher(chestName.lowercase()) {
                    for (line in item.getLore()) {
                        progressToRewardsUnlockedPatternLorePattern.matchMatcher(line) {
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
                    emblemsUnlockedLorePattern.matchMatcher(item.getLore().first()) {
                        return group("emblems")
                    }
                }
            }
        }

        return ""
    }
}
