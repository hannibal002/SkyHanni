package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PlayerSendChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyBlockLevelChatMessage {

    companion object {
        var level = -1
        var levelColor = ""
        var elitePrefix = ""
    }

    @SubscribeEvent
    fun onChatMessage(event: PlayerSendChatEvent) {
        if (level == -1) return
        event.cancelledReason = "skyblock level"

        val message = event.message
        val name = event.formattedName
        var prefix = if (event.channel == PlayerMessageChannel.ALL && !SkyHanniMod.feature.chat.allChannelPrefix)
            "" else PlayerChatFilter.getChannelPrefix(event.channel)

        if (elitePrefix != "") {
            prefix = "$prefix $elitePrefix".trim()
        }
        val colon = if (SkyHanniMod.feature.chat.playerColonHider) "" else ":"

        when (SkyHanniMod.feature.chat.skyblockLevelDesign) {
            0 -> {
                LorenzUtils.chat("$prefix §8[§$levelColor$level§8] $name§f$colon $message")
            }

            1 -> {
                LorenzUtils.chat("$prefix §$levelColor§l$level $name§f$colon $message")
            }

            2 -> {
                LorenzUtils.chat("$prefix $name §8[§$levelColor$level§8]§f$colon $message")
            }

            3 -> {
                LorenzUtils.chat("$prefix $name§f$colon $message")
            }
        }
        level = -1
    }
}