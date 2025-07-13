package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ColorUtils.component1
import at.hannibal2.skyhanni.utils.ColorUtils.component2
import at.hannibal2.skyhanni.utils.ColorUtils.component3
import at.hannibal2.skyhanni.utils.ColorUtils.component4
import at.hannibal2.skyhanni.utils.ItemBlink.checkBlinkItem
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL14
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.min
//#if MC < 1.21
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.OpenGlHelper
import java.nio.FloatBuffer
//#else
//$$ import at.hannibal2.skyhanni.utils.compat.RenderCompat
//$$ import com.mojang.blaze3d.systems.RenderSystem
//$$ import org.joml.Matrix4f
//$$ import net.minecraft.text.Text
//#endif

// todo 1.21 impl needed
/**
 * Some functions taken from NotEnoughUpdates
 */
@Suppress("UnusedParameter")
object GuiRenderUtils {

    private val fr: FontRenderer get() = Minecraft.getMinecraft().fontRendererObj

    private fun drawStringCentered(str: String, x: Float, y: Float, shadow: Boolean, color: Int) {
        val strLen = fr.getStringWidth(str)
        val x2 = x - strLen / 2f
        val y2 = y - fr.FONT_HEIGHT / 2f
        DrawContextUtils.drawContext.drawText(fr, str, x2.toInt(), y2.toInt(), color, shadow)
    }

    //#if MC > 1.21
    //$$ private fun drawStringCentered(str: Text, x: Float, y: Float, shadow: Boolean, color: Int) {
    //$$     val strLen = fr.getWidth(str)
    //$$     val x2 = x - strLen / 2f
    //$$     val y2 = y - fr.fontHeight / 2f
    //$$     DrawContextUtils.drawContext.drawText(fr, str, x2.toInt(), y2.toInt(), color, shadow)
    //$$ }
    //#endif

    fun drawStringCentered(str: String, x: Int, y: Int) {
        drawStringCentered(str, x.toFloat(), y.toFloat(), true, -1)
    }

    //#if MC > 1.21
    //$$ fun drawStringCentered(str: Text, x: Int, y: Int) {
    //$$     drawStringCentered(str, x.toFloat(), y.toFloat(), true, -1)
    //$$ }
    //#endif

    fun drawStringCenteredScaledMaxWidth(text: String, x: Float, y: Float, shadow: Boolean, length: Int, color: Int) {
        DrawContextUtils.pushMatrix()
        val strLength = fr.getStringWidth(text)
        val factor = min((length / strLength.toFloat()).toDouble(), 1.0).toFloat()
        DrawContextUtils.translate(x, y, 0f)
        DrawContextUtils.scale(factor, factor, 1f)
        drawString(text, -strLength / 2, -fr.FONT_HEIGHT / 2, color, shadow)
        DrawContextUtils.popMatrix()
    }

