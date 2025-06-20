package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.capAtMinecraftLength
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import kotlin.math.min

/**
 * Taken and modified from NotEnoughUpdates.
 */
class TextFieldRenderable(
    initialText: String = "",
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.CENTER,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
) : Renderable {

    override val width: Int
        get() = calculateWidth()

    override val height: Int
        get() = calculateHeight()

    private var searchBarYSize = 20
    private var searchBarXSize = 350
    private val searchBarPadding = 2

    private var isFocussed = false

    private var xOffset = 0
    private var yOffset = 0

    private val fr get() = Minecraft.getMinecraft().fontRendererObj

    private fun moveCursor(pos: Int) {
        //#if MC < 1.21
        textField.cursorPosition = pos
        //#else
        //$$ textField.setCursor(pos, KeyboardManager.isShiftKeyDown())
        //#endif
    }

    private val textField =
        //#if MC < 1.21
        GuiTextField(0, fr, 0, 0, 0, 0).apply {
            //#else
            //$$ TextFieldWidget(fr, 0, 0, net.minecraft.text.Text.empty()).apply {
            //#endif
            setFocused(true)
            setCanLoseFocus(false)
            setMaxStringLength(9999)
            text = initialText
        }

    private var customBorderColour = -1

    fun setCustomBorderColour(colour: Int) {
        this.customBorderColour = colour
    }

    fun getText(): String = textField.text

    fun setText(text: String) {
        if (textField.text != text) {
            textField.text = text
        }
    }

    fun setSize(width: Int, height: Int) {
        searchBarXSize = width
        searchBarYSize = height
    }

    fun setOffset(xOffset: Int, yOffset: Int) {
        this.xOffset = xOffset
        this.yOffset = yOffset
    }

    override fun toString(): String = textField.text

    private fun calculateHeight(): Int {
        val scale = GuiScreenUtils.scaleFactor
        val padding = searchBarPadding / scale
        val lines = textField.text.count { it == '\n' } + 1
        val extraSize = (searchBarYSize - 8) / 2 + 8
        return searchBarYSize + extraSize * (lines - 1) + padding * 2
    }

    private fun calculateWidth(): Int {
        val scale = GuiScreenUtils.scaleFactor
        return searchBarXSize + (searchBarPadding / scale) * 2
    }

    private fun getStringWidth0(str: String) = fr.getStringWidth(str)

    private fun getCursorPos(mouseX: Int, mouseY: Int): Int {
        val xComp: Int = mouseX - xOffset
        val yComp: Int = mouseY - yOffset

        val extraSize = (searchBarYSize - 8) / 2 + 8

        val lineNum = Math.round((((yComp - (searchBarYSize - 8) / 2)) / extraSize).toFloat())
        val text = textField.text.replacePatternControlCodes()
        val textNoColor = textField.text.replacePatternControlCodesNoColors()

        var currentLine = 0
        var cursorIndex = 0
        while (cursorIndex < textNoColor.length) {
            if (currentLine == lineNum) break
            if (textNoColor[cursorIndex] == '\n') {
                currentLine++
            }
            cursorIndex++
        }

        val textNC = textNoColor.substring(0, cursorIndex)
        val colorCodes = textNC.count { it == '¶' }
        val lines =
            text.substring(cursorIndex + (colorCodes * 2)).split("\n".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
        if (lines.isEmpty()) {
            return 0
        }
        val line = lines[0]

        val padding = (min(5.0, (searchBarXSize - strLenNoColor(line)).toDouble()) / 2).toInt()
        val trimmed = line.capAtMinecraftLength(xComp - padding)
        var linePos = strLenNoColor(trimmed)
        if (linePos != strLenNoColor(line)) {
            val after = line[linePos]
            val trimmedWidth: Int = getStringWidth0(trimmed)
            val charWidth = getStringWidth0(after.toString())
            if (trimmedWidth + charWidth / 2 < xComp - padding) {
                linePos++
            }
        }
        cursorIndex += linePos

        return cursorIndex
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (!GuiRenderUtils.isPointInRect(mouseX, mouseY, xOffset, yOffset, width, height)) {
            isFocussed = false
            //#if MC < 1.21
            textField.setSelectionPos(textField.cursorPosition)
            //#else
            //$$ textField.setSelectionEnd(textField.cursor)
            //#endif
            return
        }

        if (mouseButton == 1) {
            textField.text = ""
        } else {
            moveCursor(getCursorPos(mouseX, mouseY))
        }
        isFocussed = true
    }

    private fun strLenNoColor(str: String): Int = str.replace(Regex("(?i)§."), "").length

    fun mouseClickMove(mouseX: Int, mouseY: Int) {
        if (isFocussed) {
            //#if MC < 1.21
            textField.setSelectionPos(getCursorPos(mouseX, mouseY))
            //#else
            //$$ textField.setSelectionEnd(getCursorPos(mouseX, mouseY))
            //#endif
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int) {
        if (!isFocussed) return
        val text = textField.text
        val cursor = textField.cursorPosition

        val lines = text.split('\n')
        val lineIndex = text.substring(0, cursor).count { it == '\n' }
        val lineStart = text.lineStartIndex(lineIndex)
        val colOffset = getStringWidth0(text.substring(lineStart, cursor).replace("§", "¶"))

        if (keyCode != Keyboard.KEY_V) {
            //#if MC < 1.21
            textField.setEnabled(true)
            //#else
            //$$ textField.setEditable(true)
            //#endif
        }

        when (keyCode) {
            Keyboard.KEY_V -> if (KeyboardManager.isModifierKeyDown()) {
                //#if MC < 1.21
                textField.setEnabled(false)
                //#else
                //$$ textField.setEditable(false)
                //#endif
                val selectionEnd = textField.selectionEnd
                val cursorPosition = textField.cursorPosition
                val start = minOf(selectionEnd, cursorPosition)
                val end = maxOf(selectionEnd, cursorPosition)

                SkyHanniMod.coroutineScope.launch {
                    val content = OSUtils.readFromClipboard() ?: return@launch
                    val updatedText = StringBuilder(text).apply {
                        replace(start, end, "")
                        insert(start, content)
                    }

                    textField.text = updatedText.toString()
                    moveCursor(start + content.length)
                }
                return
            }

            Keyboard.KEY_RETURN -> {
                textField.text = buildString {
                    append(text.substring(0, cursor))
                    append('\n')
                    append(text.substring(cursor))
                }
                moveCursor(cursor + 1)
                return
            }

            Keyboard.KEY_UP -> {
                if (lineIndex > 0) {
                    val prevLineStart = text.lineStartIndex(lineIndex - 1)
                    val prevLine = lines[lineIndex - 1]
                    val newCursorOffset = offsetToIndex(prevLine, colOffset)
                    moveCursor(prevLineStart + newCursorOffset)
                } else {
                    moveCursor(0)
                }
                return
            }

            Keyboard.KEY_DOWN -> {
                if (lineIndex < lines.lastIndex) {
                    val nextLineStart = text.lineStartIndex(lineIndex + 1)
                    val nextLine = lines[lineIndex + 1]
                    val newCursorOffset = offsetToIndex(nextLine, colOffset)
                    moveCursor(nextLineStart + newCursorOffset)
                } else {
                    moveCursor(text.length)
                }
                return
            }
        }

        //#if MC < 1.21
        textField.textboxKeyTyped(typedChar, keyCode)
        //#else
        //$$ if (keyCode != 0) {
        //$$     textField.keyPressed(keyCode, 0, 0)
        //$$ } else {
        //$$     textField.charTyped(typedChar, 0)
        //$$ }
        //#endif
        if (keyCode == Keyboard.KEY_LEFT || keyCode == Keyboard.KEY_RIGHT) {
            return
        }

        val cursorPos = textField.cursorPosition
        if (cursorPos > 0) {
            if (getText()[cursorPos - 1] == '§') {
                val before = textField.text.substring(0, cursorPos - 1)
                var after = ""
                if (cursorPos < textField.text.length) {
                    after = textField.text.substring(cursorPos)
                }
                textField.text = "$before&&$after"
                moveCursor(cursorPos + 1)
            } else if (cursorPos > 2 && getText().substring(cursorPos - 3, cursorPos - 1) == "&&") {
                val before = textField.text.substring(0, cursorPos - 3)
                var after = "$typedChar"
                if (cursorPos < textField.text.length) {
                    after = textField.text.substring(cursorPos - 1)
                }
                textField.text = "$before§$after"
                moveCursor(cursorPos - 1)
            }
        }
    }

    private fun String.lineStartIndex(line: Int): Int {
        if (line == 0) return 0
        var count = 0
        var index = 0
        while (count < line) {
            val next = indexOf('\n', index)
            if (next == -1) return length
            index = next + 1
            count++
        }
        return index
    }

    private fun offsetToIndex(line: String, pixelOffset: Int): Int {
        var width = 0
        val newLine = line.replace("§", "¶")
        for (i in newLine.indices) {
            width += getStringWidth0(newLine[i].toString())
            if (width > pixelOffset) return i
        }
        return newLine.length
    }

    override fun render(posX: Int, posY: Int) {
        try {
            drawTextbox(searchBarXSize, searchBarYSize, textField, isFocussed)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error rendering TextFieldRenderable")
        }
    }

    private fun drawTextbox(width: Int, height: Int, textField: GuiTextField, focus: Boolean) {
        val renderText = textField.text
        val lineCount = renderText.count { it == '\n' } + 1
        val extraSize = (height - 8) / 2 + 8
        val bottom = height + extraSize * (lineCount - 1)

        var borderColor = if (focus) LorenzColor.GREEN.toColor().rgb else LorenzColor.WHITE.toColor().rgb
        if (customBorderColour != -1) {
            borderColor = customBorderColour
        }

        // Background & border
        GuiRenderUtils.drawRect(-1, -1, width + 1, bottom + 1, borderColor)
        GuiRenderUtils.drawRect(0, 0, width, bottom, LorenzColor.BLACK.toColor().rgb)

        // Bar text
        val text = renderText.replacePatternControlCodes()
        val textNoColor = textField.text.replacePatternControlCodesNoColors()

        val xStartOffset = 5
        text.lines().forEachIndexed { i, line ->
            val yOffset = i * extraSize
            val displayLine = line.capAtMinecraftLength(width - 10)
            GuiRenderUtils.drawString(displayLine, xStartOffset, (height - 8) / 2 + yOffset)
        }

        if (focus && System.currentTimeMillis() % 1000 > 500) {
            val textNC = textNoColor.substring(0, textField.cursorPosition)
            val colorCodes = textNC.count { it == '¶' }
            val beforeCursor = text.substring(0, textField.cursorPosition + (colorCodes * 2))
            val lineIndex = beforeCursor.count { it == '\n' }
            val lineText = beforeCursor.split("\n").lastOrNull() ?: ""
            val cursorX = xStartOffset + getStringWidth0(lineText)
            val cursorY = (height - 8) / 2 + lineIndex * extraSize
            GuiRenderUtils.drawRect(cursorX, cursorY - 1, cursorX + 1, cursorY + 9, LorenzColor.WHITE.toColor().rgb)
        }

        // Selection highlighting
        val selected = textField.selectedText
        if (selected.isEmpty()) return

        val (start, end) = listOf(textField.cursorPosition, textField.selectionEnd).sorted()

        var texX = 0
        var texY = 0
        var sectionSignPrev = false
        var bold = false

        for ((i, c) in textNoColor.withIndex()) {
            if (sectionSignPrev) {
                val lowercase = c.lowercaseChar()
                if (lowercase !in nonBoldFormattingCodes) {
                    bold = lowercase == 'l'
                }
            }
            sectionSignPrev = c == '¶'

            if (c == '\n') {
                if (i in start until end) {
                    GuiRenderUtils.drawRect(
                        xStartOffset + texX, (height - 8) / 2 - 1 + texY,
                        xStartOffset + texX + 3, (height - 8) / 2 + 9 + texY,
                        LorenzColor.GRAY.toColor().rgb,
                    )
                }

                texX = 0
                texY += extraSize
                continue
            }

            val charWidth = getStringWidth0(c.toString()) + if (bold) 1 else 0

            if (i in start until end) {
                GuiRenderUtils.drawRect(
                    xStartOffset + texX, (height - 8) / 2 - 1 + texY,
                    xStartOffset + (texX + charWidth), (height - 8) / 2 + 9 + texY,
                    LorenzColor.GRAY.toColor().rgb,
                )
                GuiRenderUtils.drawString(
                    c.toString(),
                    xStartOffset + texX,
                    (height - 8) / 2 + texY,
                )
                if (bold) {
                    GuiRenderUtils.drawString(
                        c.toString(),
                        xStartOffset + texX + 1,
                        (height - 8) / 2 + texY,
                    )
                }
            }

            texX += charWidth
        }
    }

    companion object {
        private val patternControlCode = "(?i)§([^¶])(?!¶)".toPattern()
        private val nonBoldFormattingCodes = listOf('k', 'm', 'n', 'o')

        private fun String.replacePatternControlCodes(): String {
            return patternControlCode.matcher(this).replaceAll("§$1¶$1")
        }

        private fun String.replacePatternControlCodesNoColors(): String {
            return patternControlCode.matcher(this).replaceAll("¶$1")
        }
    }
}
