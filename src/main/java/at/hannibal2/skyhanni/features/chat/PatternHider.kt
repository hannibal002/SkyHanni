package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import net.minecraft.client.Minecraft
import net.minecraft.util.IChatComponent
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PatternHider(val name: String, val pattern: List<Regex>, val isEnabled: () -> Boolean) {
    private val chat = mutableListOf<IChatComponent>()
    private var iterator = pattern.iterator()

    private var ticks = 0
    private val blockedReason = "${name}_pattern_match"

    @SubscribeEvent
    fun onTick(ignored: LorenzTickEvent) {
        if (!isEnabled()) return
        if (chat.isNotEmpty() && ticks <= 0) {
            reset(iterator.hasNext())
        }
        ticks--
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (iterator.hasNext()) {
            val pattern = iterator.next()
            if (pattern.matches(event.message)) {
                ticks = 2
                chat.add(event.chatComponent)
                event.blockedReason = blockedReason
            } else {
                reset(true)
            }
        } else {
            reset(false)
        }
    }

    private fun reset(restoreBlockedChat: Boolean) {
        if (chat.isNotEmpty() && restoreBlockedChat) {
            chat.forEach { Minecraft.getMinecraft().thePlayer.addChatMessage(it) }
        }
        iterator = pattern.iterator()
        chat.clear()
    }
}