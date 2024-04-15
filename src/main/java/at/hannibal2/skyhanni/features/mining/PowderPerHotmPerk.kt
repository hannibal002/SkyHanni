package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.api.HotmAPI
import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PowderPerHotmPerk {

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!HotmAPI.inHotmGui()) return

        val itemName = event.itemStack.displayName
        val perkEnum = HotmData.getPerkByNameOrNull(itemName.removeColor()) ?: return

        if (perkEnum.getLevelUpCost() == null) return

        val currentPowderSpend = perkEnum.calculateTotalCost(perkEnum.activeLevel).addSeparators()
        val maxPowderNeeded = perkEnum.calculateTotalCost(perkEnum.maxLevel).addSeparators()

        event.toolTip.add(" ")

        if (perkEnum.activeLevel == perkEnum.maxLevel) {
            event.toolTip.add("§7Powder spend: §e${maxPowderNeeded} §7(§aMax level§7)")
        } else {
            event.toolTip.add("§7Powder spend: §e${currentPowderSpend}§7 / §e${maxPowderNeeded}")
        }
    }
}
