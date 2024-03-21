package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SoulboundItemIcon {
    val config get() = SkyHanniMod.feature.inventory.soulbound
    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.icon) return

        val stack = event.stack ?: return

        val x = event.x + 6
        val y = event.y - 2

        for (line in stack.getLore()) {
            if (line == "§8§l* §8Soulbound §8§l*") {
                event.drawSlotText(x, y, "§5§l☍", 1.2f)
            }
            if (line == "§8§l* §8Co-op Soulbound §8§l*") {
                event.drawSlotText(x, y, "§d§l☍", 1.2f)
            }
        }
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.text) return
        for (line in event.toolTip) {
            if (line.contains("§8§l* §8Soulbound §8§l*") || line.contains("§8§l* §8Co-op Soulbound §8§l*")) {
                event.toolTip.remove(line)
                break
            }
        }
    }
}

