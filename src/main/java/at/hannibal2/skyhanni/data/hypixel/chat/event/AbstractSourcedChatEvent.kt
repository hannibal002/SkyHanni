package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.chat.AbstractChatEvent
import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.util.IChatComponent

abstract class AbstractSourcedChatEvent(
    val authorComponent: ComponentSpan,
    messageComponent: ComponentSpan,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractChatEvent(messageComponent, chatComponent, blockedReason) {
    val author = authorComponent.getText()
}
