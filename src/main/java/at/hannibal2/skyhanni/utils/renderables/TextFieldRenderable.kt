package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.SkyHanniMod
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
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Taken and modified from NotEnoughUpdates.
 */
class TextFieldRenderable(
    initialText: String,
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

    private var focus = false

    private var x = 0
    private var y = 0

    private val fr get() = Minecraft.getMinecraft().fontRendererObj

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

    fun setMaxStringLength(len: Int) {
        textField.maxStringLength = len
    }

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

    override fun toString(): String = textField.text

    fun getFocus(): Boolean = focus

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
        val xComp = mouseX - x
        val yComp = mouseY - y
        val extraSize = (searchBarYSize - 8) / 2 + 8

        val text = textField.text

        val lineNum = ((yComp - (searchBarYSize - 8) / 2) / extraSize.toFloat()).roundToInt()

        val textNoColor = text.replace(Regex("(?i)§."), "")
        val cursorIndex = textNoColor.indexOfNth('\n', lineNum).takeIf { it != -1 } ?: textNoColor.length
        val lines = text.substring(cursorIndex).split('\n')
        if (lines.isEmpty()) return 0

        val line = lines[0]
        val padding = minOf(5, searchBarXSize - strLenNoColor(line)) / 2
        val trimmed = line.capAtMinecraftLength(xComp - padding)
        var linePos = strLenNoColor(trimmed)
        if (linePos < line.length) {
            val after = line[linePos]
            val trimmedWidth = getStringWidth0(trimmed)
            val charWidth = getStringWidth0(after.toString())
            if (trimmedWidth + charWidth / 2 < xComp - padding) linePos++
        }
        return cursorIndex + linePos
    }

    fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 1) {
            textField.text = ""
        } else {
            textField.cursorPosition = getCursorPos(mouseX, mouseY)
        }
        focus = true
    }

    fun otherComponentClick() {
        focus = false
        //#if MC < 1.21
        textField.setSelectionPos(textField.cursorPosition)
        //#else
        //$$ textField.setSelectionEnd(textField.cursor)
        //#endif
    }

    private fun String.indexOfNth(char: Char, n: Int): Int {
        var pos = -1
        var count = 0
        while (count <= n) {
            pos = indexOf(char, pos + 1)
            if (pos == -1) return -1
            count++
        }
        return pos
    }

    private fun strLenNoColor(str: String): Int = str.replace(Regex("(?i)§."), "").length

    fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (focus) {
            //#if MC < 1.21
            textField.setSelectionPos(getCursorPos(mouseX, mouseY))
            //#else
            //$$ textField.setSelectionEnd(getCursorPos(mouseX, mouseY))
            //#endif
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int) {
        if (!focus) return
        val text = textField.text
        val cursor = textField.cursorPosition

        // Cache current line/column info
        val lines = text.split('\n')
        val lineIndex = text.substring(0, cursor).count { it == '\n' }
        val lineStart = text.lineStartIndex(lineIndex)
        val colOffset = getStringWidth0(text.substring(lineStart, cursor))

        when (keyCode) {
            // Ctrl+V (paste)
            Keyboard.KEY_V -> if (KeyboardManager.isModifierKeyDown()) {
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
                    textField.cursorPosition = start + content.length
                }
                return
            }

            // Enter (multiline insert)
            Keyboard.KEY_RETURN -> {
                textField.text = buildString {
                    append(text.substring(0, cursor))
                    append('\n')
                    append(text.substring(cursor))
                }
                textField.cursorPosition = cursor + 1
                return
            }

            // UP arrow
            Keyboard.KEY_UP -> {
                if (lineIndex > 0) {
                    val prevLineStart = text.lineStartIndex(lineIndex - 1)
                    val prevLine = lines[lineIndex - 1]
                    val newCursorOffset = offsetToIndex(prevLine, colOffset)
                    textField.cursorPosition = prevLineStart + newCursorOffset
                }
                return
            }

            // DOWN arrow
            Keyboard.KEY_DOWN -> {
                if (lineIndex < lines.lastIndex) {
                    val nextLineStart = text.lineStartIndex(lineIndex + 1)
                    val nextLine = lines[lineIndex + 1]
                    val newCursorOffset = offsetToIndex(nextLine, colOffset)
                    textField.cursorPosition = nextLineStart + newCursorOffset
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
        for (i in line.indices) {
            width += getStringWidth0(line[i].toString())
            if (width > pixelOffset) return i
        }
        return line.length
    }

    override fun render(posX: Int, posY: Int) {
        this.x = posX
        this.y = posY
        drawTextbox(posX, posY, searchBarXSize, searchBarYSize, searchBarPadding, textField, focus)
    }

    private fun drawTextbox(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        padding: Int,
        textField: GuiTextField,
        focus: Boolean,
    ) {
        val scale = GuiScreenUtils.scaleFactor
        val paddingUnscaled = max(1, padding / scale)

        val renderText = textField.text
        val lines = renderText.split("\n")
        val extraSize = (height - 8) / 2 + 8
        val bottom = y + height + extraSize * (lines.size - 1)

        val borderColor = if (focus) LorenzColor.GREEN.toColor().rgb else LorenzColor.WHITE.toColor().rgb

        // Background & border
        GuiRenderUtils.drawRect(
            x - paddingUnscaled,
            y - paddingUnscaled,
            x + width + paddingUnscaled,
            bottom + paddingUnscaled,
            borderColor,
        )
        GuiRenderUtils.drawRect(x, y, x + width, bottom, LorenzColor.BLACK.toColor().rgb)

        // Process § color codes into sentinel §X§X for coloring
        val pattern = Regex("§([^¶\\n])(?=([^¶\\n])?)")
        val coloredText = pattern.replace(renderText) { "§${it.groupValues[1]}¶${it.groupValues[1]}" }
        val plainText = Regex("§.|¶.").replace(coloredText, "")

        // Draw lines
        val xOffset = 5

        lines.forEachIndexed { i, line ->
            val yOffset = i * extraSize
            val displayLine = line.capAtMinecraftLength(width - 10)
            GuiRenderUtils.drawString(displayLine, x + xOffset, y + (height - 8) / 2 + yOffset)
        }

        // Cursor blink
        if (focus && System.currentTimeMillis() % 1000 > 500) {
            val cursorPos = textField.cursorPosition
            val beforeCursor = plainText.substring(0, cursorPos)
            val lineIndex = beforeCursor.count { it == '\n' }
            val lineText = beforeCursor.split("\n").lastOrNull() ?: ""
            val cursorX = x + xOffset + getStringWidth0(lineText)
            val cursorY = y + (height - 8) / 2 + lineIndex * extraSize
            GuiRenderUtils.drawRect(cursorX, cursorY - 1, cursorX + 1, cursorY + 9, LorenzColor.WHITE.toColor().rgb)
        }

        // Selection highlighting
        val selected = textField.selectedText
        if (selected.isNotEmpty()) {
            val (start, end) = listOf(textField.cursorPosition, textField.selectionEnd).sorted()
            var drawX = 0
            var drawY = 0
            var sectionSign = false
            var bold = false

            plainText.forEachIndexed { i, c ->
                if (c == '¶') {
                    sectionSign = true
                    return@forEachIndexed
                }
                if (sectionSign) {
                    sectionSign = false
                    bold = c.equals('l', ignoreCase = true)
                    return@forEachIndexed
                }

                if (c == '\n') {
                    drawX = 0
                    drawY += extraSize
                    return@forEachIndexed
                }

                val charWidth = getStringWidth0(c.toString()) + if (bold) 1 else 0

                if (i in start until end) {
                    GuiRenderUtils.drawRect(
                        x + xOffset + drawX, y + (height - 8) / 2 - 1 + drawY,
                        x + xOffset + drawX + charWidth, y + (height - 8) / 2 + 9 + drawY,
                        LorenzColor.GRAY.toColor().rgb,
                    )

                    GuiRenderUtils.drawString(
                        c.toString(),
                        x + xOffset + drawX,
                        y + (height - 8) / 2 + drawY,
                    )
                    if (bold) {
                        GuiRenderUtils.drawString(
                            c.toString(),
                            x + xOffset + drawX + 1,
                            y + (height - 8) / 2 + drawY,
                        )
                    }
                }

                drawX += charWidth
            }
        }
    }
}
