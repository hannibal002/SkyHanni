package at.hannibal2.hanni.features.mining

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.hotx.HotmData
import at.hannibal2.hanni.events.minecraft.ToolTipEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor

@HanniModule
object CurrentPowderOnHotmPerk {

    private val config get() = HanniMod.feature.mining.hotm

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
