package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.features.chat.ChatHistoryGui
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.IdentityCharacteristics
import at.hannibal2.skyhanni.utils.ReflectionUtils.getClassInstance
import at.hannibal2.skyhanni.utils.SkyHanniLogger
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.stripHypixelMessage
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.send
import at.hannibal2.skyhanni.utils.collection.CollectionUtils
import at.hannibal2.skyhanni.utils.compat.append
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
import kotlin.math.floor
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ChatManager {

    private val config get() = SkyHanniMod.feature.dev

    private val loggerAll = SkyHanniLogger("chat/all")
    private val loggerFiltered = SkyHanniLogger("chat/blocked")
    private val loggerAllowed = SkyHanniLogger("chat/allowed")
    private val loggerModified = SkyHanniLogger("chat/modified")
    private val loggerFilteredTypes = mutableMapOf<String, SkyHanniLogger>()

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
    fun onPacketSent(event: PacketSentEvent) {
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
     * If the message is cancelled return true.
     */
    fun onChatAllow(original: Component): Boolean {
        val message = original.formattedTextCompat().stripHypixelMessage()
        var cancelled = false

        if (message.startsWith("§f{\"server\":\"") || message.startsWith("{\"server\":\"")) {
            HypixelData.checkForLocraw(message)
            if (HypixelData.lastLocRaw.passedSince() < 4.seconds) {
                cancelled = true
            }
            return cancelled
        }
        val key = IdentityCharacteristics(original)
        val chatEvent = SkyHanniChatEvent.Allow(message, original)
        chatEvent.post()

        val blockReason = chatEvent.blockedReason.orEmpty().uppercase()
        if (blockReason != "") {
            loggerFiltered.log("[$blockReason] $message")
            loggerAll.log("[$blockReason] $message")
            loggerFilteredTypes.getOrPut(blockReason) { SkyHanniLogger("chat/filter_blocked/$blockReason") }
                .log(message)
            messageHistory[key] = MessageFilteringResult(original, ActionKind.BLOCKED, blockReason, null, null)
            return true
        }

        loggerAllowed.log("[allowed] $message")
        loggerAll.log("[allowed] $message")

        // TODO: Handle this with ChatManager.retractMessage or some other way for logging and /shchathistory purposes?
        if (chatEvent.chatLineId != 0) {
            cancelled = true
            original.send(chatEvent.chatLineId)
        }
        return cancelled
    }

    /**
     * If the message is modified return the modified message otherwise return null.
     */
    fun onChatModify(original: Component): Component? {
        val message = original.formattedTextCompat().stripHypixelMessage()

        val key = IdentityCharacteristics(original)
        val chatEvent = SkyHanniChatEvent.Modify(message, original)
        chatEvent.post()

        val modifiedComponent = chatEvent.chatComponent
        var modified = false
        if (modifiedComponent != original) {
            val reason = replacementReasonMap[key].orEmpty().uppercase()
            modified = true
            loggerModified.log(" ")
            loggerModified.log("[original] " + original.formattedTextCompat())
            loggerModified.log("[modified] " + modifiedComponent.formattedTextCompat())
            messageHistory[key] = MessageFilteringResult(original, ActionKind.MODIFIED, null, modifiedComponent, reason)
        } else {
            messageHistory[key] = MessageFilteringResult(original, ActionKind.ALLOWED, null, null, null)
        }

        return modifiedComponent.takeIf { modified }
    }

    /**
     * Adds canceled messages to /shchathistory if another mod canceled it
     */
    fun onChatCancel(original: Component) {
        val key = IdentityCharacteristics(original)
        if (messageHistory.contains(key)) return
        val blockReason = "OTHER_MOD"
        val message = original.formattedTextCompat().stripHypixelMessage()

        loggerFiltered.log("[$blockReason] $message")
        loggerAll.log("[$blockReason] $message")
        loggerFilteredTypes.getOrPut(blockReason) { SkyHanniLogger("chat/filter_blocked/$blockReason") }
            .log(message)
        messageHistory[key] = MessageFilteringResult(original, ActionKind.BLOCKED, blockReason, null, null)
    }

    /**
     * Added edited messages to /shchathistory if they were edited by another mod
     */
    fun onChatModifyOtherMod(original: Component, modified: Component) {
        val key = IdentityCharacteristics(original)
        val key2 = IdentityCharacteristics(modified)
        if (messageHistory[key2]?.actionKind == ActionKind.ALLOWED && messageHistory[key] == null) {
            loggerModified.log(" ")
            loggerModified.log("[original] " + original.formattedTextCompat())
            loggerModified.log("[modified] " + modified.formattedTextCompat())
            messageHistory[key2] = MessageFilteringResult(original, ActionKind.MODIFIED, null, modified, "OTHER_MOD")
        }
    }

    // TODO add another predicate to stop searching after a certain amount of lines have been
    //  searched or if the lines were sent too long ago. Same thing for the deleteChatMessage
    //  function.
    /**
     * Edits the first message in chat that matches the given [predicate] to the [replacement].
     */
    fun editMessage(
        replacement: (Component) -> Component,
        reason: String? = null,
        predicate: (GuiMessage) -> Boolean = { true },
    ) = DelayedRun.runOrNextTick {
        val mc = Minecraft.getInstance()
        val chatGui = mc.gui.chat

        val (messageIndex, message) = chatGui.allMessages.withIndex().firstOrNull {
            predicate(it.value)
        } ?: return@runOrNextTick
        val counter = message.addedTime()
        val id = message.signature
        val oldComponent = message.content
        val newComponent = replacement(message.content)

        val key = IdentityCharacteristics(oldComponent)

        reason?.let { reason ->
            messageHistory[key]?.let { history ->
                history.modified = newComponent
                history.actionKind = ActionKind.EDITED
                history.actionReason = reason.uppercase()
            }
        }

        val newMessage = GuiMessage(counter, newComponent, id, GuiMessageTag.system())
        chatGui.allMessages[messageIndex] = newMessage

        var targetIndex: Int? = null
        val iterator = chatGui.trimmedMessages.listIterator()
        while (iterator.hasNext()) {
            val lineIndex = iterator.nextIndex()
            val line = iterator.next()
            if (line.`skyhanni$getMessageId`() == message.`skyhanni$getMessageId`()) {
                if (targetIndex == null) targetIndex = lineIndex
                iterator.remove()
            }
        }
        if (targetIndex == null) {
            ErrorManager.logErrorWithData(
                IllegalStateException("Failed to find associated chat lines"),
                "Error while editing message",
                "message" to message,
                "newMessage" to newMessage,
            )
            // Fall back to safe but potentially laggy path
            chatGui.refreshTrimmedMessages()
            return@runOrNextTick
        }
        val maxWidth = floor(chatGui.width / chatGui.scale).toInt()
        //? if < 1.21.11 {
        chatGui.refreshTrimmedMessages()
        throw UnsupportedOperationException("You are running an unsupported development build. Please update to 1.21.11 or above.")
        //? } else
        /*val lines = newMessage.splitLines(mc.font, maxWidth)
        for ((lineIndex, line) in lines.withIndex()) {
            val endOfEntry = lineIndex == lines.size - 1
            val newLine = GuiMessage.Line(newMessage.addedTime(), line, newMessage.tag(), endOfEntry)
            newLine.`skyhanni$setMessageId`(newMessage.`skyhanni$getMessageId`())
            chatGui.trimmedMessages.add(targetIndex++, newLine)
        }*/
    }

    /**
     * Deletes the first message in chat that matches the given [predicate].
     */
    fun deleteMessage(
        reason: String? = null,
        predicate: (GuiMessage) -> Boolean = { true },
    ) = deleteMessages(1, reason, predicate)

    /**
     * Deletes a maximum of [amount] messages in chat that match the given [predicate].
     */
    fun deleteMessages(
        amount: Int,
        reason: String? = null,
        predicate: (GuiMessage) -> Boolean = { true },
    ) = DelayedRun.runOrNextTick {
        val mc = Minecraft.getInstance()
        val chatGui = mc.gui.chat

        val iterator = chatGui.allMessages.iterator()
        var removed = 0
        while (iterator.hasNext() && removed < amount) {
            val message = iterator.next()

            // message can be null. maybe bc of other mods?
            @Suppress("SENSELESS_COMPARISON")
            if (message == null) continue

            if (predicate(message)) {
                iterator.remove()

                val found = chatGui.trimmedMessages.removeIf {
                    it.`skyhanni$getMessageId`() == message.`skyhanni$getMessageId`()
                }
                if (!found) {
                    ErrorManager.logErrorWithData(
                        IllegalStateException("Failed to find associated chat lines"),
                        "Error while deleting message",
                        "message" to message,
                    )
                    // Fall back to safe but potentially laggy path
                    chatGui.refreshTrimmedMessages()
                }

                removed++
                val key = IdentityCharacteristics(message.content)
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
        event.registerBrigadier("shchathistory") {
            description = "Show the unfiltered chat history"
            category = CommandCategory.DEVELOPER_TEST
            argCallback("search", BrigadierArguments.greedyString()) { searchTerm ->
                val history = getRecentMessageHistoryWithSearch(searchTerm)
                if (history.isEmpty()) {
                    ChatUtils.chat("§eNot found in chat history! ($searchTerm)")
                    return@argCallback
                }
                SkyHanniMod.screenToOpen = ChatHistoryGui(history)
            }
            simpleCallback {
                SkyHanniMod.screenToOpen = ChatHistoryGui(getRecentMessageHistory())
            }
        }

        event.registerBrigadier("shtesteditmessage") {
            description = "Test message editing"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback { editMessage(replacement = { it.copy().append(" §8(edited)") }) }
        }

        event.registerBrigadier("shtestdeletemessage") {
            description = "Test message deletion"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback(::deleteMessage)
        }

        event.registerBrigadier("shrefreshchat") {
            description = "Force Minecraft to refresh chat lines"
            category = CommandCategory.DEVELOPER_TEST
            simpleCallback {
                Minecraft.getInstance().gui.chat.refreshTrimmedMessages()
                ChatUtils.chat("Refreshed chat.")
            }
        }
    }
}
