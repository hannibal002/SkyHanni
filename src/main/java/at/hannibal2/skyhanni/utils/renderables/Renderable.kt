package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.config.features.skillprogress.SkillProgressBarConfig
import at.hannibal2.skyhanni.data.HighlightOnHoverSlot
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.features.chroma.ChromaShaderManager
import at.hannibal2.skyhanni.features.chroma.ChromaType
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned
import io.github.moulberry.moulconfig.gui.GuiScreenElementWrapper
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Mouse
import java.awt.Color
import java.util.Collections
import kotlin.math.max

interface Renderable {

    val width: Int
    val height: Int

    val horizontalAlign: HorizontalAlignment
    val verticalAlign: VerticalAlignment
    fun isHovered(posX: Int, posY: Int) = currentRenderPassMousePosition?.let { (x, y) ->
        x in (posX..posX + width)
            && y in (posY..posY + height) // TODO: adjust for variable height?
    } ?: false

    /**
     * Pos x and pos y are relative to the mouse position.
     * (the GL matrix stack should already be pre transformed)
     */
    fun render(posX: Int, posY: Int)

    companion object {

        val logger = LorenzLogger("debug/renderable")
        var currentRenderPassMousePosition: Pair<Int, Int>? = null
            set

        fun <T> withMousePosition(posX: Int, posY: Int, block: () -> T): T {
            val last = currentRenderPassMousePosition
            try {
                currentRenderPassMousePosition = Pair(posX, posY)
                return block()
            } finally {
                currentRenderPassMousePosition = last
            }
        }

        fun fromAny(any: Any?, itemScale: Double = 1.0): Renderable? = when (any) {
            null -> placeholder(12)
            is Renderable -> any
            is String -> string(any)
            is ItemStack -> itemStack(any, itemScale)
            else -> null
        }

        fun link(text: String, bypassChecks: Boolean = false, onClick: () -> Unit): Renderable =
            link(string(text), onClick, bypassChecks = bypassChecks) { true }

        fun optionalLink(
            text: String,
            onClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ): Renderable =
            link(string(text), onClick, bypassChecks, condition)

        fun link(
            renderable: Renderable,
            onClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ): Renderable {
            return clickable(
                hoverable(underlined(renderable), renderable, bypassChecks, condition = condition),
                onClick,
                0,
                bypassChecks,
                condition
            )
        }

        fun clickAndHover(
            text: Any,
            tips: List<Any>,
            bypassChecks: Boolean = false,
            onClick: () -> Unit,
            onHover: () -> Unit = {},
        ): Renderable {
            return clickable(
                hoverTips(text, tips, bypassChecks = bypassChecks, onHover = onHover),
                onClick,
                bypassChecks = bypassChecks
            )
        }

        fun clickable(
            render: Renderable,
            onClick: () -> Unit,
            button: Int = 0,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ) =
            object : Renderable {
                override val width = render.width
                override val height = render.height
                override val horizontalAlign = render.horizontalAlign
                override val verticalAlign = render.verticalAlign

                private var wasDown = false

                override fun render(posX: Int, posY: Int) {
                    val isDown = Mouse.isButtonDown(button)
                    if (isDown > wasDown && isHovered(posX, posY) && condition() && shouldAllowLink(
                            true,
                            bypassChecks
                        )
                    ) {
                        onClick()
                    }
                    wasDown = isDown
                    render.render(posX, posY)
                }
            }

        fun hoverTips(
            content: Any,
            tips: List<Any>,
            highlightsOnHoverSlots: List<Int> = listOf(),
            stack: ItemStack? = null,
            color: LorenzColor? = null,
            bypassChecks: Boolean = false,
            snapsToTopIfToLong: Boolean = true,
            condition: () -> Boolean = { true },
            onHover: () -> Unit = {},
        ): Renderable {

            val render = fromAny(content) ?: string("Error")
            return object : Renderable {
                override val width = render.width
                override val height = render.height
                override val horizontalAlign = render.horizontalAlign
                override val verticalAlign = render.verticalAlign

                val tipsRender = tips.mapNotNull { fromAny(it) }

                override fun render(posX: Int, posY: Int) {
                    render.render(posX, posY)
                    val pair = Pair(posX, posY)
                    if (isHovered(posX, posY)) {
                        if (condition() && shouldAllowLink(true, bypassChecks)) {
                            onHover.invoke()
                            HighlightOnHoverSlot.currentSlots[pair] = highlightsOnHoverSlots
                            GlStateManager.pushMatrix()
                            GlStateManager.translate(0F, 0F, 400F)

                            RenderLineTooltips.drawHoveringText(
                                posX = posX,
                                posY = posY,
                                tips = tipsRender,
                                stack = stack,
                                borderColor = color,
                                snapsToTopIfToLong = snapsToTopIfToLong,
                                mouseX = currentRenderPassMousePosition?.first ?: Utils.getMouseX(),
                                mouseY = currentRenderPassMousePosition?.second ?: Utils.getMouseY(),
                            )
                            GlStateManager.popMatrix()
                        }
                    } else {
                        HighlightOnHoverSlot.currentSlots.remove(pair)
                    }
                }
            }
        }

        private fun shouldAllowLink(debug: Boolean = false, bypassChecks: Boolean): Boolean {
            val isGuiScreen = Minecraft.getMinecraft().currentScreen != null
            if (bypassChecks) {
                return isGuiScreen
            }
            val isGuiPositionEditor = Minecraft.getMinecraft().currentScreen !is GuiPositionEditor
            val isNotInSignAndOnSlot = if (Minecraft.getMinecraft().currentScreen !is GuiEditSign) {
                ToolTipData.lastSlot == null
            } else true
            val isConfigScreen = Minecraft.getMinecraft().currentScreen !is GuiScreenElementWrapper

            val openGui = Minecraft.getMinecraft().currentScreen?.javaClass?.name ?: "none"
            val isInNeuPv = openGui == "io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer"
            val isInSkyTilsPv = openGui == "gg.skytils.skytilsmod.gui.profile.ProfileGui"

            val result = isGuiScreen && isGuiPositionEditor && isNotInSignAndOnSlot && isConfigScreen &&
                !isInNeuPv && !isInSkyTilsPv

            if (debug) {
                if (!result) {
                    logger.log("")
                    logger.log("blocked link because:")
                    if (!isGuiScreen) logger.log("isGuiScreen")
                    if (!isGuiPositionEditor) logger.log("isGuiPositionEditor")
                    if (!isNotInSignAndOnSlot) logger.log("isNotInSignAndOnSlot")
                    if (!isConfigScreen) logger.log("isConfigScreen")
                    if (isInNeuPv) logger.log("isInNeuPv")
                    if (isInSkyTilsPv) logger.log("isInSkyTilsPv")
                    logger.log("")
                } else {
                    logger.log("allowed click")
                }
            }

            return result
        }

        fun underlined(renderable: Renderable) = object : Renderable {
            override val width: Int
                get() = renderable.width
            override val height = 10
            override val horizontalAlign = renderable.horizontalAlign
            override val verticalAlign = renderable.verticalAlign

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 10, width, 11, 0xFFFFFFFF.toInt())
                GlStateManager.color(1F, 1F, 1F, 1F)
                renderable.render(posX, posY)
            }
        }

        fun hoverable(
            hovered: Renderable,
            unhovered: Renderable,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ) =
            object : Renderable {
                override val width: Int
                    get() = max(hovered.width, unhovered.width)
                override val height = 10
                override val horizontalAlign get() = if (isHovered) hovered.horizontalAlign else unhovered.horizontalAlign
                override val verticalAlign get() = if (isHovered) hovered.verticalAlign else unhovered.verticalAlign

                var isHovered = false

                override fun render(posX: Int, posY: Int) {
                    isHovered = if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks)) {
                        hovered.render(posX, posY)
                        true
                    } else {
                        unhovered.render(posX, posY)
                        false
                    }
                }
            }

        fun itemStack(
            any: ItemStack,
            scale: Double = 1.0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            override val width: Int
                get() = 12
            override val height = 10
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                GlStateManager.pushMatrix()
                any.renderOnScreen(0F, 0F, scaleMultiplier = scale)
                GlStateManager.popMatrix()
            }
        }

        fun singeltonString(string: String): List<Renderable> {
            return Collections.singletonList(string(string))
        }

        fun string(
            text: String,
            scale: Double = 1.0,
            color: Color = Color.WHITE,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {

            override val width by lazy { (Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) * scale).toInt() + 1 }
            override val height = (9 * scale).toInt() + 1
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            val inverseScale = 1 / scale

            override fun render(posX: Int, posY: Int) {
                val fontRenderer = Minecraft.getMinecraft().fontRendererObj
                GlStateManager.translate(1.0, 1.0, 0.0)
                GlStateManager.scale(scale, scale, 1.0)
                fontRenderer.drawStringWithShadow(text, 0f, 0f, color.rgb)
                GlStateManager.scale(inverseScale, inverseScale, 1.0)
                GlStateManager.translate(-1.0, -1.0, 0.0)
            }
        }

        fun placeholder(width: Int, height: Int = 10) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP

            override fun render(posX: Int, posY: Int) {
            }
        }

        fun wrappedString(
            text: String,
            width: Int,
            scale: Double = 1.0,
            color: Color = Color.WHITE,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {

            val list by lazy {
                Minecraft.getMinecraft().fontRendererObj.listFormattedStringToWidth(
                    text, (width / scale).toInt()
                )
            }

            override val width by lazy {
                if (list.size == 1) {
                    (Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) / scale).toInt() + 1
                } else {
                    (width / scale).toInt() + 1
                }
            }

            override val height by lazy { list.size * ((9 * scale).toInt() + 1) }
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            val inverseScale = 1 / scale

            override fun render(posX: Int, posY: Int) {
                val fontRenderer = Minecraft.getMinecraft().fontRendererObj
                GlStateManager.translate(1.0, 1.0, 0.0)
                GlStateManager.scale(scale, scale, 1.0)
                list.forEachIndexed { index, text ->
                    fontRenderer.drawStringWithShadow(text, 0f, index * 10.0f, color.rgb)
                }
                GlStateManager.scale(inverseScale, inverseScale, 1.0)
                GlStateManager.translate(-1.0, -1.0, 0.0)
            }
        }

        fun progressBar(
            percent: Double,
            startColor: Color = Color(255, 0, 0),
            endColor: Color = Color(0, 255, 0),
            useChroma: Boolean = false,
            texture: SkillProgressBarConfig.TexturedBar.UsedTexture? = null,
            width: Int = 182,
            height: Int = 5,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val progress = if (texture == null) {
                (1.0 + percent * (width - 2.0)).toInt()
            } else {
                percent.toInt()
            }

            private var color = if (texture == null) {
                ColorUtils.blendRGB(startColor, endColor, percent)
            } else {
                startColor
            }

            override fun render(posX: Int, posY: Int) {
                if (texture == null) {
                    Gui.drawRect(0, 0, width, height, 0xFF43464B.toInt())

                    if (useChroma) {
                        ChromaShaderManager.begin(ChromaType.STANDARD)
                    }

                    val factor = 0.2
                    val bgColor = if (useChroma) Color.GRAY.darker() else color
                    Gui.drawRect(1, 1, width - 1, height - 1, bgColor.darker(factor).rgb)
                    Gui.drawRect(1, 1, progress, height - 1, color.rgb)

                    if (useChroma) {
                        ChromaShaderManager.end()
                    }
                } else {
                    val (textureX, textureY) = if (texture == SkillProgressBarConfig.TexturedBar.UsedTexture.MATCH_PACK) Pair(
                        0,
                        64
                    ) else Pair(0, 0)

                    Minecraft.getMinecraft().renderEngine.bindTexture(ResourceLocation(texture.path))
                    Minecraft.getMinecraft().ingameGUI.drawTexturedModalRect(
                        posX,
                        posY,
                        textureX,
                        textureY,
                        width,
                        height
                    )

                    if (useChroma) {
                        ChromaShaderManager.begin(ChromaType.TEXTURED)
                        GlStateManager.color(1f, 1f, 1f, 1f)
                    } else {
                        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
                    }
                    Minecraft.getMinecraft().ingameGUI.drawTexturedModalRect(
                        posX,
                        posY,
                        textureX,
                        textureY + height,
                        progress,
                        height
                    )

                    if (useChroma) {
                        ChromaShaderManager.end()
                    }
                }
            }
        }

        fun fixedSizeLine(
            content: Renderable,
            width: Int,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            val render = content

            override val width = width
            override val height = render.height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign
            override fun render(posX: Int, posY: Int) {
                render.renderXAligned(0, 0, width)
            }
        }

        fun horizontalContainer(
            content: List<Renderable>,
            spacing: Int = 0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            val renderables = content

            override val width = renderables.sumOf { it.width } + spacing * (renderables.size - 1)
            override val height = renderables.maxOf { it.height }
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                var xOffset = 0
                renderables.forEach {
                    it.renderYAligned(xOffset, 0, height)
                    xOffset += it.width + spacing
                    GlStateManager.translate(it.width.toFloat(), 0f, 0f)
                }
                GlStateManager.translate(-width.toFloat() - spacing.toFloat(), 0f, 0f)
            }
        }
    }
}
