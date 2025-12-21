package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

class PrivateMessageChatEvent(
    val direction: String?,
    author: ComponentSpan,
    message: ComponentSpan,
    chatComponent: Component,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(author, message, chatComponent, blockedReason)
