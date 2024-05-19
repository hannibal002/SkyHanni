package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ArachneChatMessageHider {

    private val config get() = SkyHanniMod.feature.chat
    private var hideArachneDeadMessage = false

    private val patternGroup = RepoPattern.group("chat.arachne")
    private val arachneCallingPattern by patternGroup.pattern(
        "calling",
        "§4☄ §r.* §r§eplaced an §r§9Arachne's Calling§r§e!.*"
    )
    private val arachneCrystalPattern by patternGroup.pattern(
        "crystal",
        "§4☄ §r.* §r§eplaced an Arachne Crystal! Something is awakening!"
    )
    private val arachneSpawnPattern by patternGroup.pattern(
        "spawn",
        "§c\\[BOSS] Arachne§r§f: (?:The Era of Spiders begins now\\.|Ahhhh\\.\\.\\.A Calling\\.\\.\\.)"
    )
    private val arachneMessageBorderPattern by patternGroup.pattern(
        "border",
        "§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"
    )
    private val arachneDownPattern by patternGroup.pattern(
        "down",
        " {30}§r§6§lARACHNE DOWN!"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
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

        arachneSpawnPattern.matchMatcher(message) {
            return true
        }

        arachneMessageBorderPattern.matchMatcher(message) {
            hideArachneDeadMessage = !hideArachneDeadMessage
            return true
        }

        arachneDownPattern.matchMatcher(message) {
            return true
        }

        return hideArachneDeadMessage
    }

    fun isEnabled() = IslandType.SPIDER_DEN.isInIsland() && config.hideArachneMessages
}
