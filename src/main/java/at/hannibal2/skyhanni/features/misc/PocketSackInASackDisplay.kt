package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderItemEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAppliedPocketSackInASack
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PocketSackInASackDisplay {

    @SubscribeEvent
    fun onRenderItemOverlayPost(event: GuiRenderItemEvent.RenderOverlayEvent.GuiRenderItemPost) {
        val stack = event.stack ?: return
        if (!LorenzUtils.inSkyBlock || stack.stackSize != 1) return
        if (!SkyHanniMod.feature.misc.sackInASackApplied) return
        val pocketSackInASackApplied = stack.getAppliedPocketSackInASack() ?: return

        val stackTip = "§a$pocketSackInASackApplied"

        GlStateManager.disableLighting()
        GlStateManager.disableDepth()
        GlStateManager.disableBlend()

        val fontRenderer = event.fontRenderer
        val x = event.x + 13 - fontRenderer.getStringWidth(stackTip)
        val y = event.y + 1

        val scale = 0.9
        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat(), y.toFloat(), 0f)
        GlStateManager.scale(scale, scale, scale)
        fontRenderer.drawStringWithShadow(stackTip, 0f, 0f, 16777215)
        val reverseScale = 1 / 0.7
        GlStateManager.scale(reverseScale, reverseScale, reverseScale)
        GlStateManager.popMatrix()

        GlStateManager.enableLighting()
        GlStateManager.enableDepth()
    }
}