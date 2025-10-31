package at.hannibal2.hanni.data.hypixel.chat.event

import at.hannibal2.hanni.utils.ComponentSpan
import net.minecraft.util.IChatComponent

class NpcChatEvent(
    authorComponent: ComponentSpan,
    messageComponent: ComponentSpan,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(authorComponent, messageComponent, chatComponent, blockedReason)