    fun drawString(str: String, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawText(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: String, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawText(fr, str, x, y, color, shadow)
    }

    //#if MC > 1.21
    //$$ fun drawString(str: Text, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
    //$$     DrawContextUtils.drawContext.drawText(fr, str, x.toInt(), y.toInt(), color, shadow)
    //$$ }
    //$$
    //$$ fun drawString(str: Text, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
    //$$     DrawContextUtils.drawContext.drawText(fr, str, x, y, color, shadow)
    //$$ }
    //#endif

    fun drawStrings(strings: String, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        drawStrings(strings.split("\n"), x, y, color, shadow)
    }

    fun drawStrings(strings: List<String>, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        var newY = y
        for (string in strings) {
            DrawContextUtils.drawContext.drawText(fr, string, x, newY, color, shadow)
            newY += 9
        }
    }

    //#if MC > 1.21
    //$$ fun drawTexts(strings: List<Text>, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
    //$$     var newY = y
    //$$     for (string in strings) {
    //$$         DrawContextUtils.drawContext.drawText(fr, string, x, newY, color, shadow)
    //$$         newY += 9
    //$$     }
    //$$ }
    //#endif

    fun isPointInRect(x: Int, y: Int, left: Int, top: Int, width: Int, height: Int) =
        left <= x && x < left + width && top <= y && y < top + height

    fun getFarmingBar(
        label: String,
        tooltip: String,
        currentValue: Number,
        maxValue: Number,
        width: Int,
        textScale: Float = .7f,
    ): Renderable {
        val current = currentValue.toDouble().coerceAtLeast(0.0)
        val percent = current.fractionOf(maxValue)
        val scale = textScale.toDouble()
        return Renderable.hoverTips(
            VerticalContainerRenderable(
                listOf(
                    StringRenderable(label, scale = scale),
                    Renderable.fixedSizeLine(
                        listOf(
                            StringRenderable(
                                "§2${DecimalFormat("0.##").format(current)} / ${
                                    DecimalFormat(
                                        "0.##",
                                    ).format(maxValue)
                                }☘",
                                scale = scale, horizontalAlign = HorizontalAlignment.LEFT,
                            ),
                            StringRenderable(
                                "§2${(percent * 100).roundTo(1)}%",
                                scale = scale,
                                horizontalAlign = HorizontalAlignment.RIGHT,
                            ),
                        ),
                        width,
                    ),
                    Renderable.progressBar(percent, width = width),
                ),
            ),
            tooltip.split('\n').map(StringRenderable::from),
        )
    }

    fun drawScaledRec(left: Int, top: Int, right: Int, bottom: Int, color: Int, inverseScale: Float) {
        drawRect(
            (left * inverseScale).toInt(),
            (top * inverseScale).toInt(),
            (right * inverseScale).toInt(),
            (bottom * inverseScale).toInt(),
            color,
        )
    }

    fun drawRect(left: Int, top: Int, right: Int, bottom: Int, color: Int) {
        DrawContextUtils.drawContext.fill(left, top, right, bottom, color)
    }

    fun renderItemAndBackground(item: ItemStack, x: Int, y: Int, color: Int) {
        DrawContextUtils.drawItem(item, x, y)
        drawRect(x, y, x + 16, y + 16, color)
    }

