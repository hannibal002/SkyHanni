package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import java.awt.Color
import org.joml.Matrix3x2f
import kotlin.math.atan
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.LivingEntity
import org.joml.Quaternionf
import org.joml.Vector3f

fun Renderable.Companion.fakePlayer(
    player: FakePlayer,
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

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        if (color != null) RenderLivingEntityHelper.setEntityColor(player, color, colorCondition)
        val mouse = currentRenderPassMousePosition ?: return
        DrawContextUtils.pushMatrix()
        val peeked = DrawContextUtils.drawContext.pose().get(Matrix3x2f())
        val translationX = peeked.m20().toInt()
        val translationY = peeked.m21().toInt()
        val averageScale = (peeked.m00() + peeked.m11()) / 2
        val adjustedPadding = (padding * averageScale).toInt()
        val adjustedWidth = (width * averageScale).toInt()
        val adjustedHeight = (height * averageScale).toInt()
        drawEntityWithoutScissor(
            DrawContextUtils.drawContext,
            adjustedPadding + translationX,
            adjustedPadding + translationY,
            adjustedPadding + adjustedWidth + translationX,
            adjustedPadding + adjustedHeight + translationY,
            (entityScale * averageScale).toInt(),
            0.0625f * averageScale,
            if (followMouse) (mouse.first - mouseOffsetX.toFloat()) * averageScale + translationX else eyesX,
            if (followMouse) (mouse.second - mouseOffsetY.toFloat()) * averageScale + translationY else eyesY,
            player,
        )
        DrawContextUtils.popMatrix()
    }
}

private fun drawEntityWithoutScissor(
    guiGraphics: GuiGraphics,
    x1: Int,
    y1: Int,
    x2: Int,
    y2: Int,
    size: Int,
    scale: Float,
    mouseX: Float,
    mouseY: Float,
    entity: LivingEntity,
) {
    //? if < 1.21.11 {
    val n: Float = (x1 + x2).toFloat() / 2.0f
    val o: Float = (y1 + y2).toFloat() / 2.0f
    val p = atan(((n - mouseX) / 40.0f).toDouble()).toFloat()
    val q = atan(((o - mouseY) / 40.0f).toDouble()).toFloat()
    val quaternionf = Quaternionf().rotateZ(3.1415927f)
    val quaternionf2 = Quaternionf().rotateX(q * 20.0f * 0.017453292f)
    quaternionf.mul(quaternionf2)
    val r: Float = entity.yBodyRot
    val s: Float = entity.getYRot()
    val t: Float = entity.getXRot()
    val u: Float = entity.yHeadRotO
    val v: Float = entity.yHeadRot
    entity.yBodyRot = 180.0f + p * 20.0f
    entity.setYRot(180.0f + p * 40.0f)
    entity.setXRot(-q * 20.0f)
    entity.yHeadRot = entity.getYRot()
    entity.yHeadRotO = entity.getYRot()
    val w: Float = entity.getScale()
    val vector3f = Vector3f(0.0f, entity.getBbHeight() / 2.0f + scale * w, 0.0f)
    val x: Float = size.toFloat() / w
    InventoryScreen.renderEntityInInventory(
        guiGraphics,
        x1,
        y1,
        x2,
        y2,
        x,
        vector3f,
        quaternionf,
        quaternionf2,
        entity
    )
    entity.yBodyRot = r
    entity.setYRot(s)
    entity.setXRot(t)
    entity.yHeadRotO = u
    entity.yHeadRot = v
    //?} else
    //InventoryScreen.renderEntityInInventoryFollowsMouse(guiGraphics, x1, y1, x2, y2, size, scale, mouseX, mouseY, entity)
}
