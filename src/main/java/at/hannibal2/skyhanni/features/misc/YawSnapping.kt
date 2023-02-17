package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.util.render.TextRenderUtils
import at.hannibal2.skyhanni.events.YawRotateEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.absoluteValue
import kotlin.math.sign

class YawSnapping {

    fun isEnabled(): Boolean =
        /*LorenzUtils.inSkyBlock && // */
        SkyHanniMod.feature.misc.yawSnapping


    var overshot = 0F
    var isLocking = false

    private val snapPoints
        get() =
            generateSequence(0F) { it + SkyHanniMod.feature.misc.yawIntervals }
                .takeWhile { it <= 360 }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.misc.displayYawOverlay) return
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        val sr = ScaledResolution(Minecraft.getMinecraft())
        GlStateManager.pushMatrix()
        GlStateManager.translate(sr.scaledWidth_double / 2, sr.scaledHeight_double / 2, 0.0)
        Gui.drawRect(-200, -16, 200, -15, 0xFFFFFFFF.toInt())
        Gui.drawRect(-200, 15, 200, 16, 0xFFFFFFFF.toInt())
        Gui.drawRect(0, -16, 1, 16, 0xFFFFFFFF.toInt())
        Gui.drawRect(-200, -16, -199, 16, 0xFFFFFFFF.toInt())
        Gui.drawRect(199, -16, 200, 16, 0xFFFFFFFF.toInt())
        val font = Minecraft.getMinecraft().fontRendererObj
        val player = Minecraft.getMinecraft().thePlayer
        val playerYaw = (player.rotationYaw % 360 + 360) % 360
        fun deltaToPosition(delta: Float) = (if (delta < -180) {
            (360 + delta) % 360
        } else if (delta > 180) {
            -(360 - delta) % 360
        } else delta) * 3

        for (yawBreak in (generateSequence(0F) { it + 15 }.takeWhile { it < 360 } + snapPoints.filter { it < 360 }).toSet()) {
            val delta = yawBreak - playerYaw
            val position = deltaToPosition(delta)
            if (position.absoluteValue < 200) {
                TextRenderUtils.drawStringCentered(
                    "${yawBreak.toInt()}",
                    font,
                    position,
                    -font.FONT_HEIGHT / 2F,
                    false,
                    0xFFFFFFFF.toInt()
                )
                Gui.drawRect(
                    position.toInt(),
                    font.FONT_HEIGHT / 2,
                    position.toInt() + 1,
                    16,
                    if (yawBreak in snapPoints) 0xFF00FF00.toInt() else 0xFFFFFFFF.toInt()
                )
            }
        }
        if (isLocking) {
            val overshotPos = overshot * 200 / SkyHanniMod.feature.misc.yawTightness
            Gui.drawRect(
                overshotPos.toInt(),
                font.FONT_HEIGHT / 2,
                overshotPos.toInt() + 1,
                16,
                0xFFFFFF00.toInt()
            )
        }
        GlStateManager.enableDepth()
        GlStateManager.color(1F, 1F, 1F, 1F)
        GlStateManager.popMatrix()
    }

    @SubscribeEvent
    fun onYawRotate(event: YawRotateEvent) {
        if (!isEnabled()) {
            isLocking = false
            return
        }
        if (isLocking) {
            overshot += event.yawDelta
            if (overshot.absoluteValue > SkyHanniMod.feature.misc.yawTightness) {
                isLocking = false
            } else {
                event.isCanceled = true
            }
            return
        }
        val old = (360 + event.oldYaw % 360) % 360
        val new = old + event.yawDelta
        for (snapPoint in snapPoints) {
            if (sign(old - snapPoint) != sign(new - snapPoint)) {
                Minecraft.getMinecraft().thePlayer.rotationYaw = snapPoint
                Minecraft.getMinecraft().thePlayer.prevRotationYaw = snapPoint
                event.isCanceled = true
                overshot = 0F
                isLocking = true
            }
        }
    }


}