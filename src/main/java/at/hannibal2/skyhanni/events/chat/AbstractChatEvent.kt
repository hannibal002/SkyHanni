package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.util.IChatComponent

abstract class AbstractChatEvent(
    val messageComponent: ComponentSpan,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : SystemMessageEvent(messageComponent.getText(), chatComponent, blockedReason) {
    override val message = messageComponent.getText().removePrefix("§r")
}
