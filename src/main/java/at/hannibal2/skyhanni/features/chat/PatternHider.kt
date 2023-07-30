package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PatternHider(
    val manager: PatternHiderManager,
    val name: String,
    val pattern: List<Regex>,
    val isEnabled: () -> Boolean
) {
    private var iterator = pattern.iterator()
    private val bufferedChat = mutableListOf<IChatComponent>()

    private var ticks = 0
    private val blockedReason = "${name}_pattern_match"

    @SubscribeEvent
    fun onTick(ignored: LorenzTickEvent) {
        if (!isEnabled()) return
        if (bufferedChat.isNotEmpty() && ticks <= 0) {
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
                block(event)
            } else reset(true)
        } else reset(false)
    }

    private fun block(event: LorenzChatEvent) {
        bufferedChat.add(event.chatComponent)
        manager.storeChat(event.chatComponent)
        event.blockedReason = blockedReason
        ticks = 2
    }

    private fun reset(restoreBlockedChat: Boolean) {
        bufferedChat.forEach {
            manager.removeChat(it, restoreBlockedChat)
        }
        iterator = pattern.iterator()
        bufferedChat.clear()
    }
}
