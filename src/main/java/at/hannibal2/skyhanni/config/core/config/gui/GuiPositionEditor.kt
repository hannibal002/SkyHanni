/*
 * Copyright (C) 2022 NotEnoughUpdates contributors
 *
 * This file is part of NotEnoughUpdates.
 *
 * NotEnoughUpdates is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * NotEnoughUpdates is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with NotEnoughUpdates. If not, see <https://www.gnu.org/licenses/>.
 */
package at.hannibal2.skyhanni.config.core.config.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.getDummySize
import at.hannibal2.skyhanni.data.OtherInventoryData
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import at.hannibal2.skyhanni.utils.renderables.RenderableTooltips
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import io.github.notenoughupdates.moulconfig.annotations.ConfigLink
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField
//? if < 26.1 {
/*import at.hannibal2.skyhanni.utils.compat.RenderCompat
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import kotlin.math.roundToInt
*///?}

class GuiPositionEditor(
    private val positions: List<Position>,
    private val border: Int,
    private val oldScreen: SkyHanniGuiContainer? = null,
    private val chestGuiPositions: Set<Position> = emptySet(),
) : SkyHanniBaseScreen() {

    private val config get() = SkyHanniMod.feature.gui
    private var grabbedX = 0
    private var grabbedY = 0
    private var clickedPos = -1
    private val oldScreenRenderContext = OldScreenRenderContext(
        oldScreen,
        { getEditorScaledWidth() },
        { getEditorScaledHeight() },
    )

    override fun guiClosed() {
        clickedPos = -1
        for (position in positions) {
            position.clicked = false
        }
        OtherInventoryData.close()
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        width = getEditorScaledWidth()
        height = getEditorScaledHeight()
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        oldScreenRenderContext.render(mouseX, mouseY, partialTicks)

        val hoveredPos = renderRectangles()

        renderLabels(hoveredPos)
    }

    fun renderWithOldScreenMetrics(action: () -> Unit) = oldScreenRenderContext.withRenderTransform(action)

    private fun <T> Position.withPositionMetrics(action: () -> T): T =
        if (this in chestGuiPositions) oldScreenRenderContext.withMetrics(action) else action()

    private fun <T> Position.withPositionRenderTransform(action: () -> T): T =
        if (this in chestGuiPositions) oldScreenRenderContext.withRenderTransform(action) else action()

    private fun Position.isHoveredWithMetrics() = withPositionMetrics { isHovered() }

    private fun renderLabels(hoveredPos: Int) {
        val displayPos = when {
            clickedPos != -1 && positions[clickedPos].clicked -> clickedPos
            else -> hoveredPos
        }

        // When the mouse isn't currently hovering over a gui element
        val text = if (displayPos == -1) {
            val extraInfo = SkyHanniMod.feature.gui.keyBindOpen == GLFW.GLFW_KEY_UNKNOWN

            buildList {
                add("§cSkyHanni Position Editor")
                if (extraInfo) {
                    add("§aTo edit hidden GUI elements set a key in /sh edit")
                    add("§athen click that key while the GUI element is visible")
                }
            }
        } else {
            getTextForPos(positions[displayPos])
        }

        renderHover(text)
    }

    private fun getTextForPos(pos: Position): List<String> {
        if (pos.clicked) return listOf("§7x: §e${pos.x}§7, y: §e${pos.y}")

        return listOf(
            "§cSkyHanni Position Editor",
            "§b${pos.internalName}",
            "  §7x: §e${pos.x}§7, y: §e${pos.y}§7, scale: §e${pos.scale.roundTo(2)}",
            "",
            "§eRight-Click to open associated config options!",
            "§eUse Scroll-Wheel to resize!",
            "§e${KeyboardManager.getKeyName(config.keyBindReset)} to reset to default position!",
        )
    }

    private fun renderHover(text: List<String>) {
        RenderableTooltips.setTooltipForRender(text.map(StringRenderable::from))
    }

    private fun renderRectangles(): Int {
        var hoveredPos = -1
        var alreadyHadHover = false

        for ((index, position) in positions.withIndex().reversed()) {
            position.withPositionRenderTransform {
                val (mouseX, mouseY) = GuiScreenUtils.mousePos
                val dummy = position.getDummySize(true)
                if (position.clicked) {
                    grabbedX += position.moveX(mouseX - grabbedX, dummy.x)
                    grabbedY += position.moveY(mouseY - grabbedY, dummy.y)
                }

                val isHovering = position.isHovered() && !alreadyHadHover

                val x = position.getAbsX()
                val y = position.getAbsY()

                val gray = -0x7fbfbfc0 // #40404080
                val selected = -0x7F0F0F10 // #F0F0F080
                GuiRenderUtils.drawRect(
                    x - border,
                    y - border,
                    x + position.getDummySize().x + border * 2,
                    y + position.getDummySize().y + border * 2,
                    if (isHovering) selected else gray,
                )

                if (isHovering) {
                    alreadyHadHover = true
                    hoveredPos = index
                }
            }
        }

        return hoveredPos
    }

    private fun getEditorScaledHeight() = Minecraft.getInstance().window.guiScaledHeight
    private fun getEditorScaledWidth() = Minecraft.getInstance().window.guiScaledWidth

    override fun onMouseClicked(originalMouseX: Int, originalMouseY: Int, mouseButton: Int) {
        for (i in positions.indices.reversed()) {
            val position = positions[i]
            val handled = position.withPositionMetrics {
                if (!position.isHovered()) return@withPositionMetrics false

                val (mouseX, mouseY) = GuiScreenUtils.mousePos

                when (mouseButton) {
                    1 -> position.jumpToConfigOptions()
                    2 -> if (config.keyBindReset == KeyboardManager.MIDDLE_MOUSE) position.resetPositionAndScale()
                    0 -> if (!position.clicked) {
                        clickedPos = i
                        position.clicked = true
                        grabbedX = mouseX
                        grabbedY = mouseY
                    }
                }

                true
            }
            if (handled) break
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        if (keyCode == config.keyBindReset) {
            positions.firstOrNull { it.isHoveredWithMetrics() }?.resetPositionAndScale()
            return
        }
        if (clickedPos == -1) return
        val position = positions[clickedPos]
        if (position.clicked) return

        position.withPositionMetrics {
            val dist = if (KeyboardManager.isShiftKeyDown()) 10 else 1
            val elementWidth = position.getDummySize(true).x
            val elementHeight = position.getDummySize(true).y
            when (keyCode) {
                GLFW.GLFW_KEY_DOWN -> position.moveY(dist, elementHeight)
                GLFW.GLFW_KEY_UP -> position.moveY(-dist, elementHeight)
                GLFW.GLFW_KEY_LEFT -> position.moveX(-dist, elementWidth)
                GLFW.GLFW_KEY_RIGHT -> position.moveX(dist, elementWidth)
                GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> position.scale -= .1F
                GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> position.scale += .1F
            }
        }
    }

    private fun Position.isHovered(): Boolean {
        val (mouseX, mouseY) = GuiScreenUtils.mousePos

        val elementWidth = getDummySize().x
        val elementHeight = getDummySize().y
        val x = getAbsX()
        val y = getAbsY()
        return GuiRenderUtils.isPointInRect(
            mouseX,
            mouseY,
            x - border,
            y - border,
            elementWidth + border * 2,
            elementHeight + border * 2,
        )
    }

    private fun Position.resetPositionAndScale() {
        val field = linkField ?: return
        val clazz = field.declaringClass.kotlin
        val instance = clazz.createInstance()

        val defaultPosition = clazz.declaredMemberProperties
            .firstNotNullOfOrNull { property ->
                property.javaField
                    ?.getAnnotation(ConfigLink::class.java)
                    ?.takeIf { it.field == field.name }
                    ?.let { property.getter.call(instance) as? Position }
            } ?: return

        with(this) {
            moveTo(defaultPosition.x, defaultPosition.y)
            scale = defaultPosition.scale
        }
    }

    override fun onMouseReleased(originalMouseX: Int, originalMouseY: Int, state: Int) {
        for (position in positions) {
            position.clicked = false
        }
    }

    override fun onMouseClickMove(originalMouseX: Int, originalMouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        for (position in positions) {
            if (!position.clicked) continue

            position.withPositionMetrics {
                val (mouseX, mouseY) = GuiScreenUtils.mousePos

                val elementWidth = position.getDummySize(true).x
                val elementHeight = position.getDummySize(true).y
                grabbedX += position.moveX(mouseX - grabbedX, elementWidth)
                grabbedY += position.moveY(mouseY - grabbedY, elementHeight)
                position.internalName?.let(GuiEditManager::handleGuiPositionMoved)
            }
        }
    }

    override fun onHandleMouseInput() {
        val scroll = MouseCompat.getScrollDelta().takeIf { it != 0 } ?: return
        val hovered = positions.firstOrNull { it.clicked }
            ?: positions.lastOrNull { it.isHoveredWithMetrics() }
            ?: return
        hovered.withPositionMetrics {
            hovered.scale += if (scroll > 0) .1F else -.1F
        }
    }

    override fun onClose() {
        if (oldScreen == null) {
            super.onClose()
        } else {
            Minecraft.getInstance().screen = oldScreen
        }
    }
}

private class OldScreenRenderContext(
    private val oldScreen: SkyHanniGuiContainer?,
    private val getEditorScaledWidth: () -> Int,
    private val getEditorScaledHeight: () -> Int,
) {

    fun render(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val oldScreen = oldScreen ?: return
        val scaleX = oldScreenScaleX(oldScreen)
        val scaleY = oldScreenScaleY(oldScreen)
        val oldScreenMouseX = (mouseX / scaleX).toInt()
        val oldScreenMouseY = (mouseY / scaleY).toInt()

        //? if < 26.1 {
        /*if (oldScreen is InventoryScreen) {
            renderOldInventoryScreen(oldScreen, scaleX, scaleY, mouseX, mouseY, oldScreenMouseX, oldScreenMouseY, partialTicks)
            return
        }
        *///?}

        DrawContextUtils.pushPop {
            DrawContextUtils.scale(scaleX, scaleY)
            //? if >= 26.1 {
            oldScreen.extractBackground(DrawContextUtils.drawContext, oldScreenMouseX, oldScreenMouseY, partialTicks)
            oldScreen.extractRenderState(DrawContextUtils.drawContext, oldScreenMouseX, oldScreenMouseY, partialTicks)
            //?} else {
            /*oldScreen.renderBg(DrawContextUtils.drawContext, partialTicks, oldScreenMouseX, oldScreenMouseY)
            oldScreen.render(DrawContextUtils.drawContext, oldScreenMouseX, oldScreenMouseY, partialTicks)
            *///?}
        }
    }

    fun <T> withMetrics(action: () -> T): T {
        val oldScreen = oldScreen ?: return action()
        return withMetrics(oldScreen, action)
    }

    fun <T> withRenderTransform(action: () -> T): T {
        val oldScreen = oldScreen ?: return action()
        return withMetrics(oldScreen) {
            DrawContextUtils.pushPopResult {
                DrawContextUtils.scale(oldScreenScaleX(oldScreen), oldScreenScaleY(oldScreen))
                action()
            }
        }
    }

    private fun <T> withMetrics(oldScreen: SkyHanniGuiContainer, action: () -> T): T {
        return GuiScreenUtils.withScreenMetricsOverride(
            oldScreen.width,
            oldScreen.height,
            oldScreenScaleFactor(oldScreen),
            action,
        )
    }

    private fun oldScreenScaleX(oldScreen: SkyHanniGuiContainer) = getEditorScaledWidth().toFloat() / oldScreen.width
    private fun oldScreenScaleY(oldScreen: SkyHanniGuiContainer) = getEditorScaledHeight().toFloat() / oldScreen.height

    private fun oldScreenScaleFactor(oldScreen: SkyHanniGuiContainer): Int {
        val windowWidth = Minecraft.getInstance().window.width
        return ((windowWidth + oldScreen.width - 1) / oldScreen.width).coerceAtLeast(1)
    }

    //? if < 26.1 {
    /*private fun renderOldInventoryScreen(
        oldScreen: InventoryScreen,
        scaleX: Float,
        scaleY: Float,
        mouseX: Int,
        mouseY: Int,
        oldScreenMouseX: Int,
        oldScreenMouseY: Int,
        partialTicks: Float,
    ) {
        DrawContextUtils.pushPop {
            DrawContextUtils.scale(scaleX, scaleY)
            drawInventoryBackground(oldScreen)
        }
        renderInventoryPlayer(oldScreen, scaleX, scaleY, mouseX, mouseY)
        DrawContextUtils.pushPop {
            DrawContextUtils.scale(scaleX, scaleY)
            oldScreen.renderContents(DrawContextUtils.drawContext, oldScreenMouseX, oldScreenMouseY, partialTicks)
            oldScreen.renderCarriedItem(DrawContextUtils.drawContext, oldScreenMouseX, oldScreenMouseY)
            oldScreen.renderSnapbackItem(DrawContextUtils.drawContext)
        }
    }

    private fun drawInventoryBackground(oldScreen: InventoryScreen) {
        DrawContextUtils.drawContext.blit(
            RenderCompat.getMinecraftGuiTextured(),
            AbstractContainerScreen.INVENTORY_LOCATION,
            oldScreen.containerLeft(),
            oldScreen.containerTop(),
            0f,
            0f,
            oldScreen.containerImageWidth(),
            oldScreen.containerImageHeight(),
            256,
            256,
        )
    }

    private fun renderInventoryPlayer(
        oldScreen: InventoryScreen,
        scaleX: Float,
        scaleY: Float,
        originalMouseX: Int,
        originalMouseY: Int,
    ) {
        val player = Minecraft.getInstance().player ?: return
        val left = oldScreen.containerLeft()
        val top = oldScreen.containerTop()
        val entityScale = (30 * ((scaleX + scaleY) / 2f)).roundToInt()

        //~ if < 26.1 'extractEntityInInventoryFollowsMouse' -> 'renderEntityInInventoryFollowsMouse'
        InventoryScreen.extractEntityInInventoryFollowsMouse(
            DrawContextUtils.drawContext,
            ((left + 26) * scaleX).roundToInt(),
            ((top + 8) * scaleY).roundToInt(),
            ((left + 75) * scaleX).roundToInt(),
            ((top + 78) * scaleY).roundToInt(),
            entityScale,
            0.0625f * ((scaleX + scaleY) / 2f),
            originalMouseX.toFloat(),
            originalMouseY.toFloat(),
            player,
        )
    }

    private fun AbstractContainerScreen<*>.containerLeft() = leftPos
    private fun AbstractContainerScreen<*>.containerTop() = topPos
    private fun AbstractContainerScreen<*>.containerImageWidth() = imageWidth
    private fun AbstractContainerScreen<*>.containerImageHeight() = imageHeight
    *///?}
}
