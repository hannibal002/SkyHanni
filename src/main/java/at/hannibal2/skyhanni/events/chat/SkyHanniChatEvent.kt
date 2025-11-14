package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.util.IChatComponent

@PrimaryFunction("onChat")
open class SkyHanniChatEvent(
    message: String,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
    var chatLineId: Int = 0,
) : AbstractChatEvent(message.asComponent().intoSpan(), chatComponent, blockedReason)
