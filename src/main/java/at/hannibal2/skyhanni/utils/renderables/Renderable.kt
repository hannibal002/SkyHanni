package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.config.core.config.gui.GuiPositionEditor
import at.hannibal2.skyhanni.config.features.skillprogress.SkillProgressBarConfig
//#if TODO
import at.hannibal2.skyhanni.data.GuiData
//#endif
import at.hannibal2.skyhanni.data.HighlightOnHoverSlot
import at.hannibal2.skyhanni.data.RenderData
import at.hannibal2.skyhanni.data.ToolTipData
import at.hannibal2.skyhanni.data.model.TextInput
//#if TODO
import at.hannibal2.skyhanni.features.chroma.ChromaShaderManager
import at.hannibal2.skyhanni.features.chroma.ChromaType
import at.hannibal2.skyhanni.features.misc.DarkenShader
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
//#endif
import at.hannibal2.skyhanni.utils.ColorUtils
import at.hannibal2.skyhanni.utils.ColorUtils.addAlpha
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.KeyboardManager.LEFT_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.RIGHT_MOUSE
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.contains
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.firstTwiceOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.runningIndexedFold
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sumAllValues
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.compat.createResourceLocation
import at.hannibal2.skyhanni.utils.guide.GuideGui
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable
import at.hannibal2.skyhanni.utils.renderables.item.ItemStackRenderable
//#if TODO
import at.hannibal2.skyhanni.utils.shader.ShaderManager
//#endif
import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiIngameMenu
import net.minecraft.client.gui.inventory.GuiEditSign
//#if TODO
import net.minecraft.client.gui.inventory.GuiInventory.drawEntityOnScreen
//#endif
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max

// todo 1.21 impl needed
@Suppress("TooManyFunctions")
interface Renderable {

    val width: Int
    val height: Int

    val horizontalAlign: HorizontalAlignment
    val verticalAlign: VerticalAlignment
    fun isHovered(posX: Int, posY: Int) = currentRenderPassMousePosition?.let { (x, y) ->
        x in (posX..posX + width) && y in (posY..posY + height)
    } ?: false

    fun isBoxHovered(posX: Int, width: Int, posY: Int, height: Int) = currentRenderPassMousePosition?.let { (x, y) ->
        x in (posX..posX + width) && y in (posY..posY + height)
    } ?: false

    /**
     * Pos x and pos y are relative to the mouse position.
     * (the GL matrix stack should already be pre transformed)
     */
    fun render(posX: Int, posY: Int)

    companion object {

        val logger = LorenzLogger("debug/renderable")
        var currentRenderPassMousePosition: Pair<Int, Int>? = null

        fun <T> withMousePosition(posX: Int, posY: Int, block: () -> T): T {
            val last = currentRenderPassMousePosition
            try {
                currentRenderPassMousePosition = Pair(posX, posY)
                return block()
            } finally {
                currentRenderPassMousePosition = last
            }
        }

        fun fromAny(any: Any?, itemScale: Double = NeuItems.ITEM_FONT_SIZE): Renderable? = when (any) {
            null -> placeholder(12)
            is Renderable -> any
            is String -> RenderableString(any)
            is ItemStack -> ItemStackRenderable(any, itemScale)
            else -> null
        }

        fun link(text: String, bypassChecks: Boolean = false, onLeftClick: () -> Unit): Renderable =
            link(RenderableString(text), onLeftClick, bypassChecks = bypassChecks)

        fun optionalLink(
            text: String,
            onLeftClick: () -> Unit,
            bypassChecks: Boolean = false,
            highlightsOnHoverSlots: List<Int> = emptyList(),
            condition: () -> Boolean = { true },
        ): Renderable = link(
            RenderableString(text),
            onLeftClick,
            bypassChecks,
            highlightsOnHoverSlots = highlightsOnHoverSlots,
            condition,
        )

        fun link(
            renderable: Renderable,
            onLeftClick: () -> Unit,
            bypassChecks: Boolean = false,
            highlightsOnHoverSlots: List<Int> = emptyList(),
            condition: () -> Boolean = { true },
            underlineColor: Color = Color.WHITE,
        ): Renderable = clickable(
            hoverable(
                underlined(renderable, underlineColor), renderable, bypassChecks,
                condition = condition,
                highlightsOnHoverSlots = highlightsOnHoverSlots,
            ),
            onLeftClick,
            bypassChecks,
            condition,
        )

        fun clickable(
            text: String,
            onLeftClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            tips: List<Any>? = null,
            onHover: () -> Unit = {},
        ) = clickable(RenderableString(text), onLeftClick, bypassChecks, condition, tips, onHover)

        fun clickable(
            render: Renderable,
            onLeftClick: () -> Unit,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            tips: List<Any>? = null,
            onHover: () -> Unit = {},
        ) = clickable(render, mapOf(LEFT_MOUSE to onLeftClick), bypassChecks, condition, tips, onHover)

        fun clickable(
            text: String,
            /**
             * This should be a direct map of key code int, to the unit that should be invoked.
             * For mouse buttons, use [LEFT_MOUSE] and [RIGHT_MOUSE] from [at.hannibal2.skyhanni.utils.KeyboardManager].
             * For keyboard codes, use the [org.lwjgl.input.Keyboard] enums.
             */
            onAnyClick: Map<Int, () -> Unit>,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            tips: List<Any>? = null,
            onHover: () -> Unit = {},
        ) = clickable(RenderableString(text), onAnyClick, bypassChecks, condition, tips, onHover)

        fun clickable(
            render: Renderable,
            /**
             * This should be a direct map of key code int, to the unit that should be invoked.
             * For mouse buttons, use [LEFT_MOUSE] and [RIGHT_MOUSE] from [at.hannibal2.skyhanni.utils.KeyboardManager].
             * For keyboard codes, use the [org.lwjgl.input.Keyboard] enums.
             */
            onAnyClick: Map<Int, () -> Unit>,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            tips: List<Any>? = null,
            onHover: () -> Unit = {},
        ) = multiClickable(
            tips?.let {
                hoverTips(render, it, bypassChecks = bypassChecks, onHover = onHover)
            } ?: onHover.takeIf { it != {} }?.let {
                hoverable(render, render, bypassChecks = bypassChecks, onHover = onHover)
            } ?: render,
            onAnyClick,
            bypassChecks,
            condition,
        )

        private fun multiClickable(
            render: Renderable,
            onAnyClick: Map<Int, () -> Unit>,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            /**
             * This unit is invoked on 'hover & click' if no keys within [onAnyClick] invoke their unit.
             * This is useful for detecting things like scrolling, which do not have a direct key code to reference.
             *
             * See [clickableAndScrollable] for an example of how this is used.
             */
            nonStandardClick: () -> Unit = {},
        ) = object : Renderable {
            override val width = render.width
            override val height = render.height
            override val horizontalAlign = render.horizontalAlign
            override val verticalAlign = render.verticalAlign

            override fun render(posX: Int, posY: Int) {
                if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks)) {
                    handleClickChecks()
                }
                render.render(posX, posY)
            }

