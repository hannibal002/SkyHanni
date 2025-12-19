package at.hannibal2.skyhanni.features.chat import at.hannibal2.skyhanni.utils.compat.formattedTextCompat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.misc.visualwords.ModifyVisualWords
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.chatMessage
import at.hannibal2.skyhanni.utils.ChatUtils.fullComponent
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.ReflectionUtils.getDeclaredFieldOrNull
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHudLine
import net.minecraft.util.math.MathHelper
//#if MC < 1.21
//$$ import at.hannibal2.skyhanni.mixins.transformers.AccessorMixinGuiNewChat
//#else
import at.hannibal2.skyhanni.utils.compat.OrderedTextUtils
//#endif

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
        // On 1.8 we use our own code to find the chat lines which uses our mouse methods, on 1.21 we use the vanilla methods
        val chatLine = if (PlatformUtils.IS_LEGACY) {
            getChatLine(MouseCompat.getX(), MouseCompat.getY()) ?: return
        } else {
            getChatLine(mouseX, mouseY) ?: return
        }
        val formatted = chatLine.fullComponent.formattedTextCompat()

        val (clipboard, infoMessage) = when {
            KeyboardManager.isMenuKeyDown() ->
                formatted.stripHypixelMessage() to "formatted message"

            KeyboardManager.isShiftKeyDown() -> (
                //#if MC < 1.21
                //$$ ModifyVisualWords.modifyText(formatted)?.removeColor()
                    //#else
                    OrderedTextUtils.orderedTextToLegacyString(ModifyVisualWords.transformText(chatLine.fullComponent.asOrderedText())).removeColor()
                    //#endif
                    ?: formatted
                ) to "modified message"

            KeyboardManager.isControlKeyDown() -> chatLine.chatMessage.removeColor() to "line"

            else -> formatted.removeColor() to "message"
        }

        ClipboardUtils.copyToClipboard(clipboard)
        ChatUtils.chat("Copied $infoMessage to clipboard!")
    }

    private fun getChatLine(mouseX: Int, mouseY: Int): ChatHudLine? {
        val mc = MinecraftClient.getInstance() ?: return null
        val chatGui = mc.inGameHud.chatHud ?: return null
        //#if MC < 1.21
        //$$ val access = chatGui as AccessorMixinGuiNewChat
        //$$ val chatScale = chatGui.chatScale
        //$$ val scaleFactor = GuiScreenUtils.scaleFactor
        //$$
        //$$ val x = MathHelper.floor((mouseX / scaleFactor - 3) / chatScale)
        //$$ val y = MathHelper.floor((mouseY / scaleFactor - 27 - getOffset()) / chatScale)
        //$$
        //$$ if (x < 0 || y < 0) return null
        //$$
        //$$ val fontHeight = mc.textRenderer.fontHeight
        //$$ val chatLines = access.drawnChatLines_skyhanni
        //$$ val maxLines = chatGui.visibleLineCount.coerceAtMost(chatLines.size)
        //$$ if (x <= MathHelper.floor(chatGui.width.toFloat() / chatGui.chatScale) && y < fontHeight * maxLines + maxLines) {
        //$$     val lineIndex = y / fontHeight + access.scrollPos_skyhanni
        //$$     if (lineIndex in 0 until chatLines.size) {
        //$$         return chatLines[lineIndex]
        //$$     }
        //$$ }
        //$$ return null
        //#else
        val chatLineY = chatGui.toChatLineY(mouseY.toDouble())
        val chatLineX = chatGui.toChatLineX(mouseX.toDouble())
        val lineIndex = (chatGui.scrolledLines + chatLineY).toInt()

        if (chatLineX < -4.0 || chatLineX > MathHelper.floor(chatGui.width.toDouble() / chatGui.chatScale).toDouble()) return null

        if (lineIndex < 0) return null
        val visibleLines = chatGui.visibleMessages
        if (lineIndex > visibleLines.size) return null
        val visibleLine = visibleLines[lineIndex]

        val matchingLines = chatGui.messages.filter {
            it.creationTick == visibleLine.addedTime && it.content.formattedTextCompat().isNotBlank()
        }

        return when {
            matchingLines.isEmpty() -> null
            matchingLines.size == 1 -> matchingLines.first()
            else -> {
                matchingLines.firstOrNull {
                    it.content.formattedTextCompat().stripHypixelMessage().removeColor()
                        .contains(OrderedTextUtils.orderedTextToLegacyString(visibleLine.content).removeColor())
                } ?: matchingLines.first()
            }
        }
        //#endif
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
