package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.StringUtils.findMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ArachneChatMessageHider {

    private val config get() = SkyHanniMod.feature.chat
    private var hideArachneDeadMessage = false

    private val patternGroup = RepoPattern.group("chat.arachne.new")
    private val arachneCallingPattern by patternGroup.pattern(
        "calling",
        "^§4☄ §r.* §r§eplaced an §r§9Arachne's Calling§r§e!"
    )
    private val arachneCrystalPattern by patternGroup.pattern(
        "crystal",
        "§4☄ §r.* §r§eplaced an Arachne Crystal! Something is awakening!"
    )
    private val arachneSpawnPattern by patternGroup.pattern(
        "spawn",
        "§c\\[BOSS] Arachne§r§f: (?:The Era of Spiders begins now\\.|Ahhhh\\.\\.\\.A Calling\\.\\.\\.)"
    )
    private val messageSpawnPattern by patternGroup.pattern(
        "messagespawn",
        "§a§l▬{64}"
    )
    private val messageDownPattern by patternGroup.pattern(
        "messagedown",
        "\" {30}§r§6§lARACHNE DOWN!\""
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

        arachneCallingPattern.findMatcher(message) {
            return true
        }
        arachneCrystalPattern.matchMatcher(message) {
            return true
        }

        arachneSpawnPattern.matchMatcher(message) {
            return true
        }

        if (messageSpawnPattern.matches(message)) {
            hideArachneDeadMessage = !hideArachneDeadMessage
            return true
        }
        if (messageDownPattern.matches(message)) {
            hideArachneDeadMessage = true
        }
        return hideArachneDeadMessage
    }

    fun isEnabled() = IslandType.SPIDER_DEN.isInIsland() && config.hideArachneMessages
}
