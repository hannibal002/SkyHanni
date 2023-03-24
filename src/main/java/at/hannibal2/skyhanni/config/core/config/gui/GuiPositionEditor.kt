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
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsX
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getAbsY
import at.hannibal2.skyhanni.data.GuiEditManager.Companion.getDummySize
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
) : GuiScreen() {
    private val positions: ArrayList<Position>
    private val originalPositions: ArrayList<Position>
    private var grabbedX = 0
    private var grabbedY = 0
    private var clickedPos = -1
    private val border = 2

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
    }

    override fun onGuiClosed() {
        super.onGuiClosed()
        clickedPos = -1
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        var hoveredPos = -1

        var mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        var mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
        for (i in positions.indices.reversed()) {
            val position = positions[i]
            val elementWidth = position.getDummySize().x
            val elementHeight = position.getDummySize().y
            val x = position.getAbsX()
            val y = position.getAbsY()
            if (inXY(mouseX, x, mouseY, y, elementWidth, elementHeight)) {
                hoveredPos = i
                break
            }
        }

        drawDefaultBackground()
        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())
        Utils.drawStringCentered(
            "§cSkyHanni Position Editor", Minecraft.getMinecraft().fontRendererObj,
            (scaledResolution.scaledWidth / 2).toFloat(), 8f, true, 0xffffff
        )

        super.drawScreen(mouseX, mouseY, partialTicks)
        GlStateManager.pushMatrix()
        width = scaledResolution.scaledWidth
        height = scaledResolution.scaledHeight
        mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
        for (position in positions) {
            if (position.clicked) {
                grabbedX += position.moveX(mouseX - grabbedX)
                grabbedY += position.moveY(mouseY - grabbedY)
            }
            val x = position.getAbsX()
            val y = position.getAbsY()

            val elementWidth = position.getDummySize().x
            val elementHeight = position.getDummySize().y
            drawRect(x - border, y - border, x + elementWidth + border * 2, y + elementHeight + border * 2, -0x7fbfbfc0)

        }

        GlStateManager.popMatrix()

        var displayPos = -1
        if (clickedPos != -1) {
            if (positions[clickedPos].clicked) {
                displayPos = clickedPos
            }
        }
        if (displayPos == -1) {
            displayPos = hoveredPos
        }

        if (displayPos != -1) {
            val pos = positions[displayPos]
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

    @Throws(IOException::class)
    override fun mouseClicked(originalX: Int, priginalY: Int, mouseButton: Int) {
        super.mouseClicked(originalX, priginalY, mouseButton)
        if (mouseButton != 0) return

        val mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        val mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
        for (i in positions.indices.reversed()) {
            val position = positions[i]
            val elementWidth = position.getDummySize().x
            val elementHeight = position.getDummySize().y
            val x = position.getAbsX()
            val y = position.getAbsY()
            if (!position.clicked) {
                if (inXY(mouseX, x, mouseY, y, elementWidth, elementHeight)) {
                    clickedPos = i
                    position.clicked = true
                    grabbedX = mouseX
                    grabbedY = mouseY
                    break
                }
            }
        }
    }

    private fun inXY(
        mouseX: Int,
        x: Int,
        mouseY: Int,
        y: Int,
        elementWidth: Int,
        elementHeight: Int,
    ) =
        mouseX >= x - border && mouseY >= y - border && mouseX <= x + elementWidth + border * 2 && mouseY <= y + elementHeight + border * 2

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (clickedPos != -1) {
            val position = positions[clickedPos]
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
                    position.moveY(dist)
                } else if (keyCode == Keyboard.KEY_UP) {
                    position.moveY(-dist)
                } else if (keyCode == Keyboard.KEY_LEFT) {
                    position.moveX(-dist)
                } else if (keyCode == Keyboard.KEY_RIGHT) {
                    position.moveX(dist)
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
            if (position.clicked) {
                mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
                mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1
                grabbedX += position.moveX(mouseX - grabbedX)
                grabbedY += position.moveY(mouseY - grabbedY)
                Utils.pushGuiScale(-1)
            }
        }
    }
}