    /** @Mojang */
    fun drawGradientRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        startColor: Int = -0xfeffff0,
        endColor: Int = -0xfeffff0,
        zLevel: Double = 0.0,
    ) {
        val (startAlpha, startRed, startGreen, startBlue) = Color(startColor, true)
        val (endAlpha, endRed, endGreen, endBlue) = Color(endColor, true)
        //#if MC < 1.21
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0)
        GlStateManager.shadeModel(7425)
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR)
        worldRenderer.pos(right.toDouble(), top.toDouble(), zLevel)
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldRenderer.pos(left.toDouble(), top.toDouble(), zLevel)
            .color(startRed, startGreen, startBlue, startAlpha).endVertex()
        worldRenderer.pos(left.toDouble(), bottom.toDouble(), zLevel)
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        worldRenderer.pos(right.toDouble(), bottom.toDouble(), zLevel)
            .color(endRed, endGreen, endBlue, endAlpha).endVertex()
        tessellator.draw()
        GlStateManager.shadeModel(7424)
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        //#else
        //$$ DrawContextUtils.drawContext.fillGradient(left, top, right, bottom, startColor, endColor)
        //#endif
    }

    fun drawTexturedRect(x: Float, y: Float, texture: ResourceLocation, alpha: Float = 1f) {
        drawTexturedRect(
            x,
            y,
            GuiScreenUtils.scaledWindowWidth.toFloat(),
            GuiScreenUtils.scaledWindowHeight.toFloat(),
            filter = GL11.GL_NEAREST,
            texture = texture,
            alpha = alpha,
        )
    }

    fun drawTexturedRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        uMin: Float = 0f,
        uMax: Float = 1f,
        vMin: Float = 0f,
        vMax: Float = 1f,
        texture: ResourceLocation,
        alpha: Float = 1f,
        filter: Int = GL11.GL_NEAREST,
    ) {
        drawTexturedRect(
            x.toFloat(),
            y.toFloat(),
            width.toFloat(),
            height.toFloat(),
            uMin,
            uMax,
            vMin,
            vMax,
            texture,
            alpha,
            filter,
        )
    }

    // Taken from NEU
    private fun drawTexturedRect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        uMin: Float = 0f,
        uMax: Float = 1f,
        vMin: Float = 0f,
        vMax: Float = 1f,
        texture: ResourceLocation,
        alpha: Float = 1f,
        filter: Int = GL11.GL_NEAREST,
    ) {
        //#if MC < 1.21
        Minecraft.getMinecraft().textureManager.bindTexture(texture)
        GlStateManager.color(1f, 1f, 1f, alpha)
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter)

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        worldRenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(uMin.toDouble(), vMax.toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(uMax.toDouble(), vMax.toDouble()).endVertex()
        worldRenderer.pos((x + width).toDouble(), y.toDouble(), 0.0).tex(uMax.toDouble(), vMin.toDouble()).endVertex()
        worldRenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex(uMin.toDouble(), vMin.toDouble()).endVertex()
        tessellator.draw()

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
        //#else
        //$$ DrawContextUtils.drawContext.drawTexture(RenderCompat.getMinecraftGuiTextured(), texture, x.toInt(), y.toInt(), uMin, vMin, uMax.toInt(), vMax.toInt(), width.toInt(), height.toInt())
        //#endif
    }

    fun enableScissor(left: Int, top: Int, right: Int, bottom: Int) {
        DrawContextUtils.drawContext.enableScissor(left, top, right, bottom)
    }

    fun disableScissor() {
        DrawContextUtils.drawContext.disableScissor()
    }

    private fun drawFloatingRect(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        light: Int = -0xcfcfca,
        dark: Int = -0xefefea,
        shadow: Boolean = true,
    ) {
        //#if MC < 1.21
        var alpha = -0x10000000

        if (!OpenGlHelper.isFramebufferEnabled()) {
            alpha = -0x1000000
        }
        //#else
        //$$ val alpha = -0x1000000
        //#endif

        val main = alpha or 0x202026
        drawRect(x, y, x + 1, y + height, light) // Left
        drawRect(x + 1, y, x + width, y + 1, light) // Top1
        drawRect(x + width - 1, y + 1, x + width, y + height, dark) // Right
        drawRect(x + 1, y + height - 1, x + width - 1, y + height, dark) // Bottom
        drawRect(x + 1, y + 1, x + width - 1, y + height - 1, main) // Middle
        if (shadow) {
            drawRect(x + width, y + 2, x + width + 2, y + height + 2, 0x70000000) // Right shadow
            drawRect(x + 2, y + height, x + width, y + height + 2, 0x70000000) // Bottom shadow
        }
    }

    fun drawFloatingRectDark(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        shadow: Boolean = true,
    ) {
        drawFloatingRect(
            x,
            y,
            width,
            height,
            -0xcfcfca,
            -0xefefea,
            shadow = shadow,
        )
    }

    fun drawFloatingRectLight(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        shadow: Boolean = true,
    ) {
        drawFloatingRect(
            x,
            y,
            width,
            height,
            light = -0xefefea,
            dark = -0xcfcfca,
            shadow = shadow,
        )
    }


    fun ItemStack.renderOnScreen(
        x: Float,
        y: Float,
        scaleMultiplier: Double = NeuItems.ITEM_FONT_SIZE,
        rescaleSkulls: Boolean = true,
        rotationDegrees: Vec3? = null,
    ) {
        val item = checkBlinkItem()
        val isSkull = rescaleSkulls && item.isSkull()

        val rotX = ((rotationDegrees?.xCoord ?: 0.0) % 360).toFloat()
        val rotY = ((rotationDegrees?.yCoord ?: 0.0) % 360).toFloat()
        val rotZ = ((rotationDegrees?.zCoord ?: 0.0) % 360).toFloat()

        //#if MC < 1.21
        val baseScale = if (isSkull) (4f / 3f) else 1f
        //#else
        //$$ val baseScale = if (isSkull) (5f / 4f) else 1f
        //#endif
        val finalScale = (baseScale * scaleMultiplier).toFloat()

        val (translateX, translateY) = if (isSkull) {
            val skullDiff = ((scaleMultiplier) * 2.5f).toFloat()
            x - skullDiff to y - skullDiff
        } else x to y

        //#if MC < 1.21
        val (hx, hy, hz) = listOf(8f, 8f, 100f)
        val (zT, zS) = listOf(-19f, 0.2f)
        //#else
        //$$ val (hx, hy, hz) = listOf(8f, 8f, 148f)
        //$$ val (zT, zS) = listOf(-95f, 1f)
        //#endif

        DrawContextUtils.pushPop {
            DrawContextUtils.translate(translateX, translateY, zT)
            DrawContextUtils.scale(finalScale, finalScale, zS)

            //#if MC < 1.21
            val savedMV: FloatBuffer = GLAllocation.createDirectFloatBuffer(16)
            //#else
            //$$ RenderSystem.assertOnRenderThread()
            //$$ lateinit var savedMV: Matrix4f
            //#endif

            DrawContextUtils.pushPop {
                DrawContextUtils.loadIdentity()
                DrawContextUtils.translate(hx, hy, hz)

                //#if MC < 1.21
                if (rotX != 0f) DrawContextUtils.rotate(rotX, 1.0, 0.0, 0.0)
                if (rotY != 0f) DrawContextUtils.rotate(rotY, 0.0, 1.0, 0.0)
                if (rotZ != 0f) DrawContextUtils.rotate(rotZ, 0.0, 0.0, 1.0)
                //#else
                //$$ val (rotXD, rotYD, rotZD) = listOf(rotX, rotY, rotZ).map { it * (Math.PI.toFloat() / 180f) }
                //$$ if (rotXD != 0f) DrawContextUtils.rotate(rotXD, 1f, 0f, 0f)
                //$$ if (rotYD != 0f) DrawContextUtils.rotate(rotYD, 0f, 1f, 0f)
                //$$ if (rotZD != 0f) DrawContextUtils.rotate(rotZD, 0f, 0f, 1f)
                //#endif

                DrawContextUtils.translate(-hx, -hy, -hz)

                //#if MC < 1.21
                GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, savedMV)
                //#else
                //$$ savedMV = DrawContextUtils.drawContext.matrices.peek().getPositionMatrix()
                //#endif
            }
            DrawContextUtils.multMatrix(savedMV)

            //#if MC < 1.21
            GL11.glEnable(GL11.GL_NORMALIZE)
            GL11.glNormal3f(0f, 0f, 1f)
            //#else
            //$$ RenderSystem.assertOnRenderThread()
            //#endif

            RenderHelper.enableGUIStandardItemLighting()

            //#if MC < 1.21
            AdjustStandardItemLighting.adjust() // Compensate for z scaling
            //#endif

            DrawContextUtils.drawItem(item, 0, 0)

            //#if MC < 1.21
            RenderHelper.disableStandardItemLighting()
            //#else
            //$$ DiffuseLighting.disableGuiDepthLighting()
            //#endif

            //#if MC < 1.21
            GL11.glDisable(GL11.GL_NORMALIZE)
            //#endif
        }
    }

    //#if MC < 1.21
    private object AdjustStandardItemLighting {

        private const val lightScaling = 2.47f // Adjust as needed
        private const val g = 0.6f // Original Value taken from RenderHelper
        private const val lightIntensity = lightScaling * g
        private val itemLightBuffer = GLAllocation.createDirectFloatBuffer(16)

        init {
            itemLightBuffer.clear()
            itemLightBuffer.put(lightIntensity).put(lightIntensity).put(lightIntensity).put(1f)
            itemLightBuffer.flip()
        }

        fun adjust() {
            GL11.glLight(16384, 4609, itemLightBuffer)
            GL11.glLight(16385, 4609, itemLightBuffer)
        }
    }
    //#endif
}
