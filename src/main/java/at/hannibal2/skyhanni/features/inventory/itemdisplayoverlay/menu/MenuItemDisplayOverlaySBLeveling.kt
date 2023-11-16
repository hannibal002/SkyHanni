package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.InventoryConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlaySBLeveling {

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.sbLeveling.isEmpty()) return ""
        val itemName = item.cleanName()
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.sbLeveling
        val chestName = InventoryUtils.openInventoryName()
        
        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.SBLeveling.GUIDE_PROGRESS)) {
            ((".*(Guide |Task).*").toPattern()).matchMatcher(chestName) {
                if (itemName.isNotEmpty()) {
                    for (line in item.getLore()) {
                        (".*Progress.*: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()).matchMatcher(line) {
                            return group("percent").replace("100", "§a✔")
                        }
                    }
                    if (itemName.contains("✔")) return "§a✔"
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.SBLeveling.WAYS_TO_LEVEL_UP_PROGRESS)) {
            for (line in item.getLore()) {
                (".*(§.)?Progress to Complete Category: (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()).matchMatcher(line) {
                    return group("percent").replace("100", "§a✔")
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.SBLeveling.SB_LEVELING_REWARDS)) {
            if ((itemName.isNotEmpty())) {
                ((".*(rewards|skyblock leveling).*").toPattern()).matchMatcher(chestName.lowercase()) {
                    for (line in item.getLore()) {
                        if (line.contains("Progress to ") || line.contains("Rewards Unlocked: ")) {
                            (".* (§.)?(?<percent>[0-9]+)(\\.[0-9]*)?(§.)?%".toPattern()).matchMatcher(line) {
                                 return group("percent").replace("100", "§a✔")
                            }
                        }
                    }
                }
            }
        }

        if (stackSizeConfig.contains(InventoryConfig.StackSizeConfig.StackSizeMenuConfig.SBLeveling.EMBLEMS_UNLOCKED)) {
            val nameWithColor = item.name ?: return ""
            if ((chestName == ("Emblems"))) {
                (("^§a(\\S*)\$").toPattern()).matchMatcher(nameWithColor) {
                    (("(§.)?(?<emblems>[\\d]+) Unlocked").toPattern()).matchMatcher(item.getLore().first()) {
                        return group("emblems")
                    }
                }
            }
        }

        return ""
    }
}
