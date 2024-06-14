package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.config.features.skillprogress.SkillProgressBarConfig
import at.hannibal2.skyhanni.data.GuiData
import at.hannibal2.skyhanni.data.HighlightOnHoverSlot
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.features.chroma.ChromaShaderManager
import at.hannibal2.skyhanni.features.chroma.ChromaType
import at.hannibal2.skyhanni.features.misc.DarkenShader
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.CollectionUtils.contains
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.renderOnScreen
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.guide.GuideGUI
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.calculateTableXOffsets
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.calculateTableYOffsets
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned
import at.hannibal2.skyhanni.utils.shader.ShaderManager
import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import java.awt.Color
import java.util.Collections
import kotlin.math.max

interface Renderable {

    val width: Int
    val height: Int

    val horizontalAlign: HorizontalAlignment
    val verticalAlign: VerticalAlignment
    fun isHovered(posX: Int, posY: Int) = currentRenderPassMousePosition?.let { (x, y) ->
        x in (posX..posX + width) && y in (posY..posY + height) // TODO: adjust for variable height?
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

        fun fromAny(any: Any?, itemScale: Double = NEUItems.itemFontSize): Renderable? = when (any) {
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
            highlightsOnHoverSlots: List<Int> = emptyList(),
            condition: () -> Boolean = { true },
        ): Renderable =
            link(string(text), onClick, bypassChecks, highlightsOnHoverSlots = highlightsOnHoverSlots, condition)

        fun link(
            renderable: Renderable,
            onClick: () -> Unit,
            bypassChecks: Boolean = false,
            highlightsOnHoverSlots: List<Int> = emptyList(),
            condition: () -> Boolean = { true },
        ): Renderable {
            return clickable(
                hoverable(
                    underlined(renderable), renderable, bypassChecks,
                    condition = condition,
                    highlightsOnHoverSlots = highlightsOnHoverSlots,
                ),
                onClick,
                0,
                bypassChecks,
                condition,
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
                bypassChecks = bypassChecks,
            )
        }

        fun multiClickAndHover(
            text: Any,
            tips: List<Any>,
            bypassChecks: Boolean = false,
            click: Map<Int, () -> Unit>,
            onHover: () -> Unit = {},
        ): Renderable {
            return multiClickable(
                hoverTips(text, tips, bypassChecks = bypassChecks, onHover = onHover),
                click,
                bypassChecks = bypassChecks,
            )
        }

        fun clickable(
            render: Renderable,
            onClick: () -> Unit,
            button: Int = 0,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ) = object : Renderable {
            override val width = render.width
            override val height = render.height
            override val horizontalAlign = render.horizontalAlign
            override val verticalAlign = render.verticalAlign

            override fun render(posX: Int, posY: Int) {
                if (isHovered(posX, posY) && condition() &&
                    shouldAllowLink(true, bypassChecks) && (button - 100).isKeyClicked()
                ) {
                    onClick()
                }
                render.render(posX, posY)
            }
        }

        fun multiClickable(
            render: Renderable,
            click: Map<Int, () -> Unit>,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
        ) = object : Renderable {
            override val width = render.width
            override val height = render.height
            override val horizontalAlign = render.horizontalAlign
            override val verticalAlign = render.verticalAlign

            override fun render(posX: Int, posY: Int) {
                if (isHovered(posX, posY) && condition() &&
                    shouldAllowLink(true, bypassChecks)
                ) for ((button, onClick) in click) {
                    if ((button - 100).isKeyClicked())
                        onClick()
                }
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

                            RenderableTooltips.setTooltipForRender(
                                tips = tipsRender,
                                stack = stack,
                                borderColor = color,
                                snapsToTopIfToLong = snapsToTopIfToLong,
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
            val guiScreen = Minecraft.getMinecraft().currentScreen

            val isGuiScreen = guiScreen != null
            if (bypassChecks) {
                return isGuiScreen
            }
            val inMenu = Minecraft.getMinecraft().currentScreen !is GuiIngameMenu
            val isGuiPositionEditor = guiScreen !is GuiPositionEditor
            val isNotInSignAndOnSlot = if (guiScreen !is GuiEditSign && guiScreen !is GuideGUI<*>) {
                ToolTipData.lastSlot == null || GuiData.preDrawEventCancelled
            } else true
            val isConfigScreen = guiScreen !is GuiScreenElementWrapper

            val openGui = guiScreen?.javaClass?.name ?: "none"
            val isInNeuPv = openGui == "io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer"
            val neuFocus = NEUItems.neuHasFocus()
            val isInSkyTilsPv = openGui == "gg.skytils.skytilsmod.gui.profile.ProfileGui"

            val result = isGuiScreen && isGuiPositionEditor && inMenu && isNotInSignAndOnSlot && isConfigScreen &&
                !isInNeuPv && !isInSkyTilsPv && !neuFocus

            if (debug) {
                if (!result) {
                    logger.log("")
                    logger.log("blocked link because:")
                    if (!isGuiScreen) logger.log("isGuiScreen")
                    if (!isGuiPositionEditor) logger.log("isGuiPositionEditor")
                    if (!inMenu) logger.log("inMenu")
                    if (!isNotInSignAndOnSlot) logger.log("isNotInSignAndOnSlot")
                    if (!isConfigScreen) logger.log("isConfigScreen")
                    if (isInNeuPv) logger.log("isInNeuPv")
                    if (neuFocus) logger.log("neuFocus")
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
            highlightsOnHoverSlots: List<Int> = emptyList(),
            onHover: () -> Unit = {},
        ) = object : Renderable {
            override val width = max(hovered.width, unhovered.width)
            override val height = max(hovered.height, unhovered.height)
            override val horizontalAlign get() = if (isHovered) hovered.horizontalAlign else unhovered.horizontalAlign
            override val verticalAlign get() = if (isHovered) hovered.verticalAlign else unhovered.verticalAlign

            var isHovered = false

            override fun render(posX: Int, posY: Int) {
                val pair = Pair(posX, posY)
                isHovered = if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks)) {
                    onHover()
                    hovered.render(posX, posY)
                    HighlightOnHoverSlot.currentSlots[pair] = highlightsOnHoverSlots
                    true
                } else {
                    unhovered.render(posX, posY)
                    HighlightOnHoverSlot.currentSlots.remove(pair)
                    false
                }
            }
        }

        /** Bottom Layer must be bigger then the top layer */
        fun doubleLayered(
            bottomLayer: Renderable,
            topLayer: Renderable,
            blockBottomHover: Boolean = true,
        ) = object : Renderable {
            override val width = bottomLayer.width
            override val height = bottomLayer.height
            override val horizontalAlign = bottomLayer.horizontalAlign
            override val verticalAlign = bottomLayer.verticalAlign

            override fun render(posX: Int, posY: Int) {
                val (x, y) = topLayer.renderXYAligned(posX, posY, width, height)
                val (posX, posY) = if (topLayer.isHovered(posX + x, posY + y) && blockBottomHover) {
                    bottomLayer.width + 1 to bottomLayer.height + 1
                } else {
                    posX to posY
                }
                bottomLayer.render(posX, posY)
            }
        }

        fun itemStackWithTip(
            item: ItemStack,
            scale: Double = NEUItems.itemFontSize,
            xSpacing: Int = 2,
            ySpacing: Int = 0,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) =
            hoverTips(
                itemStack(
                    item,
                    scale,
                    xSpacing,
                    ySpacing,
                    rescaleSkulls,
                    horizontalAlign = horizontalAlign,
                    verticalAlign = verticalAlign,
                ),
                item.getTooltip(Minecraft.getMinecraft().thePlayer, false),
                stack = item,
            )

        fun itemStack(
            item: ItemStack,
            scale: Double = NEUItems.itemFontSize,
            xSpacing: Int = 2,
            ySpacing: Int = 1,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        ) = object : Renderable {
            override val width = (15.5 * scale + 0.5).toInt() + xSpacing
            override val height = (15.5 * scale + 0.5).toInt() + ySpacing
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                item.renderOnScreen(xSpacing / 2.0f, 0F, scaleMultiplier = scale, rescaleSkulls)
            }
        }

        fun Renderable.darken(amount: Float = 1.0f) = object : Renderable {
            override val width = this@darken.width
            override val height = this@darken.height
            override val horizontalAlign = this@darken.horizontalAlign
            override val verticalAlign = this@darken.verticalAlign

            override fun render(posX: Int, posY: Int) {
                DarkenShader.darknessLevel = amount
                ShaderManager.enableShader("darken")
                this@darken.render(posX, posY)
                ShaderManager.disableShader()
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
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
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

        fun wrappedString(
            text: String,
            width: Int,
            scale: Double = 1.0,
            color: Color = Color.WHITE,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        ) = object : Renderable {

            val list by lazy {
                Minecraft.getMinecraft().fontRendererObj.listFormattedStringToWidth(
                    text, (width / scale).toInt(),
                )
            }

            override val width by lazy {
                if (list.size == 1) {
                    (Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) * scale).toInt() + 1
                } else {
                    width
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

        fun placeholder(width: Int, height: Int = 10) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP

            override fun render(posX: Int, posY: Int) {
            }
        }

        /**
         * @param content the list of rows the table should render
         */
        fun table(
            content: List<List<Renderable?>>,
            xPadding: Int = 1,
            yPadding: Int = 0,
            useEmptySpace: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            val xOffsets: List<Int> = calculateTableXOffsets(content, xPadding)
            val yOffsets: List<Int> = calculateTableYOffsets(content, yPadding)
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override val width = xOffsets.last() - xPadding
            override val height = yOffsets.last() - yPadding

            val emptySpaceX = if (useEmptySpace) 0 else xPadding
            val emptySpaceY = if (useEmptySpace) 0 else yPadding

            override fun render(posX: Int, posY: Int) {
                for ((rowIndex, row) in content.withIndex()) {
                    for ((index, renderable) in row.withIndex()) {
                        GlStateManager.pushMatrix()
                        GlStateManager.translate(xOffsets[index].toFloat(), yOffsets[rowIndex].toFloat(), 0F)
                        renderable?.renderXYAligned(
                            posX + xOffsets[index],
                            posY + yOffsets[rowIndex],
                            xOffsets[index + 1] - xOffsets[index] - emptySpaceX,
                            yOffsets[rowIndex + 1] - yOffsets[rowIndex] - emptySpaceY,
                        )
                        GlStateManager.popMatrix()
                    }
                }
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

            private val color = if (texture == null) {
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
                        0, 64,
                    ) else Pair(0, 0)

                    Minecraft.getMinecraft().renderEngine.bindTexture(ResourceLocation(texture.path))
                    Minecraft.getMinecraft().ingameGUI.drawTexturedModalRect(
                        posX, posY, textureX, textureY, width, height,
                    )

                    if (useChroma) {
                        ChromaShaderManager.begin(ChromaType.TEXTURED)
                        GlStateManager.color(1f, 1f, 1f, 1f)
                    } else {
                        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
                    }
                    Minecraft.getMinecraft().ingameGUI.drawTexturedModalRect(
                        posX, posY, textureX, textureY + height, progress, height,
                    )

                    if (useChroma) {
                        ChromaShaderManager.end()
                    }
                }
            }
        }

        // TODO use this to render current boosted crop in next jacob contest crops
        fun Renderable.renderBounds(color: Color = LorenzColor.GREEN.toColor().addAlpha(100)) = object : Renderable {
            override val width = this@renderBounds.width
            override val height = this@renderBounds.height
            override val horizontalAlign = this@renderBounds.horizontalAlign
            override val verticalAlign = this@renderBounds.verticalAlign

            override fun render(posX: Int, posY: Int) {
                Gui.drawRect(0, 0, width, height, color.rgb)
                this@renderBounds.render(posX, posY)
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
                render.renderXAligned(posX, posY, width)
            }
        }

        fun fixedSizeLine(
            content: List<Renderable>,
            width: Int,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            val render = content

            override val width = width
            override val height = render.maxOfOrNull { it.height } ?: 0
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            val emptySpace = width - render.sumOf { it.width }
            val spacing = emptySpace / render.size

            override fun render(posX: Int, posY: Int) {
                var xOffset = posX
                render.forEach {
                    val x = it.width + spacing
                    it.renderXYAligned(xOffset, posY, x, height)
                    xOffset += x
                    GlStateManager.translate(x.toFloat(), 0f, 0f)
                }
                GlStateManager.translate(-(xOffset - posX).toFloat(), 0f, 0f)
            }
        }

        fun fixedSizeCollum(
            content: Renderable,
            height: Int,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            val render = content

            override val width = render.width
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign
            override fun render(posX: Int, posY: Int) {
                render.renderYAligned(posX, posY, height)
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
            override val height = renderables.maxOfOrNull { it.height } ?: 0
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                var xOffset = posX
                renderables.forEach {
                    it.renderYAligned(xOffset, posY, height)
                    xOffset += it.width + spacing
                    GlStateManager.translate((it.width + spacing).toFloat(), 0f, 0f)
                }
                GlStateManager.translate(-width.toFloat() - spacing.toFloat(), 0f, 0f)
            }
        }

        fun verticalContainer(
            content: List<Renderable>,
            spacing: Int = 0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            val renderables = content

            override val width = renderables.maxOfOrNull { it.width } ?: 0
            override val height = renderables.sumOf { it.height } + spacing * (renderables.size - 1)
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                var yOffset = posY
                renderables.forEach {
                    it.renderXAligned(posX, yOffset, width)
                    yOffset += it.height + spacing
                    GlStateManager.translate(0f, (it.height + spacing).toFloat(), 0f)
                }
                GlStateManager.translate(0f, -height.toFloat() - spacing.toFloat(), 0f)
            }
        }

        fun scrollList(
            list: List<Renderable>,
            height: Int,
            scrollValue: ScrollValue = ScrollValue(),
            velocity: Double = 2.0,
            button: Int? = null,
            bypassChecks: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            override val width = list.maxOf { it.width }
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val virtualHeight = list.sumOf { it.height }

            private val scroll = ScrollInput.Companion.Vertical(
                scrollValue,
                0,
                virtualHeight - height,
                velocity,
                button,
            )

            private val end get() = scroll.asInt() + height

            override fun render(posX: Int, posY: Int) {
                scroll.update(
                    isHovered(posX, posY) && shouldAllowLink(true, bypassChecks),
                )

                var renderY = 0
                var virtualY = 0
                var found = false
                for (renderable in list) {
                    if ((virtualY..virtualY + renderable.height) in scroll.asInt()..end) {
                        renderable.renderXAligned(posX, posY + renderY, width)
                        GlStateManager.translate(0f, renderable.height.toFloat(), 0f)
                        renderY += renderable.height
                        found = true
                    } else if (found) {
                        found = false
                        if (renderY + renderable.height <= height) {
                            renderable.renderXAligned(posX, posY + renderY, width)
                        }
                        continue
                    }
                    virtualY += renderable.height
                }
                GlStateManager.translate(0f, -renderY.toFloat(), 0f)
            }
        }

        fun scrollTable(
            content: List<List<Renderable?>>,
            height: Int,
            scrollValue: ScrollValue = ScrollValue(),
            velocity: Double = 2.0,
            button: Int? = null,
            xPadding: Int = 1,
            yPadding: Int = 0,
            hasHeader: Boolean = false,
            bypassChecks: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {

            val xOffsets: List<Int> = calculateTableXOffsets(content, xPadding)
            val yOffsets: List<Int> = calculateTableYOffsets(content, yPadding)

            override val width = xOffsets.last() - xPadding
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val virtualHeight = yOffsets.last() - yPadding

            private val end get() = scroll.asInt() + height - yPadding - 1

            private val scroll = ScrollInput.Companion.Vertical(
                scrollValue,
                if (hasHeader) yOffsets[1] else 0,
                virtualHeight - height,
                velocity,
                button,
            )

            override fun render(posX: Int, posY: Int) {
                scroll.update(
                    isHovered(posX, posY) && shouldAllowLink(true, bypassChecks),
                )

                var renderY = 0
                if (hasHeader) {
                    for ((index, renderable) in content[0].withIndex()) {
                        GlStateManager.translate(xOffsets[index].toFloat(), 0f, 0f)
                        renderable?.renderXYAligned(
                            posX + xOffsets[index],
                            posY + renderY,
                            xOffsets[index + 1] - xOffsets[index],
                            yOffsets[1],
                        )
                        GlStateManager.translate(-xOffsets[index].toFloat(), 0f, 0f)
                    }
                    val yShift = yOffsets[1] - yOffsets[0]
                    GlStateManager.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }
                val range =
                    yOffsets.indexOfFirst { it >= scroll.asInt() }..<(yOffsets.indexOfFirst { it >= end }
                        .takeIf { it > 0 }
                        ?: yOffsets.size) - 1

                val range2 =
                    if (range.last + 3 <= yOffsets.size && yOffsets[range.last + 2] - yOffsets[range.first] <= height - renderY) {
                        range.first..range.last() + 1
                    } else {
                        range
                    }

                for (rowIndex in range2) {
                    for ((index, renderable) in content[rowIndex].withIndex()) {
                        GlStateManager.translate(xOffsets[index].toFloat(), 0f, 0f)
                        renderable?.renderXYAligned(
                            posX + xOffsets[index],
                            posY + renderY,
                            xOffsets[index + 1] - xOffsets[index],
                            yOffsets[rowIndex + 1] - yOffsets[rowIndex],
                        )
                        GlStateManager.translate(-xOffsets[index].toFloat(), 0f, 0f)
                    }
                    val yShift = yOffsets[rowIndex + 1] - yOffsets[rowIndex]
                    GlStateManager.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }
                GlStateManager.translate(0f, -renderY.toFloat(), 0f)
            }
        }

        fun drawInsideRoundedRect(
            input: Renderable,
            color: Color,
            padding: Int = 2,
            radius: Int = 10,
            smoothness: Int = 2,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            override val width = input.width + padding * 2
            override val height = input.height + padding * 2
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                RenderUtils.drawRoundRect(0, 0, width, height, color.rgb, radius, smoothness)
                GlStateManager.translate(padding.toFloat(), padding.toFloat(), 0f)
                input.render(posX + padding, posY + padding)
                GlStateManager.translate(-padding.toFloat(), -padding.toFloat(), 0f)
            }
        }

        fun drawInsideRoundedRectWithOutline(
            input: Renderable,
            color: Color,
            padding: Int = 2,
            radius: Int = 10,
            smoothness: Int = 2,
            topOutlineColor: Int,
            bottomOutlineColor: Int,
            borderOutlineThickness: Int,
            blur: Float = 0.7f,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            override val width = input.width + padding * 2
            override val height = input.height + padding * 2
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                RenderUtils.drawRoundRect(0, 0, width, height, color.rgb, radius, smoothness)
                RenderUtils.drawRoundRectOutline(
                    0,
                    0,
                    width,
                    height,
                    topOutlineColor,
                    bottomOutlineColor,
                    borderOutlineThickness,
                    radius,
                    blur,
                )

                GlStateManager.translate(padding.toFloat(), padding.toFloat(), 0f)
                input.render(posX + padding, posY + padding)
                GlStateManager.translate(-padding.toFloat(), -padding.toFloat(), 0f)
            }
        }

        fun fakePlayer(
            player: EntityPlayer,
            followMouse: Boolean = false,
            eyesX: Float = 0f,
            eyesY: Float = 0f,
            width: Int = 50,
            height: Int = 100,
            entityScale: Int = 30,
            padding: Int = 5,
            color: Int? = null,
            colorCondition: () -> Boolean = { true },
        ) = object : Renderable {
            override val width = width + 2 * padding
            override val height = height + 2 * padding
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP
            val playerWidth = entityScale
            val playerHeight = entityScale * 2
            val playerX = width / 2 + padding
            val playerY = height / 2 + playerHeight / 2 + padding

            override fun render(posX: Int, posY: Int) {
                GlStateManager.color(1f, 1f, 1f, 1f)
                if (color != null) RenderLivingEntityHelper.setEntityColor(player, color, colorCondition)
                val mouse = currentRenderPassMousePosition ?: return
                val mouseXRelativeToPlayer =
                    if (followMouse) (posX + playerX - mouse.first).toFloat() else eyesX
                val mouseYRelativeToPlayer =
                    if (followMouse) (posY + playerY - mouse.second - 1.62 * entityScale).toFloat() else eyesY
                GlStateManager.translate(0f, 0f, 100f)
                drawEntityOnScreen(
                    playerX,
                    playerY,
                    entityScale,
                    mouseXRelativeToPlayer,
                    mouseYRelativeToPlayer,
                    player,
                )
                GlStateManager.translate(0f, 0f, -100f)
            }
        }
    }
}
