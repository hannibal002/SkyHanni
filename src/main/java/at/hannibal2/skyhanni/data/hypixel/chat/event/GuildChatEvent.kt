package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component


class GuildChatEvent(
    author: ComponentSpan,
    message: ComponentSpan,
    val guildRank: ComponentSpan?,
    chatComponent: Component,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(author, message, chatComponent, blockedReason)
