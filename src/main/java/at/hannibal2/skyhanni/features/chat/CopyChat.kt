package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.fullComponent
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.ReflectionUtils.getDeclaredFieldOrNull
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.client.GuiMessage
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth

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
        val mc = Minecraft.getInstance() ?: return null
        val chatGui = mc.gui.chat ?: return null
        val chatLineY = chatGui.screenToChatY(mouseY.toDouble())
        val chatLineX = chatGui.screenToChatX(mouseX.toDouble())
        val lineIndex = (chatGui.chatScrollbarPos + chatLineY).toInt()

        if (chatLineX < -4.0 || chatLineX > Mth.floor(chatGui.width.toDouble() / chatGui.scale).toDouble()) return null

        if (lineIndex < 0) return null
        val visibleLines = chatGui.trimmedMessages
        if (lineIndex > visibleLines.size) return null
        val visibleLine = visibleLines[lineIndex]

        val matchingLines = chatGui.allMessages.filter {
            it.addedTime() == visibleLine.addedTime && it.content.formattedTextCompat().isNotBlank()
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
    }

    private val isPatcherLoaded by lazy { PlatformUtils.isModInstalled("patcher") }

    private fun getOffset(): Int {
        if (!isPatcherLoaded) return 0
        return runCatching {
            val patcherConfigClass = Class.forName("club.sk1er.patcher.config.PatcherConfig")
            if (patcherConfigClass.getDeclaredFieldOrNull("chatPosition")?.getBoolean(null) == true) 12 else 0
        }.getOrDefault(0)
    }
}
