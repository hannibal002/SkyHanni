package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object SoulboundItemIcon {

    private val config get() = SkyHanniMod.feature.inventory.soulbound

    private val coopSoulboundPattern by RepoPattern.pattern(
        "inventory.coopsoulbound",
        "§8§l* §8Co-op Soulbound §8§l*"
    )
    private val soulboundPattern by RepoPattern.pattern(
        "inventory.soulbound",
        "§8§l* §8Soulbound §8§l*"
    )

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.showSymbol) return

        val stack = event.stack ?: return

        val x = event.x + 6
        val y = event.y - 2

        for (line in stack.getLore()) {
            soulboundPattern.matchMatcher(line) {
                event.drawSlotText(x, y, "§5§l☍", 1.2f)
            }
            coopSoulboundPattern.matchMatcher(line) {
                event.drawSlotText(x, y, "§d§l☍", 1.2f)
            }
        }
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.removeTooltip) return
        for (line in event.toolTip) {
            if (line.contains("§8§l* §8Soulbound §8§l*") || line.contains("§8§l* §8Co-op Soulbound §8§l*")) {
                event.toolTip.remove(line)
                break
            }
        }
    }
}

