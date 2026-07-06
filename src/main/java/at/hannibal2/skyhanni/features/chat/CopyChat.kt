package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.fullComponent
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ActiveTextCollector
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.TextAlignment
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence
//? if < 26.1 {
/*import net.minecraft.util.Mth
*///?}
import org.joml.Matrix3x2f

object CopyChat {
    private val config get() = SkyHanniMod.feature.chat.copyChat

    @JvmStatic
    fun handleCopyChat(mouseX: Int, mouseY: Int) {
        try {
            if (!config) return
            processCopyChat(mouseX, mouseY)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(e, "Error while copying chat line")
        }
    }

    private fun processCopyChat(mouseX: Int, mouseY: Int) {
        val chatLine = getChatLine(mouseX, mouseY) ?: return

        val formatted = chatLine.fullComponent.formattedTextCompat()

        val (clipboard, infoMessage) = when {
            KeyboardManager.isMenuKeyDown() ->
                formatted.stripHypixelMessage() to "formatted message"

            KeyboardManager.isShiftKeyDown() -> (
                OrderedTextUtils.orderedTextToLegacyString(ModifyVisualWords.transformText(chatLine.fullComponent.visualOrderText))
                    .removeColor()
                ) to "modified message"

            KeyboardManager.isControlKeyDown() -> chatLine.content.string.removeColor() to "line"

            else -> chatLine.fullComponent.string.removeColor() to "message"
        }

        ClipboardUtils.copyToClipboard(clipboard)
        ChatUtils.chat("Copied $infoMessage to clipboard!")
    }

    private fun getChatLine(mouseX: Int, mouseY: Int): GuiMessage? {
        val mc = Minecraft.getInstance()
        //? if >= 26.1 {
        val chatGui = MinecraftCompat.hud.chat
        val finder = HoveredTextFinder(mc.font, mouseX, mouseY)
        chatGui.captureClickableText(finder, mc.window.guiScaledHeight, MinecraftCompat.hud.guiTicks, ChatComponent.DisplayMode.FOREGROUND)
        val visibleLine = chatGui.trimmedMessages.firstOrNull { it.content === finder.hoveredText } ?: return null

        return visibleLine.parent
        //?} else {
        /*val chatGui = mc.gui.hud.chat ?: return null
        val chatLineY = screenToChatY(mouseY.toDouble())
        val chatLineX = screenToChatX(mouseX.toDouble())
        val lineIndex = (chatGui.chatScrollbarPos + chatLineY).toInt()

        if (chatLineX < -4.0 || chatLineX > Mth.floor(chatGui.width.toDouble() / chatGui.scale).toDouble()) return null

        if (lineIndex < 0) return null
        val visibleLines = chatGui.trimmedMessages
        if (lineIndex > visibleLines.size) return null
        val visibleLine = visibleLines[lineIndex]

        val matchingLines = chatGui.allMessages.filter {
            it.addedTime() == visibleLine.addedTime() && it.content.formattedTextCompat().isNotBlank()
        }

        return when {
            matchingLines.isEmpty() -> null
            matchingLines.size == 1 -> matchingLines.first()
            else -> {
                matchingLines.firstOrNull {
                    it.content.string.removeColor()
                        .contains(OrderedTextUtils.orderedTextToLegacyString(visibleLine.content).removeColor())
                } ?: matchingLines.first()
            }
        }
        *///?}
    }

    //? if < 26.1 {
    /*fun screenToChatX(d: Double): Double {
        val mc = Minecraft.getInstance()
        val chatGui = mc.gui.hud.chat ?: return 0.0
        return d / chatGui.scale - 4.0
    }

    fun screenToChatY(d: Double): Double {
        val mc = Minecraft.getInstance()
        val chatGui = mc.gui.hud.chat ?: return 0.0
        val e = mc.window.guiScaledHeight - d - 40.0
        return e / (chatGui.scale * chatGui.lineHeight)
    }
    *///?}

    private class HoveredTextFinder(
        private val font: Font,
        private val mouseX: Int,
        private val mouseY: Int,
    ) : ActiveTextCollector {
        private var defaultParameters = ActiveTextCollector.Parameters(Matrix3x2f())

        var hoveredText: FormattedCharSequence? = null
            private set

        override fun defaultParameters(): ActiveTextCollector.Parameters = defaultParameters

        override fun defaultParameters(newParameters: ActiveTextCollector.Parameters) {
            defaultParameters = newParameters
        }

        override fun accept(
            alignment: TextAlignment,
            anchorX: Int,
            y: Int,
            parameters: ActiveTextCollector.Parameters,
            text: FormattedCharSequence,
        ) {
            val leftX = alignment.calculateLeft(anchorX, font, text)
            val renderState = GuiTextRenderState(
                font,
                text,
                parameters.pose(),
                leftX,
                y,
                -1,
                0,
                true,
                true,
                parameters.scissor(),
            )
            ActiveTextCollector.findElementUnderCursor(renderState, mouseX.toFloat(), mouseY.toFloat()) {
                hoveredText = text
            }
        }

        override fun acceptScrolling(
            message: Component,
            centerX: Int,
            left: Int,
            right: Int,
            top: Int,
            bottom: Int,
            parameters: ActiveTextCollector.Parameters,
        ) {
            defaultScrollingHelper(message, centerX, left, right, top, bottom, font.width(message), 9, parameters)
        }
    }
}
