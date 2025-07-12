package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import net.minecraft.util.IChatComponent

class SkyHanniChatEvent(
    override val message: String,
    override var chatComponent: IChatComponent,
    override var blockedReason: String? = null,
    var chatLineId: Int = 0,
) : SystemMessageEvent(
    message = message,
    chatComponent = chatComponent,
    blockedReason = blockedReason,
)
