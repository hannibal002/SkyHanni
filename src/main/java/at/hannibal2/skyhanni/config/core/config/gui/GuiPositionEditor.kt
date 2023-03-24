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
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsX_
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsY_
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getDummySize
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.isCenterX_
import io.github.moulberry.notenoughupdates.util.Utils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.io.IOException

class GuiPositionEditor(
    overlayPositions: LinkedHashMap<Position, Position>,
    closedCallback: Runnable,
) : GuiScreen() {
    private val positions: ArrayList<Position>
    private val originalPositions: ArrayList<Position>
    private val closedCallback: Runnable
    private var grabbedX = 0
    private var grabbedY = 0
    private var clickedPos = -1

    init {
        val pos = ArrayList<Position>()
        val ogPos = ArrayList<Position>()
        for (i in 0 until overlayPositions.size) {
            val overlay = ArrayList(overlayPositions.keys)[i]
            pos.add(overlayPositions[overlay]!!)
            ogPos.add(pos[i].clone())
        }
        positions = pos
        originalPositions = ogPos
        this.closedCallback = closedCallback
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        closedCallback.run()
        clickedPos = -1
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        val border = 2
        var hoveredPos = -1

        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        var mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        var mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
        for (i in positions.indices.reversed()) {
            val position = positions[i]
            val elementWidth = position.getDummySize().x.toInt()
            val elementHeight = position.getDummySize().y.toInt()
            var x = position.getAbsX_()
            var y = position.getAbsY_()
            if (position.isCenterX_()) x -= elementWidth / 2
            if (position.isCenterY) y -= elementHeight / 2
            if (mouseX >= x - border && mouseY >= y - border && mouseX <= x + elementWidth + border * 2 && mouseY <= y + elementHeight + border * 2) {
                hoveredPos = i
                break
            }
        }

        drawDefaultBackground()
        val text = "§cSkyHanni Position Editor"
        Utils.drawStringCentered(
            text, Minecraft.getMinecraft().fontRendererObj,
            (scaledResolution.scaledWidth / 2).toFloat(), 8f, true, 0xffffff
        )

        super.drawScreen(mouseX, mouseY, partialTicks)
        GlStateManager.pushMatrix()
        width = scaledResolution.scaledWidth
        height = scaledResolution.scaledHeight
        mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
        for (position in positions) {
            var elementWidth = position.getDummySize(true).x.toInt()
            var elementHeight = position.getDummySize(true).y.toInt()
            if (position.clicked) {
                grabbedX += position.moveX(mouseX - grabbedX, elementWidth, scaledResolution)
                grabbedY += position.moveY(mouseY - grabbedY, elementHeight, scaledResolution)
            }
            var x = position.getAbsX_()
            val y = position.getAbsY_()
            if (position.isCenterX_()) x -= elementWidth / 2

            elementWidth = position.getDummySize().x.toInt()
            elementHeight = position.getDummySize().y.toInt()
            drawRect(x - border, y - border, x + elementWidth + border * 2, y + elementHeight + border * 2, -0x7fbfbfc0)

            if (hoveredPos != -1) {
                val pos = positions[hoveredPos]
                Utils.drawStringCentered(
                    "§b" + pos.internalName, Minecraft.getMinecraft().fontRendererObj,
                    (scaledResolution.scaledWidth / 2).toFloat(), 18f, true, 0xffffff
                )
                val location = "§7x: §e${pos.rawX}§7, y: §e${pos.rawY}"
                Utils.drawStringCentered(
                    location, Minecraft.getMinecraft().fontRendererObj,
                    (scaledResolution.scaledWidth / 2).toFloat(), 28f, true, 0xffffff
                )
            }
        }
        GlStateManager.popMatrix()
    }

    @Throws(IOException::class)
    override fun mouseClicked(originalX: Int, priginalY: Int, mouseButton: Int) {
        super.mouseClicked(originalX, priginalY, mouseButton)

        val mouseX: Int
        val mouseY: Int
        if (mouseButton == 0) {
            mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
            mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
            for (i in positions.indices.reversed()) {
                val position = positions[i]
                val elementWidth = position.getDummySize().x.toInt()
                val elementHeight = position.getDummySize().y.toInt()
                var x = position.getAbsX_()
                val y = position.getAbsY_()
                if (position.isCenterX_()) x -= elementWidth / 2
                if (!position.clicked) {
                    if (mouseX >= x && mouseY >= y && mouseX <= x + elementWidth && mouseY <= y + elementHeight) {
                        clickedPos = i
                        position.clicked = true
                        grabbedX = mouseX
                        grabbedY = mouseY
                        break
                    }
                }
            }
        }
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (clickedPos != -1) {
            val position = positions[clickedPos]
            val elementWidth = position.getDummySize(true).x.toInt()
            val elementHeight = position.getDummySize(true).y.toInt()
            if (keyCode == SkyHanniMod.feature.gui.keyBindReset) {

                position.set(originalPositions[positions.indexOf(position)])

                // TODO option to reset everything
//                val ii = 0
//                for (pos in positions) {
//                    pos.set(originalPositions[ii])
//                }


            } else if (!position.clicked) {
                val shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
                val dist = if (shiftHeld) 10 else 1
                if (keyCode == Keyboard.KEY_DOWN) {
                    position.moveY(dist, elementHeight, ScaledResolution(Minecraft.getMinecraft()))
                } else if (keyCode == Keyboard.KEY_UP) {
                    position.moveY(-dist, elementHeight, ScaledResolution(Minecraft.getMinecraft()))
                } else if (keyCode == Keyboard.KEY_LEFT) {
                    position.moveX(-dist, elementWidth, ScaledResolution(Minecraft.getMinecraft()))
                } else if (keyCode == Keyboard.KEY_RIGHT) {
                    position.moveX(dist, elementWidth, ScaledResolution(Minecraft.getMinecraft()))
                }
            }
        }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        super.mouseReleased(mouseX, mouseY, state)
        for (position in positions) {
            position.clicked = false
        }
    }

    override fun mouseClickMove(originalX: Int, priginalY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        super.mouseClickMove(originalX, priginalY, clickedMouseButton, timeSinceLastClick)

        var mouseX: Int
        var mouseY: Int
        for (position in positions) {
            val elementWidth = position.getDummySize(true).x.toInt()
            val elementHeight = position.getDummySize(true).y.toInt()
            if (position.clicked) {
                val scaledResolution = Utils.pushGuiScale(-1)
                mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
                mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
                grabbedX += position.moveX(mouseX - grabbedX, elementWidth, scaledResolution)
                grabbedY += position.moveY(mouseY - grabbedY, elementHeight, scaledResolution)
                Utils.pushGuiScale(-1)
            }
        }
    }
}