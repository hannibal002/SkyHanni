package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.compat.toChatFormatting
import net.minecraft.network.chat.Component

class PlayerAllChatEvent(
    val levelComponent: ComponentSpan?,
    val privateIslandRank: ComponentSpan?,
    val privateIslandGuest: ComponentSpan?,
    val chatColor: String,
    authorComponent: ComponentSpan,
    messageComponent: ComponentSpan,
    chatComponent: Component,
    blockedReason: String? = null,
) : AbstractSourcedChatEvent(authorComponent, messageComponent, chatComponent, blockedReason) {
    val levelColor = levelComponent?.sampleStyleAtStart()?.color?.toChatFormatting()
    val level = levelComponent?.getText()?.toInt()
    val isAGuest get() = privateIslandGuest != null
}
