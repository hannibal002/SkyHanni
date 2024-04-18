package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.util.IChatComponent

class PlayerAllChatEvent(
    val levelColor: String?,
    val level: Int?,
    val privateIslandRank: String? = null,
    val isAGuest: Boolean,
    author: String,
    val chatColor: String?,
    message: String,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, chatComponent, blockedReason)
