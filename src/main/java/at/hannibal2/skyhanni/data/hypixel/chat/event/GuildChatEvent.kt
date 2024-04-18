package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.util.IChatComponent

class GuildChatEvent(
    author: String,
    message: String,
    val guildRank: String? = null,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, chatComponent, blockedReason)
