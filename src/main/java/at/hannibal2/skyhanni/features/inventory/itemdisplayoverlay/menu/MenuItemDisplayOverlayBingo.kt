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
    private val bingoSubgroup = itemStackSizeGroup.group("bingo")

    private val secretBingoDiscoveryLoreLinePattern by bingoSubgroup.pattern(("secretbingodiscovery.loreline"), ("(§.)*You were the (§.)*(?<rank>[\\w]+)(?<ordinal>(st|nd|rd|th)) (§.)*to"))
    private val rowColumnDiagonalItemNamePattern by bingoSubgroup.pattern(("rowcolumndiagonal.itemname"), ("(§.)*((Community )?Diagonal|Row #.|Column #.)"))
    private val topBlankPercentContribLoreLinePattern by bingoSubgroup.pattern(("topblankpercentcontrib.loreline"), ("((  )?(§.)?)?Top (§.)*(?<toUse>[\\w]{0,2})(.(?<decimal>[\\w]+))?%"))
    private val communityPersonalGoalLoreLinePattern by bingoSubgroup.pattern(("communitypersonalgoal.loreline"), ("(§.)*(?<goalType>Community|Personal) Goal"))

    @SubscribeEvent
    override fun onRenderItemTip(event: RenderItemTipEvent) {
        super.onRenderItemTip(event)
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
                        group("rank").formatNumber().let { if (it < 10000) return "§6${NumberUtil.format(it)}" }
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.Bingo.ROW_COLUMN_DIAGONAL_PROGRESS)) {
                rowColumnDiagonalItemNamePattern.matchMatcher(itemName) {
                    return if (lore.lastOrNull() == "§aBINGO!") greenCheckmark else if (lore.lastOrNull() == "§cINCOMPLETE") bigRedCross else ""
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
