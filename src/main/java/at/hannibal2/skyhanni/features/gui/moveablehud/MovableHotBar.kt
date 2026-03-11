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
    private val config get() = SkyHanniMod.feature.gui.hotbar
    override val position get() = config.hotbar
    override fun isEnabled() = isInSkyBlockOrEnabled(config.showOutsideSkyblock, config.editable)
}
