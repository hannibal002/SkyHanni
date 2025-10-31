package at.hannibal2.hanni.events.chat

import at.hannibal2.hanni.hannimodule.PrimaryFunction
import at.hannibal2.hanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import net.minecraft.util.IChatComponent

@PrimaryFunction("onChat")
open class HanniChatEvent(
    message: String,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
    var chatLineId: Int = 0,
) : AbstractChatEvent(message.asComponent().intoSpan(), chatComponent, blockedReason)
