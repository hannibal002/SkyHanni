package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.chatMessage
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object KernelGainChatMessage {

    private val config get() = SkyHanniMod.feature.chat.filterType.masterChef

    const val SEASONING_PATTERN = "RARE CROP! Seasoning (automatically donated)"

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return
        if (SEASONING_PATTERN == event.cleanMessage) {
            ChatUtils.deleteMessage("masterchef", 1) {
                it.chatMessage.removeColor() == SEASONING_PATTERN
            }
            ChatUtils.chat("§6§lRARE CROP! §2Seasoning §7§o(§e§o+1 Kernel§7§o)", prefix = false)
        }

        event.blockedReason = "arachne"
    }

    fun isEnabled() = Perk.GRAND_FEAST.isActive && IslandType.GARDEN.isInIsland() && config
}
