package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ChatManager.deleteChatLine
import at.hannibal2.skyhanni.data.ChatManager.editChatLine
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
//#if TODO
import at.hannibal2.skyhanni.mixins.hooks.ChatLineData
import at.hannibal2.skyhanni.mixins.transformers.AccessorMixinGuiNewChat
//#endif
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.onClick
import at.hannibal2.skyhanni.utils.chat.TextHelper.prefix
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.addChatMessageToChat
import at.hannibal2.skyhanni.utils.compat.command
import at.hannibal2.skyhanni.utils.compat.hover
import at.hannibal2.skyhanni.utils.compat.url
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ChatLine
import net.minecraft.util.IChatComponent
import java.util.LinkedList
import java.util.Queue
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

// todo 1.21 impl needed
@SkyHanniModule
object ChatUtils {

    // TODO log based on chat category (error, warning, debug, user error, normal)
    private val log = LorenzLogger("chat/mod_sent")
    var lastButtonClicked = 0L

    private const val DEBUG_PREFIX = "[SkyHanni Debug] §7"
    private const val USER_ERROR_PREFIX = "§c[SkyHanni] "
    private const val CHAT_PREFIX = "[SkyHanni] "

    /**
     * Sends a debug message to the chat and the console.
     * This is only sent if the debug feature is enabled.
     *
     * @param message The message to be sent
     *
     * @see DEBUG_PREFIX
     */
    fun debug(
        message: String,
        replaceSameMessage: Boolean = false,
    ) {
        val debug = SkyHanniDebugsAndTests.enabled
        if (debug && internalChat(DEBUG_PREFIX + message, replaceSameMessage)) {
            consoleLog("[Debug] $message")
        }
    }

    /**
     * Sends a message to the user that they did something incorrectly.
     * We should tell them what to do instead as well.
     *
     * @param message The message to be sent
     *
     * @see USER_ERROR_PREFIX
     */
    fun userError(
        message: String,
        replaceSameMessage: Boolean = false,
    ) {
        internalChat(USER_ERROR_PREFIX + message, replaceSameMessage)
    }

    /**
     * Sends a message to the user
     * @param message The message to be sent
     * @param prefix Whether to prefix the message with the chat prefix, default true
     * @param prefixColor Color that the prefix should be, default yellow (§e)
     * @param replaceSameMessage Replace the old message with this new message if they are identical
     *
     * @see CHAT_PREFIX
     */
    fun chat(
        message: String,
        prefix: Boolean = true,
        prefixColor: String = "§e",
        replaceSameMessage: Boolean = false,
        onlySendOnce: Boolean = false,
        messageId: Int? = null,
    ) {
        if (prefix) {
            internalChat(prefixColor + CHAT_PREFIX + message, replaceSameMessage, onlySendOnce, messageId = messageId)
        } else {
            internalChat(message, replaceSameMessage, onlySendOnce, messageId = messageId)
        }
    }

    private val messagesThatAreOnlySentOnce = mutableListOf<String>()

    private fun internalChat(
        message: String,
        replaceSameMessage: Boolean,
        onlySendOnce: Boolean = false,
        messageId: Int? = null,
    ): Boolean {
        val text = message.asComponent()
        if (onlySendOnce) {
            if (message in messagesThatAreOnlySentOnce) {
                return false
            }
            messagesThatAreOnlySentOnce.add(message)
        }

        return if (replaceSameMessage || messageId != null) {
            text.send(messageId ?: getUniqueMessageIdForString(message))
            chat(text, false)
        } else {
            chat(text)
        }
    }

    fun chat(message: IChatComponent, send: Boolean = true): Boolean {
        val formattedMessage = message.formattedText
        log.log(formattedMessage)

        if (!MinecraftCompat.localPlayerExists) {
            consoleLog(formattedMessage.removeColor())
            return false
        }

        if (send) addChatMessageToChat(message)
        return true
    }

    /**
     * Sends a message to the user that they can click and run an action
     * @param message The message to be sent
     * @param onClick The runnable to be executed when the message is clicked
     * @param hover The string to be shown when the message is hovered
     * @param expireAt When the click action should expire, default never
     * @param prefix Whether to prefix the message with the chat prefix, default true
     * @param prefixColor Color that the prefix should be, default yellow (§e)
     * @param replaceSameMessage Replace the old message with this new message if they are identical
     *
     * @see CHAT_PREFIX
     */
    fun clickableChat(
        message: String,
        onClick: () -> Unit,
        hover: String = "§eClick here!",
        expireAt: SimpleTimeMark = SimpleTimeMark.farFuture(),
        prefix: Boolean = true,
        prefixColor: String = "§e",
        oneTimeClick: Boolean = false,
        replaceSameMessage: Boolean = false,
    ) {
        val msgPrefix = if (prefix) prefixColor + CHAT_PREFIX else ""

        val rawText = msgPrefix + message
        val text = TextHelper.text(rawText) {
            this.onClick(expireAt, oneTimeClick, onClick)
            this.hover = hover.asComponent()
        }
        if (replaceSameMessage) {
            text.send(getUniqueMessageIdForString(rawText))
        } else {
            chat(text)
        }
    }

