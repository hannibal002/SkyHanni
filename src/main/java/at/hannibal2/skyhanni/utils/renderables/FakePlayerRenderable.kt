package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
//#if MC > 1.21.5
//$$ import org.joml.Matrix3x2f
//$$ import kotlin.math.atan
//$$ import net.minecraft.client.gui.DrawContext
//$$ import net.minecraft.entity.LivingEntity
//$$ import org.joml.Quaternionf
//$$ import org.joml.Vector3f
//#endif

fun Renderable.Companion.fakePlayer(
    player: EntityPlayer,
    followMouse: Boolean = false,
    eyesX: Float = 0f,
    eyesY: Float = 0f,
    width: Int = 50,
    height: Int = 100,
    entityScale: Int = 30,
    padding: Int = 5,
    color: Color? = null,
    colorCondition: () -> Boolean = { true },
) = object : Renderable {
    override val width = width + 2 * padding
    override val height = height + 2 * padding
    override val horizontalAlign = HorizontalAlignment.LEFT
    override val verticalAlign = VerticalAlignment.TOP
    val playerHeight = entityScale * 2
    val playerX = width / 2 + padding
    val playerY = height / 2 + playerHeight / 2 + padding

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        GlStateManager.color(1f, 1f, 1f, 1f)
        if (color != null) RenderLivingEntityHelper.setEntityColor(player, color, colorCondition)
        val mouse = currentRenderPassMousePosition ?: return
        val (mouseXRelativeToPlayer, mouseYRelativeToPlayer) = if (followMouse) {
            val newOffsetX = (mouseOffsetX + playerX - mouse.first).toFloat()
            val newOffsetY = (mouseOffsetY + playerY - mouse.second - 1.62 * entityScale).toFloat()
            newOffsetX to newOffsetY
        } else eyesX to eyesY
        DrawContextUtils.pushMatrix()
        DrawContextUtils.translate(0f, 0f, 100f)
        //#if MC < 1.21
        GuiInventory.drawEntityOnScreen(
            playerX,
            playerY,
            entityScale,
            mouseXRelativeToPlayer,
            mouseYRelativeToPlayer,
            player,
        )
        //#elseif MC < 1.21.7
        //$$ InventoryScreen.drawEntity(
        //$$     DrawContextUtils.drawContext,
        //$$     padding,
        //$$     padding,
        //$$     padding + width,
        //$$     padding + height,
        //$$     entityScale,
        //$$     0.0625f,
        //$$     if (followMouse) mouse.first - mouseOffsetX.toFloat() else eyesX,
        //$$     if (followMouse) mouse.second - mouseOffsetY.toFloat() else eyesY,
        //$$     player,
        //$$ )
        //#else
        //$$ val peeked = DrawContextUtils.drawContext.matrices.get(Matrix3x2f())
        //$$ val translationX = peeked.m20().toInt()
        //$$ val translationY = peeked.m21().toInt()
        //$$ val averageScale = (peeked.m00() + peeked.m11()) / 2
        //$$ val adjustedPadding = (padding * averageScale).toInt()
        //$$ val adjustedWidth = (width * averageScale).toInt()
        //$$ val adjustedHeight = (height * averageScale).toInt()
        //$$ drawEntityWithoutScissor(
        //$$     DrawContextUtils.drawContext,
        //$$     adjustedPadding + translationX,
        //$$     adjustedPadding + translationY,
        //$$     adjustedPadding + adjustedWidth + translationX,
        //$$     adjustedPadding + adjustedHeight + translationY,
        //$$     (entityScale * averageScale).toInt(),
        //$$     0.0625f * averageScale,
        //$$     if (followMouse) (mouse.first - mouseOffsetX.toFloat()) * averageScale + translationX else eyesX,
        //$$     if (followMouse) (mouse.second - mouseOffsetY.toFloat()) * averageScale + translationY else eyesY,
        //$$     player,
        //$$ )
        //#endif
        DrawContextUtils.popMatrix()
    }
}

//#if MC > 1.21.5
//$$ private fun drawEntityWithoutScissor(
//$$     context: DrawContext,
//$$     x1: Int,
//$$     y1: Int,
//$$     x2: Int,
//$$     y2: Int,
//$$     size: Int,
//$$     scale: Float,
//$$     mouseX: Float,
//$$     mouseY: Float,
//$$     entity: LivingEntity,
//$$ ) {
//$$     val f = (x1 + x2) / 2.0f
//$$     val g = (y1 + y2) / 2.0f
//$$     val h = atan(((f - mouseX) / 40.0f).toDouble()).toFloat()
//$$     val i = atan(((g - mouseY) / 40.0f).toDouble()).toFloat()
//$$     val quaternionf = Quaternionf().rotateZ(Math.PI.toFloat())
//$$     val quaternionf2 = Quaternionf().rotateX(i * 20.0f * (Math.PI / 180.0).toFloat())
//$$     quaternionf.mul(quaternionf2)
//$$     val j = entity.bodyYaw
//$$     val k = entity.yaw
//$$     val l = entity.pitch
//$$     val m = entity.lastHeadYaw
//$$     val n = entity.headYaw
//$$     entity.bodyYaw = 180.0f + h * 20.0f
//$$     entity.yaw = 180.0f + h * 40.0f
//$$     entity.pitch = -i * 20.0f
//$$     entity.headYaw = entity.yaw
//$$     entity.lastHeadYaw = entity.yaw
//$$     val o = entity.scale
//$$     val vector3f = Vector3f(0.0f, entity.height / 2.0f + scale * o, 0.0f)
//$$     val p = size / o
//$$     InventoryScreen.drawEntity(context, x1, y1, x2, y2, p, vector3f, quaternionf, quaternionf2, entity)
//$$     entity.bodyYaw = j
//$$     entity.yaw = k
//$$     entity.pitch = l
//$$     entity.lastHeadYaw = m
//$$     entity.headYaw = n
//$$ }
//#endif
