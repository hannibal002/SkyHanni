package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CompactSplashPotionMessage {
    private val config get() = SkyHanniMod.feature.chat.compactPotionMessages

    private val potionEffectPattern =
        "§a§lBUFF! §fYou have gained §r(?<name>.*)§r§f! Press TAB or type /effects to view your active effects!".toPattern()
    private val potionSplashEffectOthersPattern =
        "§a§lBUFF! §fYou were splashed by (?<playerName>.*) §fwith §r(?<effectName>.*)§r§f! Press TAB or type /effects to view your active effects!".toPattern()
    private val potionSplashEffectPattern =
        "§a§lBUFF! §fYou splashed yourself with §r(?<name>.*)§r§f! Press TAB or type /effects to view your active effects!".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock || !config.enabled) return

        potionEffectPattern.matchMatcher(event.message) {
            val name = group("name")
            sendMessage("§a§lPotion Effect! §r$name")
            event.blockedReason = "compact_potion_effect"
        }

        potionSplashEffectOthersPattern.matchMatcher(event.message) {
            val playerName = group("playerName").removeColor()
            val effectName = group("effectName")
            sendMessage("§a§lPotion Effect! §r$effectName by §b$playerName")
            event.blockedReason = "compact_potion_effect"
        }

        potionSplashEffectPattern.matchMatcher(event.message) {
            val name = group("name")
            sendMessage("§a§lPotion Effect! §r$name")
            event.blockedReason = "compact_potion_effect"
        }
    }

    private fun sendMessage(message: String) {
        if (config.clickableChatMessage) {
            LorenzUtils.hoverableChat(message, listOf("§eClick to view your potion effects."), "/effects")
        } else {
            LorenzUtils.chat(message)
        }
    }
}
