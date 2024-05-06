package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.ConfigUtils.jumpToEditor
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.Text
import at.hannibal2.skyhanni.utils.chat.Text.asComponent
import at.hannibal2.skyhanni.utils.chat.Text.command
import at.hannibal2.skyhanni.utils.chat.Text.hover
import at.hannibal2.skyhanni.utils.chat.Text.onClick
import net.minecraft.client.Minecraft
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.LinkedList
import java.util.Queue
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

object ChatUtils {

    // TODO log based on chat category (error, warning, debug, user error, normal)
    private val log = LorenzLogger("chat/mod_sent")
    var lastButtonClicked = 0L

    private const val DEBUG_PREFIX = "[SkyHanni Debug] §7"
    private const val USER_ERROR_PREFIX = "§c[SkyHanni] "
    private val ERROR_PREFIX by lazy { "§c[SkyHanni-${SkyHanniMod.version}] " }
    private const val CHAT_PREFIX = "[SkyHanni] "

    /**
     * Sends a debug message to the chat and the console.
     * This is only sent if the debug feature is enabled.
     *
     * @param message The message to be sent
     *
     * @see DEBUG_PREFIX
     */
    fun debug(message: String) {
        if (SkyHanniMod.feature.dev.debug.enabled && internalChat(DEBUG_PREFIX + message)) {
            LorenzUtils.consoleLog("[Debug] $message")
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
    fun userError(message: String) {
        internalChat(USER_ERROR_PREFIX + message)
    }

    /**
     * Sends a message to the user that an error occurred caused by something in the code.
     * This should be used for errors that are not caused by the user.
     *
     * Why deprecate this? Even if this message is descriptive for the user and the developer,
     * we don't want inconsistencies in errors, and we would need to search
     * for the code line where this error gets printed any way.
     * so it's better to use the stack trace still.
     *
     * @param message The message to be sent
     *
     * @see ERROR_PREFIX
     */
    @Deprecated(
        "Do not send the user a non clickable non stacktrace containing error message.",
        ReplaceWith("ErrorManager.logErrorStateWithData(message)")
    )
    fun error(message: String) {
        println("error: '$message'")
        internalChat(ERROR_PREFIX + message)
    }

    /**
     * Sends a message to the user
     * @param message The message to be sent
     * @param prefix Whether to prefix the message with the chat prefix, default true
     * @param prefixColor Color that the prefix should be, default yellow (§e)
     *
     * @see CHAT_PREFIX
     */
    fun chat(message: String, prefix: Boolean = true, prefixColor: String = "§e") {
        if (prefix) {
            internalChat(prefixColor + CHAT_PREFIX + message)
        } else {
            internalChat(message)
        }
    }

    fun chatComponent(message: IChatComponent) {
        internalChat(message)
    }

    private fun internalChat(message: String): Boolean {
        return internalChat(ChatComponentText(message))
    }

    private fun internalChat(message: IChatComponent): Boolean {
        val formattedMessage = message.formattedText
        log.log(formattedMessage)

        val minecraft = Minecraft.getMinecraft()
        if (minecraft == null) {
            LorenzUtils.consoleLog(formattedMessage.removeColor())
            return false
        }

        val thePlayer = minecraft.thePlayer
        if (thePlayer == null) {
            LorenzUtils.consoleLog(formattedMessage.removeColor())
            return false
        }

        thePlayer.addChatMessage(message)
        return true
    }

    /**
     * Sends a message to the user that they can click and run an action
     * @param message The message to be sent
     * @param onClick The runnable to be executed when the message is clicked
     * @param expireAt When the click action should expire, default never
     * @param prefix Whether to prefix the message with the chat prefix, default true
     * @param prefixColor Color that the prefix should be, default yellow (§e)
     *
     * @see CHAT_PREFIX
     */
    fun clickableChat(
        message: String,
        onClick: () -> Any,
        expireAt: SimpleTimeMark = SimpleTimeMark.farFuture(),
        prefix: Boolean = true,
        prefixColor: String = "§e",
    ) {
        val msgPrefix = if (prefix) prefixColor + CHAT_PREFIX else ""
        internalChat(Text.text(msgPrefix + message) {
            this.onClick(expireAt, onClick)
            this.hover = "§eClick here!".asComponent()
        })
    }

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

        internalChat(Text.text(msgPrefix + message) {
            this.hover = Text.multiline(hover)
            if (command != null) {
                this.command = command
            }
        })
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
        val text = ChatComponentText(msgPrefix + message)
        text.chatStyle.chatClickEvent = ClickEvent(ClickEvent.Action.OPEN_URL, url)
        text.chatStyle.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("$prefixColor$hover"))
        internalChat(text)
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
        components: List<ChatComponentText>,
        prefix: Boolean = true,
        prefixColor: String = "§e",
    ) {
        val msgPrefix = if (prefix) prefixColor + CHAT_PREFIX else ""
        val baseMessage = ChatComponentText(msgPrefix)

        for (component in components) baseMessage.appendSibling(component)

        internalChat(baseMessage)
    }

    private var lastMessageSent = SimpleTimeMark.farPast()
    private val sendQueue: Queue<String> = LinkedList()
    private val messageDelay = 300.milliseconds

    fun getTimeWhenNewlyQueuedMessageGetsExecuted() =
        (lastMessageSent + sendQueue.size * messageDelay).takeIf { !it.isInPast() } ?: SimpleTimeMark.now()

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        val player = Minecraft.getMinecraft().thePlayer
        if (player == null) {
            sendQueue.clear()
            return
        }
        if (lastMessageSent.passedSince() > messageDelay) {
            player.sendChatMessage(sendQueue.poll() ?: return)
            lastMessageSent = SimpleTimeMark.now()
        }
    }

    fun sendMessageToServer(message: String) {
        sendQueue.add(message)
    }

    @Deprecated("use HypixelCommands instead", ReplaceWith(""))
    fun sendCommandToServer(command: String) {
        if (command.startsWith("/")) {
            debug("Sending wrong command to server? ($command)")
        }
        sendMessageToServer("/$command")
    }

    fun MessageSendToServerEvent.isCommand(commandWithSlash: String) =
        splitMessage.takeIf { it.isNotEmpty() }?.get(0) == commandWithSlash

    fun MessageSendToServerEvent.isCommand(commandsWithSlash: Collection<String>) =
        splitMessage.takeIf { it.isNotEmpty() }?.get(0) in commandsWithSlash

    fun MessageSendToServerEvent.senderIsSkyhanni() = originatingModContainer?.modId == "skyhanni"

    fun MessageSendToServerEvent.eventWithNewMessage(message: String) =
        MessageSendToServerEvent(message, message.split(" "), this.originatingModContainer)

    fun chatAndOpenConfig(message: String, property: KMutableProperty0<*>) {
        clickableChat(
            message,
            onClick = {
                property.jumpToEditor()
            }
        )
    }
}
