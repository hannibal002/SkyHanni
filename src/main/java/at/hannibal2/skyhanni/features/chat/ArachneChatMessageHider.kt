package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ArachneChatMessageHider {
    private val config get() = SkyHanniMod.feature.chat
    private var hideArachneDeadMessage = false
    // TODO USE SH-REPO
    private val arachneCallingPattern = "§4☄ §r.* §r§eplaced an §r§9Arachne's Calling§r§e!.*".toPattern()
    private val arachneCrystalPattern = "§4☄ §r.* §r§eplaced an Arachne Crystal! Something is awakening!".toPattern()

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        if (LorenzUtils.skyBlockIsland != IslandType.SPIDER_DEN) return
        if (LorenzUtils.skyBlockArea == "Arachne's Sanctuary") return

        if (shouldHide(event.message)) {
            event.blockedReason = "arachne"
        }
    }

    private fun shouldHide(message: String): Boolean {

        arachneCallingPattern.matchMatcher(message) {
            return true
        }
        arachneCrystalPattern.matchMatcher(message) {
            return true
        }

        if (message == "§c[BOSS] Arachne§r§f: Ahhhh...A Calling...") return true
        if (message == "§c[BOSS] Arachne§r§f: The Era of Spiders begins now.") return true

        if (message == "§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            hideArachneDeadMessage = !hideArachneDeadMessage
            return true
        }
        if (message == "                              §r§6§lARACHNE DOWN!") {
            hideArachneDeadMessage = true
        }
        return hideArachneDeadMessage
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.hideArachneMessages
}
