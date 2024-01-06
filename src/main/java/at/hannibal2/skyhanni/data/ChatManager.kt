package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.features.chat.ChatFilterGui
import at.hannibal2.skyhanni.utils.IdentityCharacteristics
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.chat
import at.hannibal2.skyhanni.utils.LorenzUtils.makeAccessible
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.gui.GuiNewChat
import net.minecraft.event.HoverEvent
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.ReflectionHelper
import java.lang.invoke.MethodHandles

object ChatManager {

    private val loggerAll = LorenzLogger("chat/all")
    private val loggerFiltered = LorenzLogger("chat/blocked")
    private val loggerAllowed = LorenzLogger("chat/allowed")
    private val loggerModified = LorenzLogger("chat/modified")
    private val loggerFilteredTypes = mutableMapOf<String, LorenzLogger>()
    private val messageHistory =
        object : LinkedHashMap<IdentityCharacteristics<IChatComponent>, MessageFilteringResult>() {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<IdentityCharacteristics<IChatComponent>, MessageFilteringResult>?): Boolean {
                return size > 100
            }
        }

    private fun getRecentMessageHistory(): List<MessageFilteringResult> = messageHistory.toList().map { it.second }

    enum class ActionKind(format: Any) {
        BLOCKED(EnumChatFormatting.RED.toString() + EnumChatFormatting.BOLD),
        RETRACTED(EnumChatFormatting.DARK_PURPLE.toString() + EnumChatFormatting.BOLD),
        MODIFIED(EnumChatFormatting.YELLOW.toString() + EnumChatFormatting.BOLD),
        ALLOWED(EnumChatFormatting.GREEN),
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
        val modified: IChatComponent?
    )

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onActionBarPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet as? S02PacketChat ?: return

        val messageComponent = packet.chatComponent
        val message = LorenzUtils.stripVanillaMessage(messageComponent.formattedText)
        if (packet.type.toInt() == 2) {
            val actionBarEvent = LorenzActionBarEvent(message)
            actionBarEvent.postAndCatch()
        }

    }

    @SubscribeEvent
    fun onSendMessageToServerPacket(event: PacketEvent.SendEvent) {
        val packet = event.packet as? C01PacketChatMessage ?: return

        val message = packet.message
        event.isCanceled = MessageSendToServerEvent(message).postAndCatch()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatReceive(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2) return

        val original = event.message
        val message = LorenzUtils.stripVanillaMessage(original.formattedText)

        if (message.startsWith("§f{\"server\":\"")) return
        val key = IdentityCharacteristics(original)
        val chatEvent = LorenzChatEvent(message, original)
        if (!isSoopyMessage(event.message)) {
            chatEvent.postAndCatch()
        }

        val blockReason = chatEvent.blockedReason.uppercase()
        if (blockReason != "") {
            event.isCanceled = true
            loggerFiltered.log("[$blockReason] $message")
            loggerAll.log("[$blockReason] $message")
            loggerFilteredTypes.getOrPut(blockReason) { LorenzLogger("chat/filter_blocked/$blockReason") }
                .log(message)
            messageHistory[key] = MessageFilteringResult(original, ActionKind.BLOCKED, blockReason, null)
            return
        }

        val modified = chatEvent.chatComponent
        loggerAllowed.log("[allowed] $message")
        loggerAll.log("[allowed] $message")
        if (modified.formattedText != original.formattedText) {
            event.message = chatEvent.chatComponent
            loggerModified.log(" ")
            loggerModified.log("[original] " + original.formattedText)
            loggerModified.log("[modified] " + modified.formattedText)
            messageHistory[key] = MessageFilteringResult(original, ActionKind.MODIFIED, null, modified)
        } else {
            messageHistory[key] = MessageFilteringResult(original, ActionKind.ALLOWED, null, null)
        }

        // TODO: Handle this with ChatManager.retractMessage or some other way for logging and /shchathistory purposes?
        if (chatEvent.chatLineId != 0) {
            event.isCanceled = true
            Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessageWithOptionalDeletion(
                event.message, chatEvent.chatLineId
            )
        }
    }

    private fun isSoopyMessage(message: IChatComponent): Boolean {
        for (sibling in message.siblings) {
            if (isSoopyMessage(sibling)) return true
        }

        val style = message.chatStyle ?: return false
        val hoverEvent = style.chatHoverEvent ?: return false
        if (hoverEvent.action != HoverEvent.Action.SHOW_TEXT) return false
        val text = hoverEvent.value?.formattedText ?: return false

        val lines = text.split("\n")
        if (lines.isEmpty()) return false

        val last = lines.last()
        if (last.startsWith("§f§lCOMMON")) return true
        if (last.startsWith("§a§lUNCOMMON")) return true
        if (last.startsWith("§9§lRARE")) return true
        if (last.startsWith("§5§lEPIC")) return true
        if (last.startsWith("§6§lLEGENDARY")) return true
        if (last.startsWith("§d§lMYTHIC")) return true
        if (last.startsWith("§c§lSPECIAL")) return true

        // TODO confirm this format is correct
        if (last.startsWith("§c§lVERY SPECIAL")) return true
        return false
    }

    fun openChatFilterGUI() {
        SkyHanniMod.screenToOpen = ChatFilterGui(getRecentMessageHistory())
    }

    private val chatLinesField by lazy {
        MethodHandles.publicLookup().unreflectGetter(
            ReflectionHelper.findField(GuiNewChat::class.java, "chatLines", "field_146252_h", "h")
                .makeAccessible()
        )
    }

    fun retractMessage(message: IChatComponent?, reason: String) {
        if (message == null) return
        val chatGUI = Minecraft.getMinecraft().ingameGUI.chatGUI

        @Suppress("UNCHECKED_CAST")
        val chatLines = chatLinesField.invokeExact(chatGUI) as MutableList<ChatLine?>? ?: return
        if (!chatLines.removeIf { it?.chatComponent === message }) return
        chatGUI.refreshChat()

        val history = messageHistory[IdentityCharacteristics(message)] ?: return
        history.actionKind = ActionKind.RETRACTED
        history.actionReason = reason.uppercase()
    }
}
