package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.util.IChatComponent

class PlayerShowItemChatEvent(
    val levelColor: String?,
    val level: Int?,
    author: String,
    message: String,
    val action: String,
    val itemName: String,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractChatEvent(author, message, chatComponent, blockedReason)
