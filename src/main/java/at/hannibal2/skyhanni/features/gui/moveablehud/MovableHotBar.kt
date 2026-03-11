package at.hannibal2.skyhanni.features.gui.moveablehud

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MovableHotBar : MovableHudOverlay(
    RenderLayer.HOTBAR,
    displayName = "Hotbar",
    width = 182,
    height = 22,
    anchorOffsetX = 91,
    anchorOffsetY = 22,
) {
    override val config get() = SkyHanniMod.feature.gui.hotbar
}
