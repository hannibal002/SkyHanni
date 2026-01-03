package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.features.chat.ChatHistoryGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.IdentityCharacteristics
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.ReflectionUtils.getClassInstance
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.system.PlatformUtils.getModInstance
import net.minecraft.ChatFormatting
import net.minecraft.client.GuiMessage
import net.minecraft.client.GuiMessageTag
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import net.minecraft.network.protocol.game.ServerboundChatPacket
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChatManager {

    private val config get() = SkyHanniMod.feature.dev

    private val loggerAll = LorenzLogger("chat/all")
    private val loggerFiltered = LorenzLogger("chat/blocked")
    private val loggerAllowed = LorenzLogger("chat/allowed")
    private val loggerModified = LorenzLogger("chat/modified")
    private val loggerFilteredTypes = mutableMapOf<String, LorenzLogger>()

    private val backingMessageHistory =
        object : LinkedHashMap<IdentityCharacteristics<Component>, MessageFilteringResult>() {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<IdentityCharacteristics<Component>, MessageFilteringResult>?,
            ): Boolean {
                return size > config.chatHistoryLength.coerceAtLeast(0)
            }
        }

    private val messageHistory = CollectionUtils.ObservableMutableMap(
        backingMessageHistory,
        postUpdate = { key, value ->
            if (value == null) replacementReasonMap.remove(key)
        },
    )

    private val replacementReasonMap: MutableMap<IdentityCharacteristics<Component>, String> = mutableMapOf()

    fun addReplacementContext(
        chatComponent: Component,
        reason: String,
    ) = replacementReasonMap.put(
        IdentityCharacteristics(chatComponent),
        reason,
    )

    private fun getRecentMessageHistory(): List<MessageFilteringResult> = messageHistory.toList().map { it.second }

    private fun getRecentMessageHistoryWithSearch(searchTerm: String): List<MessageFilteringResult> =
        messageHistory.toList().map { it.second }
            .filter { it.message.string.removeColor().contains(searchTerm, ignoreCase = true) }

    enum class ActionKind(format: Any) {
        BLOCKED(ChatFormatting.RED.toString() + ChatFormatting.BOLD),
        RETRACTED(ChatFormatting.DARK_PURPLE.toString() + ChatFormatting.BOLD),
        MODIFIED(ChatFormatting.YELLOW.toString() + ChatFormatting.BOLD),
        EDITED(ChatFormatting.GOLD.toString() + ChatFormatting.BOLD),
        ALLOWED(ChatFormatting.GREEN),
        OUTGOING(ChatFormatting.BLUE),
        OUTGOING_BLOCKED(ChatFormatting.BLUE.toString() + ChatFormatting.BOLD),
        ;

        val renderedString = "$format$name"

        companion object {

            val maxLength by lazy {
                entries.maxOf { Minecraft.getInstance().font.width(it.renderedString) }
            }
        }
    }

    data class MessageFilteringResult(
        val message: Component,
        var actionKind: ActionKind,
        var actionReason: String?,
        var modified: Component?,
        var modifiedReason: String?,
        val hoverInfo: List<String> = listOf(),
        val hoverExtraInfo: List<String> = listOf(),
    )

    @HandleEvent
    fun onSendMessageToServerPacket(event: PacketSentEvent) {
        val message = getMessageFromPacket(event.packet) ?: return
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
            component, ActionKind.OUTGOING, null, null, null,
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

    private fun getMessageFromPacket(packet: Packet<*>): String? {
        return when (packet) {
            is ServerboundChatPacket -> packet.message()
            is ServerboundChatCommandPacket -> "/${packet.command}"
            else -> null
        }
    }

    /**
     * If the message is modified return the modified message otherwise return null.
     * If the message is cancelled return true.
     */
    fun onChatReceive(original: Component): Pair<Component?, Boolean> {
        var component = original
        val message = component.formattedTextCompat().stripHypixelMessage()
        var cancelled = false

        if (message.startsWith("§f{\"server\":\"") || message.startsWith("{\"server\":\"")) {
            HypixelData.checkForLocraw(message)
            if (HypixelData.lastLocRaw.passedSince() < 4.seconds) {
                cancelled = true
            }
            return null to cancelled
        }
        val key = IdentityCharacteristics(component)
        val chatEvent = SkyHanniChatEvent(message, component)
        chatEvent.post()

        val blockReason = chatEvent.blockedReason.orEmpty().uppercase()
        if (blockReason != "") {
            loggerFiltered.log("[$blockReason] $message")
            loggerAll.log("[$blockReason] $message")
            loggerFilteredTypes.getOrPut(blockReason) { LorenzLogger("chat/filter_blocked/$blockReason") }
                .log(message)
            messageHistory[key] = MessageFilteringResult(component, ActionKind.BLOCKED, blockReason, null, null)
            return null to true
        }

        val modifiedComponent = chatEvent.chatComponent
        var modified = false
        loggerAllowed.log("[allowed] $message")
        loggerAll.log("[allowed] $message")
        if (modifiedComponent.formattedTextCompat() != component.formattedTextCompat()) {
            val reason = replacementReasonMap[key].orEmpty().uppercase()
            modified = true
            loggerModified.log(" ")
            loggerModified.log("[original] " + component.formattedTextCompat())
            loggerModified.log("[modified] " + modifiedComponent.formattedTextCompat())
            messageHistory[key] = MessageFilteringResult(component, ActionKind.MODIFIED, null, modifiedComponent, reason)
            component = modifiedComponent
        } else {
            messageHistory[key] = MessageFilteringResult(component, ActionKind.ALLOWED, null, null, null)
        }

        // TODO: Handle this with ChatManager.retractMessage or some other way for logging and /shchathistory purposes?
        if (chatEvent.chatLineId != 0) {
            cancelled = true
            component.send(chatEvent.chatLineId)
            // Because we're separately sending the chat line, we don't want to modify the component again,
            // even if we "meant" to replace the component.
            modified = false
        }
        return Pair(component.takeIf { modified }, cancelled)
    }

    private fun openChatHistoryGui(args: Array<String>) {
        SkyHanniMod.screenToOpen = if (args.isEmpty()) {
            ChatHistoryGui(getRecentMessageHistory())
        } else {
            val searchTerm = args.joinToString(" ")
            val history = getRecentMessageHistoryWithSearch(searchTerm)
            if (history.isEmpty()) {
                ChatUtils.chat("§eNot found in chat history! ($searchTerm)")
                return
            }
            ChatHistoryGui(history)
        }
    }

    // TODO: Add another predicate to stop searching after a certain amount of lines have been searched
    //  or if the lines were sent too long ago. Same thing for the deleteChatLine function.
    fun MutableList<GuiMessage>.editChatLine(
        component: (Component) -> Component,
        predicate: (GuiMessage) -> Boolean,
        reason: String? = null,
    ) {
        DelayedRun.onThread.execute {
            indexOfFirst {
                predicate(it)
            }.takeIf { it != -1 }?.let {
                val chatLine = this[it]
                val counter = chatLine.addedTime()
                val id = chatLine.signature
                val oldComponent = chatLine.content
                val newComponent = component(chatLine.content)

                val key = IdentityCharacteristics(oldComponent)

                reason?.let { reason ->
                    messageHistory[key]?.let { history ->
                        history.modified = newComponent
                        history.actionKind = ActionKind.EDITED
                        history.actionReason = reason.uppercase()
                    }
                }
                this[it] = GuiMessage(counter, newComponent, id, GuiMessageTag.system())
            }
        }
    }

    fun MutableList<GuiMessage>.deleteChatLine(
        amount: Int,
        reason: String? = null,
        predicate: (GuiMessage) -> Boolean,
    ) {
        DelayedRun.onThread.execute {
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
                    val key = IdentityCharacteristics(chatLine.content)
                    reason?.let {
                        messageHistory[key]?.let { history ->
                            history.actionKind = ActionKind.RETRACTED
                            history.actionReason = it.uppercase()
                        }
                    }
                }
            }
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shchathistory") {
            description = "Show the unfiltered chat history"
            category = CommandCategory.DEVELOPER_TEST
            legacyCallbackArgs { openChatHistoryGui(it) }
        }
    }
}
