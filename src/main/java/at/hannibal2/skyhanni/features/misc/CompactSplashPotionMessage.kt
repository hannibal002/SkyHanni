package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class CompactSplashPotionMessage {

    private val POTION_EFFECT_PATTERN =
        "§a§lBUFF! §fYou have gained §r(.*)§r§f! Press TAB or type /effects to view your active effects!".toPattern()

    private val POTION_EFFECT_OTHERS_PATTERN =
        "§a§lBUFF! §fYou were splashed by (.*) §fwith §r(.*)§r§f! Press TAB or type /effects to view your active effects!".toPattern()

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzUtils.inSkyBlock || !SkyHanniMod.feature.chat.compactPotionMessage) return

        var matcher = POTION_EFFECT_PATTERN.matcher(event.message)
        if (matcher.matches()) {
            val name = matcher.group(1)
            event.chatComponent = ChatComponentText("§a§lPotion Effect! §r$name")
        }

        matcher = POTION_EFFECT_OTHERS_PATTERN.matcher(event.message)
        if (matcher.matches()) {
            val playerName = matcher.group(1).removeColor()
            val effectName = matcher.group(2)
            event.chatComponent = ChatComponentText("§a§lPotion Effect! §r$effectName by §b$playerName")
        }
    }
}