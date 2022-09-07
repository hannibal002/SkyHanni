package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PlayerSendChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class SkyBlockLevelChatMessage {

    companion object {
        var level = -1
        var levelColor = ""

        fun setData(level: Int, levelColor: String) {
            this.level = level
            this.levelColor = levelColor
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: PlayerSendChatEvent) {
        if (level == -1) return
        event.cancelledReason = "skyblock level"

        val finalMessage = event.message
        val name = event.playerName
        val prefix = event.channel.prefix

        if (SkyHanniMod.feature.chat.hideSkyblockLevel) {
            LorenzUtils.chat("$prefix §b$name §f$finalMessage")
        } else {

            when (SkyHanniMod.feature.chat.skyblockLevelDesign) {
                0 -> {
                    LorenzUtils.chat("$prefix §8[§${levelColor}${level}§8] §b$name §f$finalMessage")
                }

                1 -> {
                    LorenzUtils.chat("$prefix §${levelColor}§l${level} §b$name §f$finalMessage")
                }

                2 -> {
                    LorenzUtils.chat("$prefix §b$name §8[§${levelColor}${level}§8]§f: $finalMessage")
                }
            }
        }
        level = -1
    }
}