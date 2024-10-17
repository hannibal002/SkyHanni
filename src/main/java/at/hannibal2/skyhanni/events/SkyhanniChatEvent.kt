package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.util.IChatComponent

class SkyhanniChatEvent(
    val message: String,
    var chatComponent: IChatComponent,
    var blockedReason: String = "",
    var chatLineId: Int = 0,
) : SkyHanniEvent()
