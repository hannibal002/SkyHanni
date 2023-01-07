package at.hannibal2.skyhanni.features

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class CompactSplashPotionMessage {

    private val POTION_EFFECT_PATTERN =
        Pattern.compile("§a§lBUFF! §fYou have gained §r(.*)§r§f! Press TAB or type /effects to view your active effects!")

    private val POTION_EFFECT_OTHERS_PATTERN =
        Pattern.compile("§a§lBUFF! §fYou were splashed by (.*) §fwith §r(.*)§r§f! Press TAB or type /effects to view your active effects!")

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyblock || !SkyHanniMod.feature.chat.compactPotionMessage) return

        var matcher = POTION_EFFECT_PATTERN.matcher(event.message)
        if (matcher.matches()) {
            val name = matcher.group(1)
            event.chatComponent = ChatComponentText("§a§lPotion Effect! §r$name")
        }

        matcher = POTION_EFFECT_OTHERS_PATTERN.matcher(event.message)
        if (matcher.matches()) {
            val playerName = matcher.group(1)
            val effectName = matcher.group(2)
            event.chatComponent = ChatComponentText("§a§lPotion Effect! §r$effectName §7(by $playerName§7)")
        }
    }
}