package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CompactSplashPotionMessage {
    private val potionEffectPattern =
        "§a§lBUFF! §fYou have gained §r(?<name>.*)§r§f! Press TAB or type /effects to view your active effects!".toPattern()
    private val potionEffectOthersPattern =
        "§a§lBUFF! §fYou were splashed by (?<playerName>.*) §fwith §r(?<effectName>.*)§r§f! Press TAB or type /effects to view your active effects!".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock || !SkyHanniMod.feature.chat.compactPotionMessage) return

        potionEffectPattern.matchMatcher(event.message) {
            val name = group("name")
            event.chatComponent = ChatComponentText("§a§lPotion Effect! §r$name")
        }

        potionEffectOthersPattern.matchMatcher(event.message) {
            val playerName = group("playerName").removeColor()
            val effectName = group("effectName")
            event.chatComponent = ChatComponentText("§a§lPotion Effect! §r$effectName by §b$playerName")
        }
    }
}