package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.compat.toChatFormatting
import net.minecraft.network.chat.Component

object PlayerAllChatEvent {

    class Allow(
        val levelComponent: ComponentSpan?,
        val privateIslandRank: ComponentSpan?,
        val privateIslandGuest: ComponentSpan?,
        val chatColor: String,
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason) {
        val levelColor = levelComponent?.sampleStyleAtStart()?.color?.toChatFormatting()
        val level = levelComponent?.getText()?.toInt()
        val isAGuest get() = privateIslandGuest != null
    }

    class Modify(
        val levelComponent: ComponentSpan?,
        val privateIslandRank: ComponentSpan?,
        val privateIslandGuest: ComponentSpan?,
        val chatColor: String,
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason) {
        val levelColor = levelComponent?.sampleStyleAtStart()?.color?.toChatFormatting()
        val level = levelComponent?.getText()?.toInt()
        val isAGuest get() = privateIslandGuest != null
    }
}
