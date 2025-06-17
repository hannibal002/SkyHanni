package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.capAtMinecraftLength
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
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
            //$$ GuiTextField(0, fr, 0, 0, net.minecraft.text.Text.empty()).apply {
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
        var cursorIndex = textNoColor.indexOfNth('\n', lineNum).takeIf { it != -1 } ?: textNoColor.length
        val lines = text.substring(cursorIndex).split('\n')
        if (lines.isEmpty()) return 0

        val line = lines[0]
        val padding = minOf(5, searchBarXSize - strLenNoColor(line)) / 2
        val trimmed = fr.trimStringToWidth(line, xComp - padding)
        var linePos = strLenNoColor(trimmed)
        if (linePos < line.length) {
            val after = line[linePos]
            val trimmedWidth = getStringWidth0(trimmed)
            val charWidth = fr.getCharWidth(after)
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
        textField.setSelectionPos(textField.cursorPosition)
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

    fun strLenNoColor(str: String): Int = str.replace(Regex("(?i)§."), "").length

    fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (focus) {
            textField.setSelectionPos(getCursorPos(mouseX, mouseY))
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int) {
        if (!focus) return

        if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            val selectionEnd = textField.selectionEnd
            val cursorPosition = textField.cursorPosition
            val start = minOf(selectionEnd, cursorPosition)
            val end = maxOf(selectionEnd, cursorPosition)

            val content = GuiScreen.getClipboardString()
            val updatedText = StringBuilder(getText()).apply {
                replace(start, end, "")
                insert(start, content)
            }

            textField.text = updatedText.toString()
            textField.cursorPosition = start + content.length
            return
        }

        // Multiline Enter
        if (keyCode == 28) {
            val pos = textField.cursorPosition
            textField.text = buildString {
                append(textField.text.substring(0, pos))
                append('\n')
                append(textField.text.substring(pos))
            }
            textField.cursorPosition = pos + 1
            return
        }

        textField.textboxKeyTyped(typedChar, keyCode)
    }

    override fun render(posX: Int, posY: Int) {
        this.x = posX
        this.y = posY
        drawTextbox(posX, posY, searchBarXSize, searchBarYSize, searchBarPadding, textField, focus)
    }

    fun drawTextbox(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        padding: Int,
        textField: GuiTextField,
        focus: Boolean,
    ) {
        val mc = Minecraft.getMinecraft()
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
            val cursorX = x + xOffset + fr.getStringWidth(lineText)
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

                val charWidth = fr.getStringWidth(c.toString()) + if (bold) 1 else 0

                if (i in start until end) {
                    GuiRenderUtils.drawRect(
                        x + xOffset + drawX, y + (height - 8) / 2 - 1 + drawY,
                        x + xOffset + drawX + charWidth, y + (height - 8) / 2 + 9 + drawY,
                        LorenzColor.GRAY.toColor().rgb,
                    )

                    mc.fontRendererObj.drawString(
                        c.toString(),
                        x + xOffset + drawX,
                        y + (height - 8) / 2 + drawY,
                        LorenzColor.BLACK.toColor().rgb,
                    )
                    if (bold) {
                        mc.fontRendererObj.drawString(
                            c.toString(),
                            x + xOffset + drawX + 1,
                            y + (height - 8) / 2 + drawY,
                            LorenzColor.BLACK.toColor().rgb,
                        )
                    }
                }

                drawX += charWidth
            }
        }
    }
}
