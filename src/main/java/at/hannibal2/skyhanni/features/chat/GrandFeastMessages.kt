package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.data.Perk
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getKernels
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong

@SkyHanniModule
object GrandFeastMessages {

    private val config get() = SkyHanniMod.feature.chat.grandFeastMessages

    const val TED_MESSAGE = "[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse."
    const val SEASONING_MESSAGE = "RARE CROP! Seasoning (automatically donated)"

    var shouldSendSeasoningMessage = false

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isActive()) return
        if (TED_MESSAGE == event.cleanMessage) {
            if (config.masterChef) {
                event.blockedReason = "masterChef"
                return
            } else if (config.seasoningKernels) {
                // Delay Master Chef message by blocking it and resending
                // later so it still gets sent after the Seasoning message
                event.blockedReason = "masterChefSeasoningKernels"
                return
            }
        }
        if (SEASONING_MESSAGE == event.cleanMessage && config.seasoningKernels) {
            event.blockedReason = "seasoningKernels"
            shouldSendSeasoningMessage = true
            return
        }
    }

    @HandleEvent
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        if (!shouldSendSeasoningMessage) return
        if (getKernels().isEmpty()) return
        val kernels = getKernels().formatLong() + 1
        ChatUtils.chat("§6§lRARE CROP! §2Seasoning §7(§e$kernels Kernels§7)", prefix = false)
        if (!config.masterChef) {
            ChatUtils.chat("§e[NPC] Feast Chef Ted§f: Thanks for the donation! I've added a §eKernel §fto your purse.", prefix = false)
        }
        shouldSendSeasoningMessage = false
    }

    fun isActive() = Perk.GRAND_FEAST.isActive && IslandType.GARDEN.isInIsland()
}
