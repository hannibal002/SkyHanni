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
        val channelPrefix = PlayerChatFilter.getChannelPrefix(event.channel)

        val colon = if (SkyHanniMod.feature.chat.playerColonHider) "" else ":"

        val levelFormat = getLevelFormat(name)
        LorenzUtils.chat("$channelPrefix$elitePrefix$levelFormat§f$colon $message")
        level = -1
    }

    private fun getLevelFormat(name: String) = when (SkyHanniMod.feature.chat.skyblockLevelDesign) {
        0 -> "§8[§$levelColor${level}§8] $name"
        1 -> "§${levelColor}§l$level $name"
        2 -> "$name §8[§$levelColor${level}§8]"
        3 -> name
        else -> "§8[§$levelColor${level}§8] $name"
    }
}