            private fun handleClickChecks() {
                var processed = false
                for ((key, onKeyClicked) in onAnyClick) {
                    if (key.isKeyClicked()) {
                        onKeyClicked()
                        processed = true
                    }
                }
                if (!processed) nonStandardClick()
            }
        }

        fun clickableAndScrollable(
            render: Renderable,
            onAnyClick: Map<Int, () -> Unit>,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            scrollValue: ScrollValue = ScrollValue(),
        ): Renderable {
            val pureScrollInput = ScrollInput.Companion.PureVertical(scrollValue)

            return multiClickable(
                render = render,
                onAnyClick = onAnyClick,
                bypassChecks = bypassChecks,
                condition = condition,
                nonStandardClick = {
                    pureScrollInput.update(true)
                    when (pureScrollInput.asDirection()) {
                        ScrollInput.ScrollDirection.UP -> onAnyClick[RIGHT_MOUSE]?.invoke()
                        ScrollInput.ScrollDirection.DOWN -> onAnyClick[LEFT_MOUSE]?.invoke()
                        else -> {}
                    }
                    pureScrollInput.dispose()
                },
            )
        }

        fun hoverTips(
            content: Any,
            tips: List<Any>,
            highlightsOnHoverSlots: List<Int> = listOf(),
            stack: ItemStack? = null,
            color: LorenzColor? = null,
            spacedTitle: Boolean = false,
            bypassChecks: Boolean = false,
            snapsToTopIfToLong: Boolean = true,
            condition: () -> Boolean = { true },
            onHover: () -> Unit = {},
        ): Renderable {

            val render = fromAny(content) ?: RenderableString("Error")
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
                            DrawContextUtils.pushMatrix()
                            DrawContextUtils.translate(0F, 0F, 400F)

                            RenderableTooltips.setTooltipForRender(
                                tips = tipsRender,
                                stack = stack,
                                borderColor = color,
                                snapsToTopIfToLong = snapsToTopIfToLong,
                                spacedTitle = spacedTitle,
                            )
                            DrawContextUtils.popMatrix()
                        }
                    } else {
                        HighlightOnHoverSlot.currentSlots.remove(pair)
                    }
                }
            }
        }

        internal fun shouldAllowLink(debug: Boolean = false, bypassChecks: Boolean): Boolean {
            val guiScreen = Minecraft.getMinecraft().currentScreen.takeIf { it != null } ?: return false

            // Never support grayed out inventories
            if (RenderData.outsideInventory) return false

            if (bypassChecks) return true

            val inMenu = Minecraft.getMinecraft().currentScreen !is GuiIngameMenu
            val isGuiPositionEditor = guiScreen !is GuiPositionEditor
            val isNotInSignAndOnSlot = if (guiScreen !is GuiEditSign && guiScreen !is GuideGui<*>) {
                ToolTipData.lastSlot == null
                    //#if TODO
                    || GuiData.preDrawEventCancelled
                //#endif
            } else true
            val isConfigScreen = guiScreen !is GuiScreenElementWrapper

            val openGui = guiScreen.javaClass.name ?: "none"
            val isInNeuPv = openGui == "io.github.moulberry.notenoughupdates.profileviewer.GuiProfileViewer"
            val neuFocus = NeuItems.neuHasFocus()
            val isInSkytilsPv = openGui == "gg.skytils.skytilsmod.gui.profile.ProfileGui"
            val isInSkytilsSettings =
                openGui.let { it.startsWith("gg.skytils.vigilance.gui.") || it.startsWith("gg.skytils.skytilsmod.gui.") }
            val isInNeuSettings = openGui.startsWith("io.github.moulberry.notenoughupdates.")

            val result =
                isGuiPositionEditor &&
                    inMenu &&
                    isNotInSignAndOnSlot &&
                    isConfigScreen &&
                    !isInNeuPv &&
                    !isInSkytilsPv &&
                    !neuFocus &&
                    !isInSkytilsSettings &&
                    !isInNeuSettings

            if (debug) {
                if (!result) {
                    logger.log("")
                    logger.log("blocked link because:")
                    if (!isGuiPositionEditor) logger.log("isGuiPositionEditor")
                    if (!inMenu) logger.log("inMenu")
                    if (!isNotInSignAndOnSlot) logger.log("isNotInSignAndOnSlot")
                    if (!isConfigScreen) logger.log("isConfigScreen")
                    if (isInNeuPv) logger.log("isInNeuPv")
                    if (neuFocus) logger.log("neuFocus")
                    if (isInSkytilsPv) logger.log("isInSkytilsPv")
                    if (isInSkytilsSettings) logger.log("isInSkytilsSettings")
                    if (isInNeuSettings) logger.log("isInNeuSettings")
                    logger.log("")
                } else {
                    logger.log("allowed click")
                }
            }

            return result
        }

        fun underlined(renderable: Renderable, color: Color = Color.WHITE) = object : Renderable {
            override val width: Int
                get() = renderable.width
            override val height: Int
                get() = renderable.height + 1
            override val horizontalAlign = renderable.horizontalAlign
            override val verticalAlign = renderable.verticalAlign

            override fun render(posX: Int, posY: Int) {
                GuiRenderUtils.drawRect(0, height, width, 11, color.rgb)
                GlStateManager.color(1F, 1F, 1F, 1F)
                renderable.render(posX, posY)
            }
        }

        fun hoverable(
            hovered: Renderable,
            unHovered: Renderable,
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            highlightsOnHoverSlots: List<Int> = emptyList(),
            onHover: () -> Unit = {},
        ) = object : Renderable {
            override val width = max(hovered.width, unHovered.width)
            override val height = max(hovered.height, unHovered.height)
            override val horizontalAlign get() = if (isHovered) hovered.horizontalAlign else unHovered.horizontalAlign
            override val verticalAlign get() = if (isHovered) hovered.verticalAlign else unHovered.verticalAlign

            var isHovered = false

            override fun render(posX: Int, posY: Int) {
                val pair = Pair(posX, posY)
                isHovered = if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks)) {
                    onHover()
                    hovered.render(posX, posY)
                    HighlightOnHoverSlot.currentSlots[pair] = highlightsOnHoverSlots
                    true
                } else {
                    unHovered.render(posX, posY)
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
                val (nPosX, nPosY) = if (topLayer.isHovered(posX + x, posY + y) && blockBottomHover) {
                    bottomLayer.width + 1 to bottomLayer.height + 1
                } else {
                    posX to posY
                }
                bottomLayer.render(nPosX, nPosY)
            }
        }

        @Deprecated(
            "Use ItemStackRenderable instead",
            ReplaceWith("ItemStackRenderable(item, scale, xSpacing, ySpacing, rescaleSkulls, horizontalAlign, verticalAlign)"),
        )
        fun itemStack(
            item: ItemStack,
            scale: Double = NeuItems.ITEM_FONT_SIZE,
            xSpacing: Int = 2,
            ySpacing: Int = 1,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
            highlight: Boolean = false,
        ) = object : Renderable {
            override val width = (15.5 * scale + 0.5).toInt() + xSpacing
            override val height = (15.5 * scale + 0.5).toInt() + ySpacing
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                if (highlight) {
                    item.addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 1)
                }
                item.renderOnScreen(xSpacing / 2f, 0F, scaleMultiplier = scale, rescaleSkulls)
            }
        }

        //#if TODO
        fun Renderable.darken(amount: Float = 1f) = object : Renderable {
            override val width = this@darken.width
            override val height = this@darken.height
            override val horizontalAlign = this@darken.horizontalAlign
            override val verticalAlign = this@darken.verticalAlign

            override fun render(posX: Int, posY: Int) {
                DarkenShader.darknessLevel = amount
                ShaderManager.enableShader(ShaderManager.Shaders.DARKEN)
                this@darken.render(posX, posY)
                ShaderManager.disableShader()
            }
        }
        //#endif

        @Deprecated(
            "Use RenderableString instead",
            ReplaceWith("RenderableString(text, scale, color, horizontalAlign, verticalAlign)"),
        )
        fun string(
            text: String,
            scale: Double = 1.0,
            color: Color = Color.WHITE,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        ) = RenderableString(
            text,
            scale,
            color,
            horizontalAlign,
            verticalAlign,
        )

        @Deprecated(
            "use WrappedRenderableString instead",
            ReplaceWith("WrappedRenderableString(text, width, scale, color, horizontalAlign, verticalAlign)"),
        )
        fun wrappedString(
            text: String,
            width: Int,
            scale: Double = 1.0,
            color: Color = Color.WHITE,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
            internalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
        ) = WrappedRenderableString(
            text,
            width,
            scale,
            color,
            horizontalAlign,
            verticalAlign,
            internalAlign,
        )

        fun placeholder(width: Int, height: Int = 10) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = HorizontalAlignment.LEFT
            override val verticalAlign = VerticalAlignment.TOP

            override fun render(posX: Int, posY: Int) {}
        }

        fun searchableTable(
            content: Map<List<Renderable>, String>,
            textInput: TextInput,
            key: Int,
            xPadding: Int = 1,
            yPadding: Int = 0,
            header: List<Renderable> = emptyList(),
            useEmptySpace: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            var list = filterListMap(content, textInput.textBox)
            private val fullContent = if (header.isNotEmpty()) listOf(header) + content.keys else content.keys
            val xOffsets = RenderableUtils.calculateTableX(fullContent, xPadding)
            val yOffsets = RenderableUtils.calculateTableY(fullContent, yPadding)
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override val width = xOffsets.sum()
            override val height = yOffsets.sumAllValues().toInt()

            val emptySpaceX = if (useEmptySpace) 0 else xPadding
            val emptySpaceY = if (useEmptySpace) 0 else yPadding

            init {
                textInput.registerToEvent(key) {
                    list = filterListMap(content, textInput.textBox)
                }
            }

            @Suppress("NOTHING_TO_INLINE")
            inline fun renderRow(posX: Int, posY: Int, row: List<Renderable>, renderY: Int): Int {
                var renderX = 0
                val yShift = yOffsets[row] ?: row.firstOrNull()?.height ?: 0
                for ((index, renderable) in row.withIndex()) {
                    val xShift = xOffsets[index]
                    renderable.renderXYAligned(
                        posX + renderX,
                        posY + renderY,
                        xShift - emptySpaceX,
                        yShift - emptySpaceY,
                    )
                    DrawContextUtils.translate(xShift.toFloat(), 0f, 0f)
                    renderX += xShift
                }
                DrawContextUtils.translate(-renderX.toFloat(), yShift.toFloat(), 0f)
                return renderY + yShift
            }

            override fun render(posX: Int, posY: Int) {
                var renderY = 0
                if (header.isNotEmpty()) {
                    renderY = renderRow(posX, posY, header, renderY)
                }
                for (row in list) {
                    renderY = renderRow(posX, posY, row, renderY)
                }
                DrawContextUtils.translate(0f, -renderY.toFloat(), 0f)
            }
        }

        /**
         * @param content the list of rows the table should render
         */
        fun table(
            content: List<List<Renderable>>,
            xPadding: Int = 1,
            yPadding: Int = 0,
            useEmptySpace: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            val xOffsets: List<Int> = RenderableUtils.calculateTableXOffsets(content, xPadding)
            val yOffsets: List<Int> = RenderableUtils.calculateTableYOffsets(content, yPadding)
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override val width = xOffsets.last() - xPadding
            override val height = yOffsets.last() - yPadding

            val emptySpaceX = if (useEmptySpace) 0 else xPadding
            val emptySpaceY = if (useEmptySpace) 0 else yPadding

            override fun render(posX: Int, posY: Int) {
                for ((rowIndex, row) in content.withIndex()) {
                    for ((index, renderable) in row.withIndex()) {
                        DrawContextUtils.pushMatrix()
                        DrawContextUtils.translate(xOffsets[index].toFloat(), yOffsets[rowIndex].toFloat(), 0F)
                        renderable.renderXYAligned(
                            posX + xOffsets[index],
                            posY + yOffsets[rowIndex],
                            xOffsets[index + 1] - xOffsets[index] - emptySpaceX,
                            yOffsets[rowIndex + 1] - yOffsets[rowIndex] - emptySpaceY,
                        )
                        DrawContextUtils.popMatrix()
                    }
                }
            }
        }

        /**
         * @param searchPrefix text that is static in front of the textbox
         * @param onUpdateSize function that is called if the size changes (since the search text can get bigger than [content])
         * @param textInput The text input, can be external or internal
         * @param shouldRenderTopElseBottom true == Renders on top, false == Renders at the Bottom
         * @param hideIfNoText hides text box if no input is given
         * @param ySpacing space between the search and [content]
         * @param onHover is triggered if [content] or the text box is hovered
         * @param bypassChecks bypass the [shouldAllowLink] logic
         * @param condition condition to being able to input / [onHover] to trigger
         * @param scale text scale of the textbox
         * @param color color of the textbox
         * @param key event key for the [textInput] to register the event, needs clearing if [textInput] is external, default = 0
         */
        fun searchBox(
            content: Renderable,
            searchPrefix: String,
            onUpdateSize: (Renderable) -> Unit,
            textInput: TextInput = TextInput(),
            shouldRenderTopElseBottom: Boolean = true,
            hideIfNoText: Boolean = true,
            ySpacing: Int = 0,
            onHover: (TextInput) -> Unit = {},
            bypassChecks: Boolean = false,
            condition: () -> Boolean = { true },
            scale: Double = 1.0,
            color: Color = Color.WHITE,
            key: Int = 0,
        ) = object : Renderable {
            val textBoxHeight = (9 * scale).toInt() + 1

            val isTextBoxEmpty get() = textInput.textBox.isEmpty()

            override var width: Int = content.width
            override var height: Int = content.height + if (hideIfNoText && isTextBoxEmpty) 0 else (ySpacing + textBoxHeight)
            override val horizontalAlign = content.horizontalAlign
            override val verticalAlign = content.verticalAlign

            val searchWidth: Int
                get() {
                    val fontRenderer = Minecraft.getMinecraft().fontRendererObj
                    val string = searchPrefix + textInput.editTextWithAlwaysCarriage()
                    return (fontRenderer.getStringWidth(string) * scale).toInt() + 1
                }

            init {
                textInput.registerToEvent(key) {
                    var shouldUpdate = false
                    if (hideIfNoText) {
                        if (isTextBoxEmpty) {
                            if (height != content.height) {
                                height = content.height
                                shouldUpdate = true
                            }
                        } else {
                            if (height == content.height) {
                                height = content.height + ySpacing + textBoxHeight
                                shouldUpdate = true
                            }
                        }
                    }
                    val searchWidth = searchWidth
                    if (searchWidth > width) {
                        width = searchWidth
                        shouldUpdate = true
                    } else {
                        if (width > content.width) {
                            width = maxOf(content.width, searchWidth)
                            shouldUpdate = true
                        }
                    }
                    if (shouldUpdate) {
                        onUpdateSize(this)
                    }
                }
            }

            override fun render(posX: Int, posY: Int) {
                if (shouldRenderTopElseBottom && !(hideIfNoText && isTextBoxEmpty)) {
                    RenderableUtils.renderString(searchPrefix + textInput.editText(), scale, color)
                    DrawContextUtils.translate(0f, (ySpacing + textBoxHeight).toFloat(), 0f)
                }
                if (isHovered(posX, posY) && condition() && shouldAllowLink(true, bypassChecks)) {
                    onHover(textInput)
                    textInput.makeActive()
                    textInput.handle()
                    val yOff: Int = if (shouldRenderTopElseBottom) 0 else content.height + ySpacing
                    if (isBoxHovered(posX, width, posY + yOff, textBoxHeight) && RIGHT_MOUSE.isKeyClicked()) {
                        textInput.clear()
                    }
                } else {
                    textInput.disable()
                }
                if (hideIfNoText && isTextBoxEmpty) {
                    content.render(posX, posY)
                } else if (!shouldRenderTopElseBottom) {
                    content.render(posX, posY)
                    DrawContextUtils.translate(0f, (ySpacing).toFloat(), 0f)
                    if (!(hideIfNoText && textInput.textBox.isEmpty())) {
                        RenderableUtils.renderString(searchPrefix + textInput.editText(), scale, color)
                    }
                    DrawContextUtils.translate(0f, -(ySpacing).toFloat(), 0f)
                } else {
                    content.render(posX, posY + textBoxHeight + ySpacing)
                    DrawContextUtils.translate(0f, -(ySpacing + textBoxHeight).toFloat(), 0f)
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
                    GuiRenderUtils.drawRect(0, 0, width, height, 0xFF43464B.toInt())

                    if (useChroma) {
                        //#if TODO
                        ChromaShaderManager.begin(ChromaType.STANDARD)
                        //#endif
                    }

                    val factor = 0.2
                    val bgColor = if (useChroma) Color.GRAY.darker() else color
                    GuiRenderUtils.drawRect(1, 1, width - 1, height - 1, bgColor.darker(factor).rgb)
                    GuiRenderUtils.drawRect(1, 1, progress, height - 1, color.rgb)

                    if (useChroma) {
                        //#if TODO
                        ChromaShaderManager.end()
                        //#endif
                    }
                } else {
                    val (textureX, textureY) = if (texture == SkillProgressBarConfig.TexturedBar.UsedTexture.MATCH_PACK) Pair(
                        0, 64,
                    ) else Pair(0, 0)

                    GuiRenderUtils.drawTexturedRect(
                        posX, posY, width, height, textureX.toFloat(), textureY.toFloat(),
                        (textureX + width).toFloat(), (textureY + height).toFloat(), createResourceLocation(texture.path),
                        alpha = 1f, filter = GL11.GL_NEAREST
                    )

                    if (useChroma) {
                        //#if TODO
                        ChromaShaderManager.begin(ChromaType.TEXTURED)
                        //#endif
                        GlStateManager.color(1f, 1f, 1f, 1f)
                    } else {
                        GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
                    }
                    GuiRenderUtils.drawTexturedRect(
                        posX, posY, progress, height, textureX.toFloat(), (textureY + height).toFloat(),
                        (textureX + progress).toFloat(), (textureY + height + height).toFloat(), createResourceLocation(texture.path),
                        alpha = 1f, filter = GL11.GL_NEAREST
                    )

                    if (useChroma) {
                        //#if TODO
                        ChromaShaderManager.end()
                        //#endif
                    }
                }
            }
        }

        fun Renderable.renderBounds(color: Color = LorenzColor.GREEN.toColor().addAlpha(100)) = object : Renderable {
            override val width = this@renderBounds.width
            override val height = this@renderBounds.height
            override val horizontalAlign = this@renderBounds.horizontalAlign
            override val verticalAlign = this@renderBounds.verticalAlign

            override fun render(posX: Int, posY: Int) {
                GuiRenderUtils.drawRect(0, 0, width, height, color.rgb)
                this@renderBounds.render(posX, posY)
            }

        }

        fun rectButton(
            content: Renderable,
            activeColor: Color,
            inActiveColor: Color = activeColor.darker(0.4),
            hoveredColor: (Color) -> Color = { it.darker(0.5) },
            onClick: (Boolean) -> Unit,
            onHover: (Boolean) -> Unit = {},
            button: Int = 0,
            bypassChecks: Boolean = false,
            condition: (Boolean) -> Boolean = { true },
            startState: Boolean = false,
            padding: Int = 2,
            radius: Int = 10,
            smoothness: Int = 2,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {

            var state = startState

            val color get() = if (state) activeColor else inActiveColor

            override val width = content.width + padding * 2
            override val height = content.height + padding * 2
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                val realColor: Color
                if (isHovered(posX, posY) && condition(state) && shouldAllowLink(true, bypassChecks)) {
                    if ((button - 100).isKeyClicked()) {
                        state = !state
                        onClick(state)
                    }
                    onHover(state)
                    realColor = hoveredColor(color)
                } else {
                    realColor = color
                }
                //#if TODO
                RenderUtils.drawRoundRect(0, 0, width, height, realColor.rgb, radius, smoothness)
                //#endif
                DrawContextUtils.translate(padding.toFloat(), padding.toFloat(), 0f)
                content.render(posX + padding, posY + padding)
                DrawContextUtils.translate(-padding.toFloat(), -padding.toFloat(), 0f)
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
                    DrawContextUtils.translate(x.toFloat(), 0f, 0f)
                }
                DrawContextUtils.translate(-(xOffset - posX).toFloat(), 0f, 0f)
            }
        }

        fun fixedSizeColumn(
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

        fun line(builderAction: MutableList<Renderable>.() -> Unit): Renderable {
            return HorizontalContainerRenderable(buildList { builderAction() })
        }

        fun vertical(builderAction: MutableList<Renderable>.() -> Unit): Renderable {
            return VerticalContainerRenderable(buildList { builderAction() }, spacing = 2)
        }

        @Deprecated(
            "Use HorizontalContainerRenderable instead",
            ReplaceWith("HorizontalContainerRenderable(content, spacing, horizontalAlign, verticalAlign)"),
        )
        fun horizontalContainer(
            content: List<Renderable>,
            spacing: Int = 0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ): Renderable = HorizontalContainerRenderable(
            content,
            spacing,
            horizontalAlign,
            verticalAlign,
        )

        @Deprecated(
            "Use VerticalContainerRenderable instead",
            ReplaceWith("VerticalContainerRenderable(content, spacing, horizontalAlign, verticalAlign)"),
        )
        fun verticalContainer(
            content: List<Renderable>,
            spacing: Int = 0,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ): Renderable = VerticalContainerRenderable(
            content,
            spacing,
            horizontalAlign,
            verticalAlign,
        )

        fun scrollList(
            list: List<Renderable>,
            height: Int,
            scrollValue: ScrollValue = ScrollValue(),
            velocity: Double = 2.0,
            button: Int? = null,
            bypassChecks: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
            showScrollableTipsInList: Boolean = false,
        ) = object : Renderable {
            private val scrollUpTip = RenderableString("§7§oMore items above (scroll)")
            private val scrollDownTip = RenderableString("§7§oMore items below (scroll)")

            override val width = maxOf(list.maxOfOrNull { it.width } ?: 0, scrollDownTip.width, scrollUpTip.width)
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val virtualHeight = list.sumOf { it.height }

            private val scroll = ScrollInput.Companion.Vertical(
                scrollValue,
                0,
                virtualHeight - height + if (showScrollableTipsInList && virtualHeight > height) scrollUpTip.height else 0,
                velocity,
                button,
            )

            override fun render(posX: Int, posY: Int) {
                scroll.update(
                    isHovered(posX, posY) && shouldAllowLink(true, bypassChecks),
                )

                scrollListRender(
                    posX,
                    posY,
                    height,
                    width,
                    list,
                    scroll,
                    showScrollableTipsInList,
                    scrollUpTip,
                    scrollDownTip,
                )
            }
        }

        fun searchableScrollList(
            content: Map<Renderable, String?>,
            height: Int,
            scrollValue: ScrollValue = ScrollValue(),
            velocity: Double = 2.0,
            button: Int? = null,
            textInput: TextInput,
            key: Int,
            bypassChecks: Boolean = false,
            showScrollableTipsInList: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            private var list: Set<Renderable> = filterList(content, textInput.textBox)

            private val scrollUpTip = RenderableString("§7§oMore items above (scroll)")
            private val scrollDownTip = RenderableString("§7§oMore items below (scroll)")

            override val width = maxOf(list.maxOfOrNull { it.width } ?: 0, scrollUpTip.width, scrollDownTip.width)
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val virtualHeight get() = list.sumOf { it.height }
            private var scroll = createScroll()

            init {
                textInput.registerToEvent(key) {
                    // null = ignored, never filtered
                    list = filterList(content, textInput.textBox)
                    scroll = createScroll()
                }
            }

            private fun createScroll() = ScrollInput.Companion.Vertical(
                scrollValue,
                0,
                virtualHeight - height + if (showScrollableTipsInList && virtualHeight > height) scrollUpTip.height else 0,
                velocity,
                button,
            )

            override fun render(posX: Int, posY: Int) {
                scroll.update(
                    isHovered(posX, posY) && shouldAllowLink(true, bypassChecks),
                )

                scrollListRender(
                    posX,
                    posY,
                    height,
                    width,
                    list,
                    scroll,
                    showScrollableTipsInList,
                    scrollUpTip,
                    scrollDownTip,
                )
            }
        }

        private fun scrollListRender(
            posX: Int,
            posY: Int,
            height: Int,
            width: Int,
            list: Collection<Renderable>,
            scroll: ScrollInput.Companion.Vertical,
            showScrollableTipsInList: Boolean,
            scrollUpTip: Renderable,
            scrollDownTip: Renderable,
        ) {
            val end = scroll.asInt() + height + 1

            var renderY = 0
            var virtualY = 0
            var found = false

            var negativeSpace1 = 0
            var negativeSpace2 = 0

            // If showScrollableTipsInList is true, and we are scrolled 'down', display a tip indicating
            // there are more items above
            if (showScrollableTipsInList && !scroll.atMinimum()) {
                scrollUpTip.renderXAligned(posX, posY, width)
                DrawContextUtils.translate(0f, scrollUpTip.height.toFloat(), 0f)
                renderY += scrollUpTip.height
                negativeSpace1 -= scrollUpTip.height
            }

            val atScrollEnd = scroll.atMaximum()
            if (!atScrollEnd) {
                negativeSpace2 -= scrollDownTip.height
            }

            val window = scroll.asInt()..(end + negativeSpace1 + negativeSpace2)

            for (renderable in list) {
                if ((virtualY..virtualY + renderable.height) in window) {
                    renderable.renderXAligned(posX, posY + renderY, width)
                    DrawContextUtils.translate(0f, renderable.height.toFloat(), 0f)
                    renderY += renderable.height
                    found = true
                } else if (found) {
                    if (renderY + renderable.height <= height + negativeSpace2) {
                        renderable.renderXAligned(posX, posY + renderY, width)
                        DrawContextUtils.translate(0f, renderable.height.toFloat(), 0f)
                        renderY += renderable.height
                    }
                    break
                }
                virtualY += renderable.height
            }

            // If showScrollableTipsInList is true, and we are scrolled 'up', display a tip indicating
            // there are more items below
            if (showScrollableTipsInList && !atScrollEnd) {
                scrollDownTip.renderXAligned(posX, posY + height - scrollDownTip.height, width)
            }

            DrawContextUtils.translate(0f, -renderY.toFloat(), 0f)
        }

        fun filterList(content: Map<Renderable, String?>, textBox: String) =
            filterListBase(content, textBox, RenderableString("§cNo search results!"))

        private fun filterListMap(content: Map<List<Renderable>, String?>, textBox: String) =
            filterListBase(content, textBox, listOf(RenderableString("§cNo search results!")))

        private fun <T> filterListBase(content: Map<T, String?>, textBox: String, empty: T): Set<T> {
            val map = content.filter { it.value?.contains(textBox, ignoreCase = true) != false }
            val set = map.keys.toMutableSet()
            if (map.filter { it.value != null }.isEmpty()) {
                if (textBox.isNotEmpty()) {
                    set.add(empty)
                }
            }
            return set
        }

        fun searchableScrollable(
            table: Map<List<Renderable>, String>,
            key: Int,
            lines: Int,
            velocity: Double,
            textInput: TextInput,
            scrollValue: ScrollValue,
            showScrollableTipsInList: Boolean = true,
            asTable: Boolean = true,
        ): Renderable? {
            if (table.isEmpty()) return null
            return if (asTable) {
                val height = RenderableUtils.calculateTableY(table.keys, 0).maxOf { it.value }
                searchableScrollTable(
                    table,
                    key = key,
                    height = lines * height,
                    textInput = textInput,
                    velocity = velocity,
                    scrollValue = scrollValue,
                    showScrollableTipsInList = showScrollableTipsInList,
                )
            } else {
                @Suppress("USELESS_CAST")
                val content = table.mapKeys { HorizontalContainerRenderable(it.key) as Renderable }
                val height = content.maxOf { it.key.height }
                searchableScrollList(
                    content,
                    key = key,
                    height = lines * height,
                    textInput = textInput,
                    velocity = velocity,
                    scrollValue = scrollValue,
                    showScrollableTipsInList = showScrollableTipsInList,
                )
            }
        }

        private fun searchableScrollTable(
            content: Map<List<Renderable>, String?>,
            height: Int,
            scrollValue: ScrollValue = ScrollValue(),
            velocity: Double = 2.0,
            button: Int? = null,
            textInput: TextInput,
            key: Int,
            xPadding: Int = 1,
            yPadding: Int = 0,
            header: List<Renderable> = emptyList(),
            bypassChecks: Boolean = false,
            showScrollableTipsInList: Boolean = false,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {

            private val scrollUpTip = RenderableString("§7§oMore items above (scroll)")
            private val scrollDownTip = RenderableString("§7§oMore items below (scroll)")

            private var list = filterListMap(content, textInput.textBox).toList()

            private val fullContent = if (header.isNotEmpty()) listOf(header) + content.keys else content.keys

            val xOffsets = RenderableUtils.calculateTableX(fullContent, xPadding)
            val yOffsets = RenderableUtils.calculateTableY(fullContent, yPadding)

            override val width = maxOf(xOffsets.sum(), scrollUpTip.width, scrollDownTip.width)
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            private val virtualHeight get() = list.sumOf { yOffsets[it] ?: 0 }

            private val end get() = scroll.asInt() + height + 1

            private var scroll = createScroll()

            private fun createScroll() = ScrollInput.Companion.Vertical(
                scrollValue,
                yOffsets[header] ?: 0,
                virtualHeight - height + if (showScrollableTipsInList && virtualHeight > height) scrollUpTip.height else 0,
                velocity,
                button,
            )

            init {
                textInput.registerToEvent(key) {
                    // null = ignored, never filtered
                    list = filterListMap(content, textInput.textBox).toList()
                    scroll = createScroll()
                }
            }

            override fun render(posX: Int, posY: Int) {
                scroll.update(
                    isHovered(posX, posY) && shouldAllowLink(true, bypassChecks),
                )

                var renderY = 0
                if (header.isNotEmpty()) {
                    var offset = 0
                    for ((index, renderable) in header.withIndex()) {
                        renderable.renderXYAligned(
                            posX + offset,
                            posY,
                            xOffsets[index],
                            yOffsets[header] ?: 0,
                        )
                        DrawContextUtils.translate(xOffsets[index].toFloat(), 0f, 0f)
                        offset += xOffsets[index]
                    }
                    DrawContextUtils.translate(-offset.toFloat(), 0f, 0f)
                    val yShift = yOffsets[header] ?: 0
                    DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }

                val range = if (list.size == 1) {
                    0..0
                } else {
                    val list = list
                    val nStart = scroll.asInt()

                    val endReduce1 = if (showScrollableTipsInList && !scroll.atMinimum()) scrollUpTip.height else 0
                    val endReduce2 = if (showScrollableTipsInList && !scroll.atMaximum()) scrollDownTip.height else 0

                    val nEnd = end - endReduce1 - endReduce2

                    val sequence = list.asSequence().withIndex()
                    val folded = sequence.runningIndexedFold(0) { past, value -> past + (yOffsets[value] ?: 0) }
                    val pair = folded.firstTwiceOf({ it.value >= nStart }, { it.value >= nEnd || it.index == list.lastIndex })
                    val firstElement = pair.first ?: return // Never null
                    val lastElement = pair.second ?: return // Never null

                    val spaceLeft = nEnd - nStart - if (lastElement.index == list.lastIndex && lastElement.value < nEnd) 1 else 0

                    val subEnd = if ((lastElement.value - firstElement.value) < spaceLeft) 0 else 1

                    val start = firstElement.index

                    val end = (lastElement.takeIf { it.value >= nEnd }?.index ?: list.size).minus(subEnd)

                    start until end
                }

                if (showScrollableTipsInList && !scroll.atMinimum()) {
                    scrollUpTip.render(posX, posY)
                    val yShift = scrollUpTip.height
                    renderY += yShift
                    DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
                }

                for (rowIndex in range) {
                    val row = list[rowIndex]
                    var offset = 0
                    val yShift = yOffsets[row] ?: 0
                    for ((index, renderable) in row.withIndex()) {
                        renderable.renderXYAligned(
                            posX + offset,
                            posY + renderY,
                            xOffsets[index],
                            yShift,
                        )
                        DrawContextUtils.translate(xOffsets[index].toFloat(), 0f, 0f)
                        offset += xOffsets[index]
                    }
                    DrawContextUtils.translate(-offset.toFloat(), 0f, 0f)
                    DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }

                if (showScrollableTipsInList && !scroll.atMaximum()) {
                    scrollDownTip.render(posX, posY)
                }

                DrawContextUtils.translate(0f, -renderY.toFloat(), 0f)
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

            val xOffsets: List<Int> = RenderableUtils.calculateTableXOffsets(content, xPadding)
            val yOffsets: List<Int> = RenderableUtils.calculateTableYOffsets(content, yPadding)

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
                        DrawContextUtils.translate(xOffsets[index].toFloat(), 0f, 0f)
                        renderable?.renderXYAligned(
                            posX + xOffsets[index],
                            posY,
                            xOffsets[index + 1] - xOffsets[index],
                            yOffsets[1],
                        )
                        DrawContextUtils.translate(-xOffsets[index].toFloat(), 0f, 0f)
                    }
                    val yShift = yOffsets[1] - yOffsets[0]
                    DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }
                val range =
                    yOffsets.indexOfFirst { it >= scroll.asInt() }..<(
                        yOffsets.indexOfFirst { it >= end }.takeIf { it > 0 }
                            ?: yOffsets.size
                        ) - 1

                val range2 = if (range.last + 3 <= yOffsets.size && yOffsets[range.last + 2] - yOffsets[range.first] <= height - renderY) {
                    range.first..range.last() + 1
                } else {
                    range
                }

                for (rowIndex in range2) {
                    for ((index, renderable) in content[rowIndex].withIndex()) {
                        DrawContextUtils.translate(xOffsets[index].toFloat(), 0f, 0f)
                        renderable?.renderXYAligned(
                            posX + xOffsets[index],
                            posY + renderY,
                            xOffsets[index + 1] - xOffsets[index],
                            yOffsets[rowIndex + 1] - yOffsets[rowIndex],
                        )
                        DrawContextUtils.translate(-xOffsets[index].toFloat(), 0f, 0f)
                    }
                    val yShift = yOffsets[rowIndex + 1] - yOffsets[rowIndex]
                    DrawContextUtils.translate(0f, yShift.toFloat(), 0f)
                    renderY += yShift
                }
                DrawContextUtils.translate(0f, -renderY.toFloat(), 0f)
            }
        }

        //#if TODO
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
                //#if TODO
                RenderUtils.drawRoundRect(0, 0, width, height, color.rgb, radius, smoothness)
                //#endif
                DrawContextUtils.translate(padding.toFloat(), padding.toFloat(), 0f)
                input.render(posX + padding, posY + padding)
                DrawContextUtils.translate(-padding.toFloat(), -padding.toFloat(), 0f)
            }
        }

        fun drawInsideRoundedRectOutline(
            input: Renderable,
            padding: Int = 2,
            radius: Int = 10,
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
                DrawContextUtils.translate(padding.toFloat(), padding.toFloat(), 0f)
                input.render(posX + padding, posY + padding)
                DrawContextUtils.translate(-padding.toFloat(), -padding.toFloat(), 0f)

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
            }
        }

        fun drawInsideImage(
            input: Renderable,
            texture: ResourceLocation,
            alpha: Int = 255,
            padding: Int = 2,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
            radius: Int = 0,
        ) = object : Renderable {
            override val width = input.width + padding * 2
            override val height = input.height + padding * 2
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                RenderUtils.drawRoundTexturedRect(
                    0,
                    0,
                    width,
                    height,
                    GL11.GL_NEAREST,
                    radius,
                    texture = texture,
                    alpha = alpha / 255f,
                )

                DrawContextUtils.translate(padding.toFloat(), padding.toFloat(), 0f)
                input.render(posX + padding, posY + padding)
                DrawContextUtils.translate(-padding.toFloat(), -padding.toFloat(), 0f)
            }
        }

        fun drawInsideFixedSizedImage(
            input: Renderable,
            texture: ResourceLocation,
            width: Int = input.width,
            height: Int = input.height,
            alpha: Int = 255,
            padding: Int = 2,
            uMin: Float = 0f,
            uMax: Float = 1f,
            vMin: Float = 0f,
            vMax: Float = 1f,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.TOP,
        ) = object : Renderable {
            override val width = width
            override val height = height
            override val horizontalAlign = horizontalAlign
            override val verticalAlign = verticalAlign

            override fun render(posX: Int, posY: Int) {
                GuiRenderUtils.drawTexturedRect(0, 0, width, height, uMin, uMax, vMin, vMax, texture, alpha / 255f)

                DrawContextUtils.translate(padding.toFloat(), padding.toFloat(), 0f)
                input.render(posX + padding, posY + padding)
                DrawContextUtils.translate(-padding.toFloat(), -padding.toFloat(), 0f)
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

                DrawContextUtils.translate(padding.toFloat(), padding.toFloat(), 0f)
                input.render(posX + padding, posY + padding)
                DrawContextUtils.translate(-padding.toFloat(), -padding.toFloat(), 0f)
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

            override fun render(posX: Int, posY: Int) {
                GlStateManager.color(1f, 1f, 1f, 1f)
                if (color != null) RenderLivingEntityHelper.setEntityColor(player, color, colorCondition)
                val mouse = currentRenderPassMousePosition ?: return
                val mouseXRelativeToPlayer = if (followMouse) (posX + playerX - mouse.first).toFloat() else eyesX
                val mouseYRelativeToPlayer = if (followMouse) (posY + playerY - mouse.second - 1.62 * entityScale).toFloat() else eyesY
                DrawContextUtils.translate(0f, 0f, 100f)
                drawEntityOnScreen(
                    playerX,
                    playerY,
                    entityScale,
                    mouseXRelativeToPlayer,
                    mouseYRelativeToPlayer,
                    player,
                )
                DrawContextUtils.translate(0f, 0f, -100f)
            }
        }
        //#endif
    }
}
