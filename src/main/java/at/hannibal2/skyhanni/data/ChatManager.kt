package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.features.chat.ChatFilterGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.IdentityCharacteristics
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.ReflectionUtils.getClassInstance
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.system.PlatformUtils.getModInstance
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ChatLine
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent
import kotlin.time.Duration.Companion.seconds

//#if MC > 1.21
//$$ import net.minecraft.client.gui.hud.MessageIndicator
//#endif

@SkyHanniModule
object ChatManager {

    private val config get() = SkyHanniMod.feature.dev

    private val loggerAll = LorenzLogger("chat/all")
    private val loggerFiltered = LorenzLogger("chat/blocked")
    private val loggerAllowed = LorenzLogger("chat/allowed")
    private val loggerModified = LorenzLogger("chat/modified")
    private val loggerFilteredTypes = mutableMapOf<String, LorenzLogger>()
    private val messageHistory =
        object : LinkedHashMap<IdentityCharacteristics<IChatComponent>, MessageFilteringResult>() {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<IdentityCharacteristics<IChatComponent>, MessageFilteringResult>?,
            ): Boolean {
                return size > config.chatHistoryLength.coerceAtLeast(0)
            }
        }

    private fun getRecentMessageHistory(): List<MessageFilteringResult> = messageHistory.toList().map { it.second }

    private fun getRecentMessageHistoryWithSearch(searchTerm: String): List<MessageFilteringResult> =
        messageHistory.toList().map { it.second }
            .filter { it.message.formattedText.removeColor().contains(searchTerm, ignoreCase = true) }

    enum class ActionKind(format: Any) {
        BLOCKED(EnumChatFormatting.RED.toString() + EnumChatFormatting.BOLD),
        RETRACTED(EnumChatFormatting.DARK_PURPLE.toString() + EnumChatFormatting.BOLD),
        MODIFIED(EnumChatFormatting.YELLOW.toString() + EnumChatFormatting.BOLD),
        EDITED(EnumChatFormatting.GOLD.toString() + EnumChatFormatting.BOLD),
        ALLOWED(EnumChatFormatting.GREEN),
        OUTGOING(EnumChatFormatting.BLUE),
        OUTGOING_BLOCKED(EnumChatFormatting.BLUE.toString() + EnumChatFormatting.BOLD),
        ;

        val renderedString = "$format$name"

        companion object {

            val maxLength by lazy {
                entries.maxOf { Minecraft.getMinecraft().fontRendererObj.getStringWidth(it.renderedString) }
            }
        }
    }

    data class MessageFilteringResult(
        val message: IChatComponent,
        var actionKind: ActionKind,
        var actionReason: String?,
        var modified: IChatComponent?,
        val hoverInfo: List<String> = listOf(),
        val hoverExtraInfo: List<String> = listOf(),
    )

    @HandleEvent
    fun onSendMessageToServerPacket(event: PacketSentEvent) {
        val packet = event.packet as? C01PacketChatMessage ?: return

        val message = packet.message
        val component = message.asComponent()
        val originatingModCall = event.findOriginatingModCall()
        val originatingModContainer = originatingModCall?.getClassInstance()?.getModInstance()
        val hoverInfo = listOf(
            "§7Message created by §a${originatingModCall?.toString() ?: "§cprobably minecraft"}",
            "§7Mod id: §a${originatingModContainer?.id}",
            "§7Mod name: §a${originatingModContainer?.name}",
        )
        val stackTrace =
            Thread.currentThread().stackTrace.map {
                "§7  §2${it.className}§7.§a${it.methodName}§7" +
                    if (it.fileName == null) "" else "(§b${it.fileName}§7:§3${it.lineNumber}§7)"
            }
        val result = MessageFilteringResult(
            component, ActionKind.OUTGOING, null, null,
            hoverInfo = hoverInfo,
            hoverExtraInfo = hoverInfo + listOf("") + stackTrace,
        )

        messageHistory[IdentityCharacteristics(component)] = result
        val trimmedMessage = message.trimEnd()
        if (MessageSendToServerEvent(
                trimmedMessage,
                trimmedMessage.split(" "),
                originatingModContainer,
            ).post()
        ) {
            event.cancel()
            messageHistory[IdentityCharacteristics(component)] = result.copy(actionKind = ActionKind.OUTGOING_BLOCKED)
        }
    }

    /**
     * If the message is modified return the modified message otherwise return null.
     * If the message is cancelled return true.
     */
    fun onChatReceive(original: IChatComponent): Pair<IChatComponent?, Boolean> {
        var component = original
        val message = component.formattedText.stripHypixelMessage()
        var cancelled = false

        if (message.startsWith("§f{\"server\":\"")) {
            HypixelData.checkForLocraw(message)
            if (HypixelData.lastLocRaw.passedSince() < 4.seconds) {
                cancelled = true
            }
            return null to cancelled
        }
        val key = IdentityCharacteristics(component)
        val chatEvent = SkyHanniChatEvent(message, component)
        chatEvent.post()

        val blockReason = chatEvent.blockedReason.uppercase()
        if (blockReason != "") {
            loggerFiltered.log("[$blockReason] $message")
            loggerAll.log("[$blockReason] $message")
            loggerFilteredTypes.getOrPut(blockReason) { LorenzLogger("chat/filter_blocked/$blockReason") }
                .log(message)
            messageHistory[key] = MessageFilteringResult(component, ActionKind.BLOCKED, blockReason, null)
            return null to true
        }

        val eventComponent = chatEvent.chatComponent
        var modified = false
        loggerAllowed.log("[allowed] $message")
        loggerAll.log("[allowed] $message")
        if (eventComponent.formattedText != component.formattedText) {
            modified = true
            component = chatEvent.chatComponent
            loggerModified.log(" ")
            loggerModified.log("[original] " + component.formattedText)
            loggerModified.log("[modified] " + eventComponent.formattedText)
            messageHistory[key] = MessageFilteringResult(component, ActionKind.MODIFIED, null, eventComponent)
        } else {
            messageHistory[key] = MessageFilteringResult(component, ActionKind.ALLOWED, null, null)
        }

        // TODO: Handle this with ChatManager.retractMessage or some other way for logging and /shchathistory purposes?
        if (chatEvent.chatLineId != 0) {
            cancelled = true
            component.send(chatEvent.chatLineId)
        }
        return Pair(component.takeIf { modified }, cancelled)
    }

    private fun openChatFilterGUI(args: Array<String>) {
        SkyHanniMod.screenToOpen = if (args.isEmpty()) {
            ChatFilterGui(getRecentMessageHistory())
        } else {
            val searchTerm = args.joinToString(" ")
            val history = getRecentMessageHistoryWithSearch(searchTerm)
            if (history.isEmpty()) {
                ChatUtils.chat("§eNot found in chat history! ($searchTerm)")
                return
            }
            ChatFilterGui(history)
        }
    }

    // TODO: Add another predicate to stop searching after a certain amount of lines have been searched
    //  or if the lines were sent too long ago. Same thing for the deleteChatLine function.
    fun MutableList<ChatLine>.editChatLine(
        component: (IChatComponent) -> IChatComponent,
        predicate: (ChatLine) -> Boolean,
        reason: String? = null,
    ) {
        indexOfFirst {
            predicate(it)
        }.takeIf { it != -1 }?.let {
            val chatLine = this[it]
            //#if MC < 1.21
            val counter = chatLine.updatedCounter
            val id = chatLine.chatLineID
            val oldComponent = chatLine.chatComponent
            val newComponent = component(chatLine.chatComponent)
            //#else
            //$$ val counter = chatLine.creationTick
            //$$ val id = chatLine.signature
            //$$ val oldComponent = chatLine.content
            //$$ val newComponent = component(chatLine.content)
            //#endif

            val key = IdentityCharacteristics(oldComponent)

            reason?.let { reason ->
                messageHistory[key]?.let { history ->
                    history.modified = newComponent
                    history.actionKind = ActionKind.EDITED
                    history.actionReason = reason.uppercase()
                }
            }

            //#if MC < 1.21
            this[it] = ChatLine(counter, newComponent, id)
            //#else
            //$$ this[it] = ChatHudLine(counter, newComponent, id, MessageIndicator.system())
            //#endif
        }
    }

    fun MutableList<ChatLine>.deleteChatLine(
        amount: Int,
        reason: String? = null,
        predicate: (ChatLine) -> Boolean,
    ) {
        val iterator = iterator()
        var removed = 0
        while (iterator.hasNext() && removed < amount) {
            val chatLine = iterator.next()

            // chatLine can be null. maybe bc of other mods?
            @Suppress("SENSELESS_COMPARISON")
            if (chatLine == null) continue

            if (predicate(chatLine)) {
                iterator.remove()
                removed++
                //#if MC < 1.21
                val key = IdentityCharacteristics(chatLine.chatComponent)
                //#else
                //$$ val key = IdentityCharacteristics(chatLine.content)
                //#endif
                reason?.let {
                    messageHistory[key]?.let { history ->
                        history.actionKind = ActionKind.RETRACTED
                        history.actionReason = it.uppercase()
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.register("shchathistory") {
            description = "Show the unfiltered chat history"
            category = CommandCategory.DEVELOPER_TEST
            callback { openChatFilterGUI(it) }
        }
    }
}
