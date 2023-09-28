package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawSlotText
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAppliedPocketSackInASack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PocketSackInASackDisplay {

    private val config get() = SkyHanniMod.feature.misc.pocketSackInASack
    private val maxedStitched = 3

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyBlock || stack.stackSize != 1) return
        if (!config.showOverlay) return
        val pocketSackInASackApplied = stack.getAppliedPocketSackInASack() ?: return

        val stackTip = "§a$pocketSackInASackApplied"
        val x = event.x + 13
        val y = event.y + 1

        event.drawSlotText(x, y, stackTip, .9f)
    }

    @SubscribeEvent
    fun onTooltip(event: LorenzToolTipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.replaceLore) return
        val itemStack = event.itemStack
        val applied = itemStack.getAppliedPocketSackInASack() ?: return

        if (!ItemUtils.isSack(itemStack.displayName)) return
        val iterator = event.toolTip.listIterator()
        var next = false
        for (line in iterator) {
            if (line.contains("7This sack is")) {
                val color = if (applied == maxedStitched) "§a" else "§b"
                iterator.set("§7This sack is stitched $color$applied§7/$color$maxedStitched")
                next = true
                continue
            }
            if (next) {
                iterator.set("§7times with a §cPocket Sack-in-a-Sack§7.")
                return
            }
        }
    }
}