package at.hannibal2.hanni.data.hypixel.chat.event

import at.hannibal2.hanni.utils.ComponentSpan
import net.minecraft.util.IChatComponent


class GuildChatEvent(
    author: ComponentSpan,
    message: ComponentSpan,
    val guildRank: ComponentSpan?,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(author, message, chatComponent, blockedReason)
