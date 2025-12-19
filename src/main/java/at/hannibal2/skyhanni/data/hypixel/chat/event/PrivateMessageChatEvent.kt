package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.text.Text

class PrivateMessageChatEvent(
    val direction: String?,
    author: ComponentSpan,
    message: ComponentSpan,
    chatComponent: Text,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(author, message, chatComponent, blockedReason)
