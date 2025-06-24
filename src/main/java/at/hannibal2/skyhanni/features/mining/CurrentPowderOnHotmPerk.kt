package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object CurrentPowderOnHotmPerk {

    private val config get() = SkyHanniMod.feature.mining.hotm

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        val itemName = event.itemStack.displayName
        val perk = HotmData.getPerkByNameOrNull(itemName.removeColor()) ?: return

        if (perk.isMaxLevel || !perk.isUnlocked) return

        val powderType = perk.powderType ?: return
        val index = event.toolTip.indexOfFirst { it.contains("Cost") }

        event.toolTip.add(index + 2, " ")
        event.toolTip.add(index + 3, "You have")
        event.toolTip.add(index + 4, "${powderType.color}${powderType.current.addSeparators()} ${powderType.displayName} Powder")
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HotmData.inInventory && config.currentPowder

}
