package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.ItemBlink.checkBlinkItem
import at.hannibal2.skyhanni.utils.ItemUtils.isSkull
import at.hannibal2.skyhanni.utils.NumberUtil.fractionOf
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.RenderCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import java.text.DecimalFormat
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.component3
import kotlin.math.min

//? > 1.21.6 {
/*import kotlin.math.sqrt
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import com.mojang.blaze3d.ProjectionType
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.CachedOrthoProjectionMatrixBuffer
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.item.ItemStackRenderState
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
 *///?}

// todo 1.21 impl needed
/**
 * Some functions taken from NotEnoughUpdates
 */
@Suppress("UnusedParameter")
object GuiRenderUtils {

    private val fr: Font get() = Minecraft.getInstance().font

    private fun drawStringCentered(str: String, x: Float, y: Float, shadow: Boolean, color: Int) {
        val strLen = fr.width(str)
        val x2 = x - strLen / 2f
        val y2 = y - fr.lineHeight / 2f
        DrawContextUtils.drawContext.drawString(fr, str, x2.toInt(), y2.toInt(), color, shadow)
    }

    private fun drawStringCentered(str: Component, x: Float, y: Float, shadow: Boolean, color: Int) {
        val strLen = fr.width(str)
        val x2 = x - strLen / 2f
        val y2 = y - fr.lineHeight / 2f
        DrawContextUtils.drawContext.drawString(fr, str, x2.toInt(), y2.toInt(), color, shadow)
    }

    fun drawStringCentered(str: String, x: Int, y: Int) {
        drawStringCentered(str, x.toFloat(), y.toFloat(), true, -1)
    }
    fun drawStringCentered(str: Component, x: Int, y: Int) {
        drawStringCentered(str, x.toFloat(), y.toFloat(), true, -1)
    }

    fun drawStringCenteredScaledMaxWidth(text: String, x: Float, y: Float, shadow: Boolean, length: Int, color: Int) {
        DrawContextUtils.pushMatrix()
        val strLength = fr.width(text)
        val factor = min((length / strLength.toFloat()).toDouble(), 1.0).toFloat()
        DrawContextUtils.translate(x, y, 0f)
        DrawContextUtils.scale(factor, factor, 1f)
        drawString(text, -strLength / 2, -fr.lineHeight / 2, color, shadow)
        DrawContextUtils.popMatrix()
    }

