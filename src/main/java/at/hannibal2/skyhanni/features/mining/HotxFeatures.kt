package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hotx.HotfData
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.RenderItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object HotxFeatures {

    private val configHotm get() = SkyHanniMod.feature.mining.hotm
    private val configHotf get() = SkyHanniMod.feature.foraging.hotf

    @HandleEvent(onlyOnSkyblock = true)
    fun onBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (!(configHotm.highlightEnabledPerks && HotmData.inInventory) &&
            !(configHotf.highlightEnabledPerks && HotfData.inInventory)
        ) return
        HotmData.entries.forEach { entry ->
            val color = if (!entry.isUnlocked) LorenzColor.DARK_GRAY
            else if (entry.enabled) LorenzColor.GREEN else LorenzColor.RED
            entry.slot?.highlight(color)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderTip(event: RenderItemTipEvent) {
        handleLevelStackSize(event)
        handleTokenStackSize(event)
    }

    private fun handleLevelStackSize(event: RenderItemTipEvent) {
        if (!(configHotm.levelStackSize && HotmData.inInventory) &&
            !(configHotf.levelStackSize && HotfData.inInventory)
        ) return
        HotmData.entries.firstOrNull {
            event.stack.displayName == it.item?.displayName
        }?.let {
            event.stackTip = if (it.activeLevel == 0 || it.activeLevel == it.maxLevel) "" else "§e${it.activeLevel}"
            it.activeLevel.toString()
        }
    }

    private fun handleTokenStackSize(event: RenderItemTipEvent) {
        if (!(configHotm.tokenStackSize && HotmData.inInventory)
            && !(configHotf.tokenStackSize && HotfData.inInventory)
        ) return
        if (event.stack.displayName != HotmData.heartItem?.stack?.displayName) return
        event.stackTip = HotmData.availableTokens.takeIf { it != 0 }?.let { "§b$it" }.orEmpty()
    }

}