    /**
     * Sends the message in chat.
     * Show the lines when on hover.
     * Offer option to click on the chat message to copy the lines to clipboard.
     * Sseful for quick debug infos
     */
    fun clickToClipboard(message: String, lines: List<String>) {
        val text = lines.joinToString("\n") { "§7$it" }
        clickableChat(
            "$message §7(hover for info)",
            hover = "$text\n \n§eClick to copy to clipboard!",
            onClick = {
                ClipboardUtils.copyToClipboard(text.removeColor())
            },
        )
    }

    private val uniqueMessageIdStorage = mutableMapOf<String, Int>()

    // TODO kill Detekt's Missing newline after "{" check and then format this function in a kotlin typical way again
    private fun getUniqueMessageIdForString(string: String): Int {
        return uniqueMessageIdStorage.getOrPut(string) { getUniqueMessageId() }
    }

    private var lastUniqueMessageId = 123242

    fun getUniqueMessageId() = lastUniqueMessageId++

    /**
     * Sends a message to the user that they can click and run a command
     * @param message The message to be sent
     * @param hover The message to be shown when the message is hovered
     * @param command The command to be executed when the message is clicked
     * @param prefix Whether to prefix the message with the chat prefix, default true
     * @param prefixColor Color that the prefix should be, default yellow (§e)
     *
     * @see CHAT_PREFIX
     */
    fun hoverableChat(
        message: String,
        hover: List<String>,
        command: String? = null,
        prefix: Boolean = true,
        prefixColor: String = "§e",
    ) {
        val msgPrefix = if (prefix) prefixColor + CHAT_PREFIX else ""

        chat(
            TextHelper.text(msgPrefix + message) {
                this.hover = TextHelper.multiline(hover)
                if (command != null) {
                    this.command = command
                }
            },
        )
    }

    /**
     * Sends a message to the user that they can click and run a command
     * @param message The message to be sent
     * @param url The url to be opened
     * @param autoOpen Automatically opens the url as well as sending the clickable link message
     * @param hover The message to be shown when the message is hovered
     * @param prefix Whether to prefix the message with the chat prefix, default true
     * @param prefixColor Color that the prefix should be, default yellow (§e)
     *
     * @see CHAT_PREFIX
     */
    fun clickableLinkChat(
        message: String,
        url: String,
        hover: String = "§eOpen $url",
        autoOpen: Boolean = false,
        prefix: Boolean = true,
        prefixColor: String = "§e",
    ) {
        val msgPrefix = if (prefix) prefixColor + CHAT_PREFIX else ""
        chat(
            TextHelper.text(msgPrefix + message) {
                this.url = url
                this.hover = "$prefixColor$hover".asComponent()
            },
        )
        if (autoOpen) OSUtils.openBrowser(url)
    }

    /**
     * Sends a message to the user that combines many message components e.g. clickable, hoverable and regular text
     * @param components The list of components to be joined together to form the final message
     * @param prefix Whether to prefix the message with the chat prefix, default true
     * @param prefixColor Color that the prefix should be, default yellow (§e)
     *
     * @see CHAT_PREFIX
     */
    fun multiComponentMessage(
        components: List<IChatComponent>,
        prefix: Boolean = true,
        prefixColor: String = "§e",
    ) {
        val msgPrefix = if (prefix) prefixColor + CHAT_PREFIX else ""
        chat(TextHelper.join(components).prefix(msgPrefix))
    }

    private val chatGui get() = Minecraft.getMinecraft().ingameGUI.chatGUI

    //#if TODO
    var chatLines: MutableList<ChatLine>
        get() = (chatGui as AccessorMixinGuiNewChat).chatLines_skyhanni
        set(value) {
            (chatGui as AccessorMixinGuiNewChat).chatLines_skyhanni = value
        }

    var drawnChatLines: MutableList<ChatLine>
        get() = (chatGui as AccessorMixinGuiNewChat).drawnChatLines_skyhanni
        set(value) {
            (chatGui as AccessorMixinGuiNewChat).drawnChatLines_skyhanni = value
        }
    //#endif

    /** Edits the first message in chat that matches the given [predicate] to the new [component]. */
    fun editFirstMessage(
        component: (IChatComponent) -> IChatComponent,
        reason: String,
        predicate: (ChatLine) -> Boolean,
    ) {
        //#if TODO
        chatLines.editChatLine(component, predicate, reason)
        refreshChat()
        //#endif
    }

