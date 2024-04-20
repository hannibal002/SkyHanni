package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import net.minecraft.util.IChatComponent

open class AbstractChatEvent(
    val author: String,
    val message: String,
    var chatComponent: IChatComponent,
    var blockedReason: String? = null,
) : LorenzEvent() {
    val cleanedAuthor: String
        get() = author.cleanPlayerName()
}
