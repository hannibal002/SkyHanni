package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzActionBarEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.SeaCreatureFishEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreatureManager
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.event.HoverEvent
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.IChatComponent
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ChatManager {

    private val loggerAll = LorenzLogger("chat/all")
    private val loggerFiltered = LorenzLogger("chat/blocked")
    private val loggerAllowed = LorenzLogger("chat/allowed")
    private val loggerModified = LorenzLogger("chat/modified")
    private val loggerFilteredTypes = mutableMapOf<String, LorenzLogger>()

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onActionBarPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet !is S02PacketChat) return
        val messageComponent = packet.chatComponent

        val message = LorenzUtils.stripVanillaMessage(messageComponent.formattedText)
        if (packet.type.toInt() == 2) {
            val actionBarEvent = LorenzActionBarEvent(message)
            actionBarEvent.postAndCatch()
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatReceive(event: ClientChatReceivedEvent) {
        if (event.type.toInt() == 2) return

        val original = event.message
        val message = LorenzUtils.stripVanillaMessage(original.formattedText)

        if (message.startsWith("§f{\"server\":\"")) return

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
        if (last.startsWith("§f§lCOMMON")) return true
        if (last.startsWith("§a§lUNCOMMON")) return true
        if (last.startsWith("§9§lRARE")) return true
        if (last.startsWith("§5§lEPIC")) return true
        if (last.startsWith("§6§lLEGENDARY")) return true
        if (last.startsWith("§c§lSPECIAL")) return true

        // TODO confirm this format is correct
        if (last.startsWith("§c§lVERY SPECIAL")) return true

        if (last.startsWith("§d§lMYTHIC")) return true

        return false
    }

    @SubscribeEvent
    fun onChatMessage(chatEvent: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock) return

        val seaCreature = SeaCreatureManager.getSeaCreature(chatEvent.message) ?: return
        SeaCreatureFishEvent(seaCreature, chatEvent).postAndCatch()
    }
}