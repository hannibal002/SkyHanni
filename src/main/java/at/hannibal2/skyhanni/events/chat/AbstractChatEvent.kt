package at.hannibal2.hanni.events.chat

import at.hannibal2.hanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.hanni.utils.ComponentSpan
import net.minecraft.util.IChatComponent

abstract class AbstractChatEvent(
    val messageComponent: ComponentSpan,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : SystemMessageEvent(messageComponent.getText(), chatComponent, blockedReason) {
    override val message = messageComponent.getText().removePrefix("§r")
}