    fun drawString(str: String, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: String, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x, y, color, shadow)
    }

    fun drawString(str: Component, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: Component, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x, y, color, shadow)
    }

    fun drawString(str: FormattedCharSequence, x: Float, y: Float, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x.toInt(), y.toInt(), color, shadow)
    }

    fun drawString(str: FormattedCharSequence, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        DrawContextUtils.drawContext.drawString(fr, str, x, y, color, shadow)
    }

    fun drawStrings(strings: String, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        drawStrings(strings.split("\n"), x, y, color, shadow)
    }

    fun drawStrings(strings: List<String>, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        var newY = y
        for (string in strings) {
            DrawContextUtils.drawContext.drawString(fr, string, x, newY, color, shadow)
            newY += 9
        }
    }

    fun drawTexts(strings: List<Component>, x: Int, y: Int, color: Int = -1, shadow: Boolean = true) {
        var newY = y
        for (string in strings) {
            DrawContextUtils.drawContext.drawString(fr, string, x, newY, color, shadow)
            newY += 9
        }
    }

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
        return with(Renderable) {
            hoverTips(
                vertical(
                    text(label, scale = scale),
                    fixedSizeLine(
                        listOf(
                            text(
                                "§2${DecimalFormat("0.##").format(current)} / ${
                                    DecimalFormat(
                                        "0.##",
                                    ).format(maxValue)
                                }☘",
                                scale = scale, horizontalAlign = HorizontalAlignment.LEFT,
                            ),
                            text(
                                "§2${(percent * 100).roundTo(1)}%",
                                scale = scale,
                                horizontalAlign = HorizontalAlignment.RIGHT,
                            ),
                        ),
                        width,
                    ),
                    progressBar(percent, width = width),
                ),
                tooltip.split('\n').map(StringRenderable::from),
            )
        }
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

    fun drawGradientRect(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        startColor: Int = -0xfeffff0,
        endColor: Int = -0xfeffff0,
        zLevel: Double = 0.0,
    ) {
        DrawContextUtils.drawContext.fillGradient(left, top, right, bottom, startColor, endColor)
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
        DrawContextUtils.drawContext.blit(RenderCompat.getMinecraftGuiTextured(), texture, x.toInt(), y.toInt(), uMin, vMin, uMax.toInt(), vMax.toInt(), width.toInt(), height.toInt())
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
        val alpha = -0x1000000

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

    // todo, does this actually have to be matching Mojang's projection matrix?
    //  theirs is 1000 -> 11000 by default, but we only use ~20 layers of that,
    //  see if we can adjust this to maybe 100f -> 200f.
    //  if we do change this, the 1.21.6 zT below will need to be adjusted as well.
    //? > 1.21.6 {
    /*private val projectionMatrix by lazy { CachedOrthoProjectionMatrixBuffer("SkyHanni Item Rendering", 1000f, 11000f, true) }
    private val itemRenderStateButCool by lazy { ItemStackRenderState() }
    *///?}

    private const val SKULL_SCALE = (5f / 4f)

    @Suppress("unused")
    fun ItemStack.renderOnScreen(
        x: Float,
        y: Float,
        scaleMultiplier: Double = NeuItems.ITEM_FONT_SIZE,
        rescaleSkulls: Boolean = true,
        rotationDegrees: Vec3? = null,
    ) {
        val item = checkBlinkItem()
        val isItemSkull = rescaleSkulls && item.isSkull()

        val rotX = ((rotationDegrees?.x ?: 0.0) % 360).toFloat()
        val rotY = ((rotationDegrees?.y ?: 0.0) % 360).toFloat()
        val rotZ = ((rotationDegrees?.z ?: 0.0) % 360).toFloat()

        //? > 1.21.6 {
        /*/*Minecraft.getInstance().itemModelResolver.updateForTopItem(itemRenderStateButCool, item, ItemDisplayContext.FIXED, MinecraftCompat.localWorld, MinecraftCompat.localPlayer, 0)
            *///? < 1.21.9 {
            val baseItemScale = if (isItemSkull || itemRenderStateButCool.usesBlockLight()) SKULL_SCALE else 1f
            //?} else {
            /*val baseItemScale = if (isItemSkull) SKULL_SCALE else 1f
            *///?}
        *///?} else {
        val baseItemScale = if (isItemSkull) SKULL_SCALE else 1f
        //?}

        val finalItemScale = (baseItemScale * scaleMultiplier).toFloat()

        val (translateX, translateY) = if (isItemSkull) {
            val skullDiff = ((scaleMultiplier) * 2.5f).toFloat()
            x - skullDiff to y - skullDiff
        } else x to y

        //? if < 1.21.6 {
        val (hx, hy, hz) = listOf(8f, 8f, 148f)
        val (zT, zS) = listOf(-95f, 1f)
        //?}

        //? < 1.21.6 {
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(translateX, translateY, zT)
            DrawContextUtils.scale(finalItemScale, finalItemScale, zS)

            RenderSystem.assertOnRenderThread()
            lateinit var savedMV: Matrix4f

            DrawContextUtils.pushPop {
                DrawContextUtils.loadIdentity()
                DrawContextUtils.translate(hx, hy, hz)

                val (rotXD, rotYD, rotZD) = listOf(rotX, rotY, rotZ).map { it * (Math.PI.toFloat() / 180f) }
                if (rotXD != 0f) DrawContextUtils.rotate(rotXD, 1f, 0f, 0f)
                if (rotYD != 0f) DrawContextUtils.rotate(rotYD, 0f, 1f, 0f)
                if (rotZD != 0f) DrawContextUtils.rotate(rotZD, 0f, 0f, 1f)

                DrawContextUtils.translate(-hx, -hy, -hz)


                savedMV = DrawContextUtils.drawContext.pose().last().pose()

            }
            DrawContextUtils.multMatrix(savedMV)


            RenderSystem.assertOnRenderThread()

            Lighting.setupFor3DItems()

            DrawContextUtils.drawItem(item, 0, 0)

            Lighting.setupForFlatItems()
        }
        //?} else {
        /*val matrices2D = DrawContextUtils.drawContext.pose()

        // And similarly, we need to extract the scaling from the GUI editor as well, since we're building our own stack.
        val guiScaleX = sqrt(matrices2D.m00() * matrices2D.m00() + matrices2D.m01() * matrices2D.m01())
        val guiScaleY = sqrt(matrices2D.m10() * matrices2D.m10() + matrices2D.m11() * matrices2D.m11())
        val totalItemScale = ((guiScaleX + guiScaleY) * 0.5f) * finalItemScale

        if (rotationDegrees != null || (totalItemScale > 1 && itemRenderStateButCool.usesBlockLight())) {
            //? < 1.21.9 {
                 val adjX = matrices2D.m20 + (x * guiScaleX) - (totalItemScale * 1.8f)
                 val adjY = matrices2D.m21 + (y * guiScaleY) - (totalItemScale * 1.8f)

                 item.customRenderOnScreen(adjX, adjY, totalItemScale, rotX, rotY, rotZ)
            //?} else {
                 /*item.normalRenderOnScreen(translateX, translateY, finalItemScale)
            *///?}
         } else {
             item.normalRenderOnScreen(translateX, translateY, finalItemScale)
         }
        *///?}
    }

    // TODO: On 1.21.10+ it is completely broken
    //? > 1.21.6 {
    /*private fun ItemStack.customRenderOnScreen(
         x: Float, y: Float, finalItemScale: Float,
         rotX: Float, rotY: Float, rotZ: Float,
     ) {
         val client = Minecraft.getInstance()
         val window = client.window

         // Thank Vixid for this -  I would have never figured out how to do this.
         RenderSystem.backupProjectionMatrix()
         val guiWidth = window.width.toFloat() / window.guiScale.toFloat()
         val guiHeight = window.height.toFloat() / window.guiScale.toFloat()
         val slice = projectionMatrix.getBuffer(guiWidth, guiHeight)
         RenderSystem.setProjectionMatrix(slice, ProjectionType.ORTHOGRAPHIC)
         RenderSystem.getModelViewStack().pushMatrix()
         RenderSystem.getModelViewStack().identity()
         val textureMatrixBackup = Matrix4f(RenderSystem.getTextureMatrix())
         RenderSystem.resetTextureMatrix()

         // We have to use our own MatrixStack, because the DrawContext matrices are a 2D matrix now
         val matrices = PoseStack()
         matrices.pushPose()
         // TODO -1100f comes from projectionMatrix above, needs changing
         matrices.translate(x, y, -1100f)

         // Because by default the item is rendered flipped in all directions (what the fuck, Mojang?),
         // we need to translate all three ways before rendering the item, so we can flip it, and still
         // have it 'end' in the right position.
         val itemSize = 16f * finalItemScale
         matrices.translate(itemSize, itemSize, 0f)
         // These scales being negative is what does the "flipping back to normal viewing"
         matrices.scale(-finalItemScale, -finalItemScale, -1f)

         // Since we want to rotate the item around its center point, we translate half in, in each direction
         // Matrices are pre-scaled, so we use the raw 8f values
         matrices.translate(8f, 8f, 8f)

         // 'planned' rotations are done now.
         if (rotX != 0f) matrices.mulPose(Axis.XP.rotationDegrees(rotX))
         if (rotY != 0f) matrices.mulPose(Axis.YP.rotationDegrees(rotY))
         if (rotZ != 0f) matrices.mulPose(Axis.ZP.rotationDegrees(rotZ))

         // With the ItemRenderer call, all blocks and skulls are rendered from a true side view, rather than
         // the old "angled down" view. This rotation set re-creates the old view.
         matrices.mulPose(Axis.XN.rotationDegrees(30f))
         matrices.mulPose(Axis.YP.rotationDegrees(45f))

         // We need to scale up before rendering - for some reason the default is 1 x 1 x 1
         matrices.scale(16f, 16f, 16f)

         client.gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_3D)

            //? < 1.21.9 {
                 val consumers = client.renderBuffers().bufferSource()
                 itemRenderStateButCool.render(matrices, consumers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY)
                 consumers.endBatch()
            //?} else {
                 /*val dispatcher = client.gameRenderer.featureRenderDispatcher
                 val consumers = dispatcher.submitNodeStorage
                 itemRenderStateButCool.submit(matrices, consumers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0)
                 dispatcher.endFrame()
            *///?}
         matrices.popPose()
         RenderSystem.teardownOverlayColor()
         RenderSystem.getModelViewStack().popMatrix()
         RenderSystem.getTextureMatrix().set(textureMatrixBackup)
         RenderSystem.restoreProjectionMatrix()
     }

     private fun ItemStack.normalRenderOnScreen(translateX: Float, translateY: Float, scale: Float) {
         DrawContextUtils.pushPop {
             DrawContextUtils.translate(translateX, translateY, 0f)
             DrawContextUtils.scale(scale, scale, 0f)

             RenderSystem.assertOnRenderThread()

             Minecraft.getInstance().gameRenderer.lighting.setupFor(Lighting.Entry.ITEMS_3D)

             DrawContextUtils.drawItem(this, 0, 0)
         }
     }
    *///?}
}