    /**
     * Deletes a maximum of [amount] messages in chat that match the given [predicate].
     */
    fun deleteMessage(
        reason: String,
        amount: Int = 1,
        predicate: (ChatLine) -> Boolean,
    ) {
        //#if TODO
        chatLines.deleteChatLine(amount, reason, predicate)
        refreshChat()
        //#endif
    }

    private fun refreshChat() {
        DelayedRun.onThread.execute {
            chatGui.refreshChat()
        }
    }

    private var deleteNext: Pair<String, (String) -> Boolean>? = null

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onChat(event: SkyHanniChatEvent) {
        val (reason, predicate) = deleteNext ?: return
        this.deleteNext = null

        if (predicate(event.message)) {
            event.blockedReason = reason
        }
    }

    fun deleteNextMessage(
        reason: String,
        predicate: (String) -> Boolean,
    ) {
        deleteNext = reason to predicate
    }

    private var lastMessageSent = SimpleTimeMark.farPast()
    private val sendQueue: Queue<String> = LinkedList()
    private val messageDelay = 300.milliseconds

    fun getTimeWhenNewlyQueuedMessageGetsExecuted() =
        (lastMessageSent + sendQueue.size * messageDelay).takeIf { !it.isInPast() } ?: SimpleTimeMark.now()

    @HandleEvent
    fun onTick() {
        if (lastMessageSent.passedSince() > messageDelay) {
            MinecraftCompat.localPlayer.sendChatMessage(sendQueue.poll() ?: return)
            lastMessageSent = SimpleTimeMark.now()
        }
    }

    fun sendMessageToServer(message: String) {
        if (canSendInstantly()) {
            MinecraftCompat.localPlayerOrNull?.let {
                it.sendChatMessage(message)
                lastMessageSent = SimpleTimeMark.now()
                return
            }
        }
        sendQueue.add(message)
    }

    private fun canSendInstantly() = sendQueue.isEmpty() && lastMessageSent.passedSince() > messageDelay

    fun MessageSendToServerEvent.isCommand(commandWithSlash: String) = splitMessage.takeIf {
        it.isNotEmpty()
    }?.get(0) == commandWithSlash

    fun MessageSendToServerEvent.isCommand(commandsWithSlash: Collection<String>) =
        splitMessage.takeIf { it.isNotEmpty() }?.get(0) in commandsWithSlash

    fun MessageSendToServerEvent.senderIsSkyhanni() = originatingModContainer?.id == "skyhanni"

    fun MessageSendToServerEvent.eventWithNewMessage(message: String) =
        MessageSendToServerEvent(message, message.split(" "), this.originatingModContainer)

    fun chatAndOpenConfig(message: String, property: KMutableProperty0<*>) {
        clickableChat(
            message,
            onClick = { property.jumpToEditor() },
            "§eClick to find setting in the config!",
        )
    }

    fun clickToActionOrDisable(
        message: String,
        option: KMutableProperty0<*>,
        actionName: String,
        action: () -> Unit,
        oneTimeClick: Boolean = false,
    ) {
        clickableChat(
            "$message\n§e[CLICK to $actionName or disable this feature]",
            onClick = {
                if (KeyboardManager.isShiftKeyDown() || KeyboardManager.isModifierKeyDown()) {
                    option.jumpToEditor()
                } else {
                    action()
                }
            },
            hover = "§eClick to $actionName!\n§eShift-Click or Control-Click to disable this feature!",
            oneTimeClick = oneTimeClick,
            replaceSameMessage = true,
        )
    }

    //#if MC < 1.16
    val ChatLine.chatMessage get() = chatComponent.formattedText.stripHypixelMessage()
    var ChatLine.fullComponent: IChatComponent
        get() = (this as ChatLineData).skyHanni_fullComponent
        set(value) {
            (this as ChatLineData).skyHanni_fullComponent = value
        }

    fun ChatLine.passedSinceSent() = (Minecraft.getMinecraft().ingameGUI.updateCounter - updatedCounter).ticks
    //#elseif MC < 1.21
    //$$ val GuiMessage<Component>.chatMessage get() = message.formattedTextCompat().stripHypixelMessage()
    //$$ fun GuiMessage<Component>.passedSinceSent() = (Minecraft.getInstance().gui.guiTicks - addedTime).ticks
    //#else
    //$$ val ChatHudLine.chatMessage get() = content.formattedTextCompat().stripHypixelMessage()
    //$$ fun ChatHudLine.passedSinceSent() = (MinecraftClient.getInstance().inGameHud.ticks - creationTick).ticks
    //#endif

    fun consoleLog(text: String) {
        SkyHanniMod.consoleLog(text)
    }

}
