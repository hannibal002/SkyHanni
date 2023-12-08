package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.AbstractMenuStackSize
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayBingo : AbstractMenuStackSize() {

    private val secretBingoDiscoveryLoreLinePattern = (("(§.)*You were the (§.)*(?<rank>[\\w]+)(?<ordinal>(st|nd|rd|th)) (§.)*to").toPattern())
    private val rowColumnDiagonalItemNamePattern = (("§e((Community )?Diagonal|Row #.|Column #.)").toPattern())
    private val topBlankPercentContribLoreLinePattern = (("((  )?(§.)?)?Top (§.)*(?<toUse>[\\w]{0,2})(.(?<decimal>[\\w]+))?%").toPattern())
    private val communityPersonalGoalLoreLinePattern = (("(§.)*(?<goalType>Community|Personal) Goal").toPattern())

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    override fun getStackTip(item: ItemStack): String {
        if (configMenuStackSize.bingo.isEmpty()) return ""
        val stackSizeConfig = configMenuStackSize.bingo
        val lore = item.getLore()
        val chestName = InventoryUtils.openInventoryName()
        val itemName = item.cleanName()

        if (lore.isNotEmpty() && itemName.isNotEmpty() && (chestName == "Bingo Card")) { // only for items in bingo card menu and have item lore at all
            if (stackSizeConfig.contains(StackSizeMenuConfig.Bingo.SECRET_BINGO_DISCOVERY) && (lore.lastOrNull() == "§aGOAL REACHED")) {
                for (line in lore) {
                    secretBingoDiscoveryLoreLinePattern.matchMatcher(line) {
                        val nth = group("rank").formatNumber()
                        if (nth < 10000) return "§6${NumberUtil.format(nth)}"
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.Bingo.ROW_COLUMN_DIAGONAL_PROGRESS)) {
                rowColumnDiagonalItemNamePattern.matchMatcher(itemName) {
                    if (lore.lastOrNull() == "§aBINGO!") {
                        return "§a§z✔"
                    }
                    else if (lore.lastOrNull() == "§cINCOMPLETE") {
                        return "§c§l✖"
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.Bingo.TOP_BLANK_PERCENT_COMMUNITY_GOAL_CONTRIB)) {
                communityPersonalGoalLoreLinePattern.matchMatcher(lore.first()) {
                    if (group("goalType") == "Community") {
                        for (line in lore) {
                            topBlankPercentContribLoreLinePattern.matchMatcher(line) {
                                return group("toUse")
                            }
                        }
                    }
                }
            }
        }

        return ""
    }
}
