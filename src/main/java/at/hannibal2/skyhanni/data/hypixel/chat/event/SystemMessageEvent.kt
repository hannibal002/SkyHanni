package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.util.IChatComponent

// A SkyHanniChatEvent after filtering all player send events, leaving messages from the game/system.
class SystemMessageEvent(
    val message: String,
    var chatComponent: IChatComponent,
    var blockedReason: String? = null,
) : SkyHanniEvent()
