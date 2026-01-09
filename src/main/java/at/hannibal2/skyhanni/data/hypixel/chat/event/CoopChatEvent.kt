package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

class CoopChatEvent(
    authorComponent: ComponentSpan,
    messageComponent: ComponentSpan,
    chatComponent: Component,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(authorComponent, messageComponent, chatComponent, blockedReason)
