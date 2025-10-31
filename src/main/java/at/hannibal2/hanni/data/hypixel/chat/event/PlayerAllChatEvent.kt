package at.hannibal2.hanni.data.hypixel.chat.event

import at.hannibal2.hanni.utils.ComponentSpan
import net.minecraft.util.IChatComponent
//#if MC > 1.21
//$$ import at.hannibal2.hanni.utils.compat.toChatFormatting
//#endif

class PlayerAllChatEvent(
    val levelComponent: ComponentSpan?,
    val privateIslandRank: ComponentSpan?,
    val privateIslandGuest: ComponentSpan?,
    val chatColor: String,
    authorComponent: ComponentSpan,
    messageComponent: ComponentSpan,
    chatComponent: IChatComponent,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(authorComponent, messageComponent, chatComponent, blockedReason) {
    val levelColor =
        //#if MC < 1.21
        levelComponent?.sampleStyleAtStart()?.color
    //#else
    //$$ levelComponent?.sampleStyleAtStart()?.color?.toChatFormatting()
    //#endif
    val level = levelComponent?.getText()?.toInt()
    val isAGuest get() = privateIslandGuest != null
}
