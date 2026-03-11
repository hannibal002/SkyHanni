package at.hannibal2.skyhanni.features.gui.moveablehud

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MovableHeldItemTooltip : MovableHudOverlay(
    RenderLayer.HELD_ITEM_TOOLTIP,
    displayName = "Held Item Tooltip",
    width = 182,
    height = 10,
    anchorOffsetX = 91,
    anchorOffsetY = 59,
) {
    override val config get() = SkyHanniMod.feature.gui.heldItemTooltip
}
