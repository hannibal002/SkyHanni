package at.hannibal2.skyhanni.features.inventory.itemdisplayoverlay.menu

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.inventory.stacksize.StackSizeMenuConfig
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class MenuItemDisplayOverlayBingo {

    private val secretBingoDiscoveryLoreLinePattern = (("(§.)*You were the (§.)*(?<nth>[\\w]+)(?<ordinal>(st|nd|rd|th)) (§.)*to").toPattern())
    private val rowColumnDiagonalItemNamePattern = (("§e((Community )?Diagonal|Row #.|Column #.)").toPattern())

    @SubscribeEvent
    fun onRenderItemTip(event: RenderItemTipEvent) {
        event.stackTip = getStackTip(event.stack)
    }

    private fun getStackTip(item: ItemStack): String {
        if (SkyHanniMod.feature.inventory.stackSize.menu.bingo.isEmpty()) return ""
        val stackSizeConfig = SkyHanniMod.feature.inventory.stackSize.menu.bingo
        val lore = item.getLore()
        val chestName = InventoryUtils.openInventoryName()
        val itemName = item.cleanName()

        if (lore.isNotEmpty() && itemName.isNotEmpty() && (chestName == "Bingo Card")) { // only for items in bingo card menu and have item lore at all
            if (stackSizeConfig.contains(StackSizeMenuConfig.Bingo.SECRET_BINGO_DISCOVERY) && (lore.last() == "§aGOAL REACHED")) {
                for (line in lore) {
                    secretBingoDiscoveryLoreLinePattern.matchMatcher(line) {
                        val nth = group("nth").formatNumber()
                        if (nth < 10000) return "${NumberUtil.format(nth)}"
                    }
                }
            }

            if (stackSizeConfig.contains(StackSizeMenuConfig.Bingo.ROW_COLUMN_DIAGONAL_PROGRESS)) {
                rowColumnDiagonalItemNamePattern.matchMatcher(itemName) {
                    if (lore.last() != "§cINCOMPLETE") {
                        return "§a✔"
                    }
                }
            }
        }

        return ""
    }
}
