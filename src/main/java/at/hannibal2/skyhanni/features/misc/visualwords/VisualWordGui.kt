package at.hannibal2.skyhanni.features.misc.visualwords

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.StringUtils.convertToFormatted
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.io.IOException

open class VisualWordGui : GuiScreen() {
    private var guiLeft = 0
    private var guiTop = 0
    private var screenHeight = 0
    private val sizeX = 360
    private val sizeY = 180
    private val maxTextLength = 75

    private var mouseX = 0
    private var mouseY = 0
    private var lastMouseScroll = 0
    private var noMouseScrollFrames = 0

    private var pageScroll = 0
    private var scrollVelocity = 0.0
    private val maxNoInputFrames = 100

    private var lastClickedHeight = 0
    private var lastClickedWidth = 0
    private var changedIndex = -1
    private var changedAction = ""

    private var currentlyEditing = false
    private var currentIndex = -1

    private var currentTextBox = ""
    private var currentText = ""

    private var modifiedWords = mutableListOf<VisualWord>()

    companion object {
        fun isInGui() = Minecraft.getMinecraft().currentScreen is VisualWordGui
    }

    override fun drawScreen(unusedX: Int, unusedY: Int, partialTicks: Float) {
        super.drawScreen(unusedX, unusedY, partialTicks)
        drawDefaultBackground()
        screenHeight = height
        guiLeft = (width - sizeX) / 2
        guiTop = (height - sizeY) / 2

        mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth
        mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1

        GlStateManager.pushMatrix()
        drawRect(guiLeft, guiTop, guiLeft + sizeX, guiTop + sizeY, 0x50000000)
        val scale = 0.75f
        val inverseScale = 1 / scale

        if (!currentlyEditing) {
            val adjustedY = guiTop + 30 + pageScroll
            var toRemove = -1

            val x = guiLeft + 180
            val y = guiTop + 170

            drawUnmodifiedStringCentered("§aAdd New", x, y)
            val colour =
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
            drawRect(x - 30, y - 10, x + 30, y + 10, colour)

            GlStateManager.scale(scale, scale, 1f)

            drawUnmodifiedStringCentered(
                "§7Modify Words. Replaces the top with the bottom",
                (guiLeft + 180) * inverseScale,
                (guiTop + 9) * inverseScale
            )
            drawUnmodifiedString("§bPhrase", (guiLeft + 30) * inverseScale, (guiTop + 5) * inverseScale)
            drawUnmodifiedString("§bStatus", (guiLeft + 310) * inverseScale, (guiTop + 5) * inverseScale)

            for ((index, phrase) in modifiedWords.withIndex()) {
                if (adjustedY + 30 * index < guiTop + 20) continue
                if (adjustedY + 30 * index > guiTop + 125) continue

                if (phrase.phrase == "" && phrase.replacement == "") {
                    toRemove = index
                }

                var inBox = false
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, adjustedY + 30 * index, sizeX, 30)) inBox = true

                drawUnmodifiedString("${index + 1}.", (guiLeft + 5) * inverseScale, (adjustedY + 10 + 30 * index) * inverseScale)

                if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft + 335, adjustedY + 30 * index + 7, 16, 16)) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    phrase.enabled = !phrase.enabled
                    saveChanges()
                    SoundUtils.playClickSound()
                } else if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft + 295,
                        adjustedY + 30 * index + 7, 16, 16) && index != 0) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    SoundUtils.playClickSound()
                    changedIndex = index
                    changedAction = "up"
                } else if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft + 315,
                        adjustedY + 30 * index + 7, 16, 16) && index != modifiedWords.size - 1) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    SoundUtils.playClickSound()
                    changedIndex = index
                    changedAction = "down"
                } else if (GuiRenderUtils.isPointInRect(lastClickedWidth, lastClickedHeight, guiLeft, adjustedY + 30 * index, sizeX, 30)) {
                    lastClickedWidth = 0
                    lastClickedHeight = 0
                    SoundUtils.playClickSound()
                    currentlyEditing = true
                    currentIndex = index
                }

                if (inBox) {
                    GuiRenderUtils.drawScaledRec(guiLeft, adjustedY + 30 * index, guiLeft + sizeX, adjustedY + 30 * index + 30, 0x50303030, inverseScale)
                }

                val statusBlock = if (phrase.enabled) {
                    ItemStack(Blocks.stained_hardened_clay, 1, 13)
                } else {
                    ItemStack(Blocks.stained_hardened_clay, 1, 14)
                }

                GlStateManager.scale(inverseScale, inverseScale, 1f)

                if (index != 0) {
                    val skullItem = ItemUtils.createSkull("§§Up", "7f68dd73-1ff6-4193-b246-820975d6fab1", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzczMzRjZGRmYWI0NWQ3NWFkMjhlMWE0N2JmOGNmNTAxN2QyZjA5ODJmNjczN2RhMjJkNDk3Mjk1MjUxMDY2MSJ9fX0=")
                    GuiRenderUtils.renderItemAndBackground(skullItem, guiLeft + 295, adjustedY + 30 * index + 7, 0x50828282)
                }
                if (index != modifiedWords.size - 1) {
                    val skullItem = ItemUtils.createSkull("§§Down", "e4ace6de-0629-4719-aea3-3e113314dd3f", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTc3NDIwMzRmNTlkYjg5MGM4MDA0MTU2YjcyN2M3N2NhNjk1YzQzOTlkOGUwZGE1Y2U5MjI3Y2Y4MzZiYjhlMiJ9fX0=")
                    GuiRenderUtils.renderItemAndBackground(skullItem, guiLeft + 315, adjustedY + 30 * index + 7, 0x50828282)
                }

                GuiRenderUtils.renderItemAndBackground(statusBlock, guiLeft + 335, adjustedY + 30 * index + 7, 0x50828282)

                GlStateManager.scale(scale, scale, 1f)

                if (inBox) {
                    drawUnmodifiedString(phrase.phrase, (guiLeft + 15) * inverseScale, (adjustedY + 5 + 30 * index) * inverseScale)
                    drawUnmodifiedString(phrase.replacement, (guiLeft + 15) * inverseScale, (adjustedY + 15 + 30 * index) * inverseScale)
                } else {
                    drawUnmodifiedString(phrase.phrase.convertToFormatted(), (guiLeft + 15) * inverseScale, (adjustedY + 5 + 30 * index) * inverseScale)
                    drawUnmodifiedString(phrase.replacement.convertToFormatted(), (guiLeft + 15) * inverseScale, (adjustedY + 15 + 30 * index) * inverseScale)
                }
            }

            if (modifiedWords.size < 1) {
                modifiedWords = SkyHanniMod.feature.storage.modifiedWords
            }

            if (toRemove != -1) {
                modifiedWords.removeAt(toRemove)
                saveChanges()
            }

            GlStateManager.scale(inverseScale, inverseScale, 1f)

            scrollScreen()
        }
        else {
            val x = guiLeft + 180
            var y = guiTop + 140
            drawUnmodifiedStringCentered("§cDelete", x, y)
            var colour = if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
            drawRect(x - 30, y - 10, x + 30, y + 10, colour)
            y += 30
            drawUnmodifiedStringCentered("§eBack", x, y)
            colour = if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
            drawRect(x - 30, y - 10, x + 30, y + 10, colour)

            if (currentIndex < modifiedWords.size && currentIndex != -1) {
                y -= 150
                val currentPhrase = modifiedWords[currentIndex]
                val status = if (currentPhrase.enabled) "§2Enabled" else "§4Disabled"
                drawUnmodifiedStringCentered(status, x, y)
                colour = if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) 0x50828282 else 0x50303030
                drawRect(x - 30, y - 10, x + 30, y + 10, colour)

                drawUnmodifiedString("§bIs replaced by:", guiLeft + 30, guiTop + 75)

                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 35, sizeX, 30)) {
                    drawRect(guiLeft, guiTop + 35, guiLeft + sizeX, guiTop + 35 + 30, 0x50303030)
                }
                if (currentTextBox == "phrase") {
                    drawRect(guiLeft, guiTop + 35, guiLeft + sizeX, guiTop + 35 + 30, 0x50828282)
                }

                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 90, sizeX, 30)) {
                    drawRect(guiLeft, guiTop + 90, guiLeft + sizeX, guiTop + 90 + 30, 0x50303030)
                }
                if (currentTextBox == "replacement") {
                    drawRect(guiLeft, guiTop + 90, guiLeft + sizeX, guiTop + 90 + 30, 0x50828282)
                }

                GlStateManager.scale(0.75f, 0.75f, 1f)

                drawUnmodifiedString("§bThe top line of each section", (guiLeft + 10) * inverseScale, (guiTop + 12) * inverseScale)
                drawUnmodifiedString("§bis the preview of the bottom text", (guiLeft + 10) * inverseScale, (guiTop + 22) * inverseScale)

                drawUnmodifiedString("§bTo get the Minecraft", (guiLeft + 220) * inverseScale, (guiTop + 12) * inverseScale)
                drawUnmodifiedString("§b formatting character use \"&&\"", (guiLeft + 220) * inverseScale, (guiTop + 22) * inverseScale)

                drawUnmodifiedString(currentPhrase.phrase.convertToFormatted(), (guiLeft + 30) * inverseScale, (guiTop + 40) * inverseScale)
                drawUnmodifiedString(currentPhrase.phrase, (guiLeft + 30) * inverseScale, (guiTop + 55) * inverseScale)

                drawUnmodifiedString(currentPhrase.replacement.convertToFormatted(), (guiLeft + 30) * inverseScale, (guiTop + 95) * inverseScale)
                drawUnmodifiedString(currentPhrase.replacement, (guiLeft + 30) * inverseScale, (guiTop + 110) * inverseScale)

                GlStateManager.scale(inverseScale, inverseScale, 1f)
            }
        }

        if (changedIndex != -1) {
            if (changedAction == "up") {
                if (changedIndex > 0) {
                    val temp = modifiedWords[changedIndex]
                    modifiedWords[changedIndex] = modifiedWords[changedIndex - 1]
                    modifiedWords[changedIndex - 1] = temp
                }
            }
            else if (changedAction == "down") {
                if (changedIndex < modifiedWords.size - 1) {
                    val temp = modifiedWords[changedIndex]
                    modifiedWords[changedIndex] = modifiedWords[changedIndex + 1]
                    modifiedWords[changedIndex + 1] = temp
                }
            }

            changedIndex = -1
            changedAction = ""
            saveChanges()
        }

        GlStateManager.popMatrix()
    }

    override fun handleMouseInput() {
        super.handleMouseInput()

        if (Mouse.getEventButtonState()) {
            mouseClickEvent()
        }
        if (!Mouse.getEventButtonState()) {
            if (Mouse.getEventDWheel() != 0) {
                lastMouseScroll = Mouse.getEventDWheel()
                noMouseScrollFrames = 0
            }
        }
    }

    @Throws(IOException::class)
    fun mouseClickEvent() {
        if (!currentlyEditing) {
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop, sizeX, sizeY - 25)) {
                lastClickedWidth = mouseX
                lastClickedHeight = mouseY
            }
        }
        val x = guiLeft + 180
        var y = guiTop + 140
        if (currentlyEditing) {
            if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) {
                SoundUtils.playClickSound()
                currentlyEditing = false
                modifiedWords.removeAt(currentIndex)
                currentIndex = -1
                saveChanges()
                currentTextBox = ""
            }
            if (currentIndex < modifiedWords.size && currentIndex != -1) {
                y = guiTop + 20
                if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) {
                    SoundUtils.playClickSound()
                    modifiedWords[currentIndex].enabled = !modifiedWords[currentIndex].enabled
                    saveChanges()
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 35, sizeX, 30)) {
                    SoundUtils.playClickSound()
                    currentTextBox = "phrase"
                    currentText = modifiedWords[currentIndex].phrase
                } else if (GuiRenderUtils.isPointInRect(mouseX, mouseY, guiLeft, guiTop + 90, sizeX, 30)) {
                    SoundUtils.playClickSound()
                    currentTextBox = "replacement"
                    currentText = modifiedWords[currentIndex].replacement
                } else {
                    if (currentTextBox != "") {
                        SoundUtils.playClickSound()
                        currentTextBox = ""
                    }
                }
            }
        }
        y = guiTop + 170
        if (GuiRenderUtils.isPointInRect(mouseX, mouseY, x - 30, y - 10, 60, 20)) {
            SoundUtils.playClickSound()
            if (currentlyEditing) {
                currentIndex = -1
                currentTextBox = ""
            } else {
                modifiedWords.add(VisualWord("", "", true))
                currentTextBox = "phrase"
                currentIndex = modifiedWords.size - 1
                saveChanges()
            }
            currentlyEditing = !currentlyEditing
        }
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
        if (!currentlyEditing) return
        if (currentTextBox == "") return
        if (currentIndex >= modifiedWords.size || currentIndex == -1) return

        if (keyCode == Keyboard.KEY_ESCAPE) {
            saveTextChanges()
            currentTextBox = ""
            return
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (currentText.isNotEmpty()) {
                currentText = if (KeyboardManager.isControlKeyDown()) {
                    ""
                } else if (KeyboardManager.isShiftKeyDown()) {
                    val lastSpaceIndex = currentText.lastIndexOf(' ')
                    if (lastSpaceIndex >= 0) {
                        currentText.substring(0, lastSpaceIndex)
                    } else {
                        ""
                    }
                } else {
                    currentText.substring(0, currentText.length - 1)
                }
                saveTextChanges()
            }

            return
        }

        if (currentText.length < maxTextLength && !Character.isISOControl(typedChar)) {
            currentText += typedChar
            saveTextChanges()
            return
        }

        if (KeyboardManager.isPastingKeysDown()) {
            SkyHanniMod.coroutineScope.launch {
                val clipboard = OSUtils.readFromClipboard() ?: ""
                for (char in clipboard) {
                    if (currentText.length < maxTextLength && !Character.isISOControl(char)) {
                        currentText += char
                    }
                }
                saveTextChanges()
            }
        }
    }

    private fun saveTextChanges() {
        if (currentTextBox == "phrase") {
            modifiedWords[currentIndex].phrase = currentText
        } else if (currentTextBox == "replacement") {
            modifiedWords[currentIndex].replacement = currentText
        }
        saveChanges()
    }

    private fun scrollScreen() {
        scrollVelocity += lastMouseScroll / 48.0
        scrollVelocity *= 0.95
        pageScroll += scrollVelocity.toInt() + lastMouseScroll / 24

        noMouseScrollFrames++

        if (noMouseScrollFrames >= maxNoInputFrames) {
            scrollVelocity *= 0.75
        }

        if (pageScroll > 0) {
            pageScroll = 0
        }

        pageScroll = MathHelper.clamp_int(pageScroll, -(SkyHanniMod.feature.storage.modifiedWords.size * 30 - 100), 0)
        lastMouseScroll = 0
    }

    private fun saveChanges() {
        ModifyVisualWords.modifiedWords = modifiedWords
        ModifyVisualWords.textCache.invalidateAll()
        SkyHanniMod.feature.storage.modifiedWords = modifiedWords
    }

    private fun drawUnmodifiedString(str: String, x: Float, y: Float) {
        GuiRenderUtils.drawString("§§$str", x, y)
    }

    private fun drawUnmodifiedString(str: String, x: Int, y: Int) {
        drawUnmodifiedString(str, x.toFloat(), y.toFloat())
    }

    private fun drawUnmodifiedStringCentered(str: String?, x: Int, y: Int) {
        GuiRenderUtils.drawStringCentered("§§$str", x, y)
    }

    private fun drawUnmodifiedStringCentered(str: String?, x: Float, y: Float) {
        drawUnmodifiedStringCentered(str, x.toInt(), y.toInt())
    }
}