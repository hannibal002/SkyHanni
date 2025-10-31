package at.hannibal2.hanni.features.chat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object ArachneChatMessageHider {

    private val config get() = HanniMod.feature.chat
    private var hideArachneDeadMessage = false

    private val patternGroup = RepoPattern.group("chat.arachne")

    /**
     * REGEX-TEST: §4☄ §r§7littlegremlins §r§eplaced an §r§9Arachne's Calling§r§e! Something is awakening! §r§e(§r§a4§r§e/§r§a4§r§e)
     */
    val arachneCallingPattern by patternGroup.pattern(
        "calling",
        "§4☄ §r.* §r§eplaced an §r§9Arachne's Calling§r§e!.*",
    )

    /**
     * REGEX-TEST: §4☄ §r§7SultanHakeem §r§eplaced an Arachne Crystal! Something is awakening!
     */
    val arachneCrystalPattern by patternGroup.pattern(
        "crystal",
        "§4☄ §r.* §r§eplaced an Arachne Crystal! Something is awakening!",
    )

    /**
     * REGEX-TEST: §c[BOSS] Arachne§r§f: The Era of Spiders begins now.
     */
    private val arachneSpawnPattern by patternGroup.pattern(
        "spawn",
        "§c\\[BOSS] Arachne§r§f: (?:The Era of Spiders begins now\\.|Ahhhh\\.\\.\\.A Calling\\.\\.\\.)",
    )

    /**
     * REGEX-TEST: §dArachne's Keeper used §r§2Venom Shot §r§don you hitting you for §r§c87.7 damage §r§dand infecting you with venom.
     * REGEX-TEST: §dArachne used §r§2Venom Shot §r§don you hitting you for §r§c58 damage §r§dand infecting you with venom.
     * REGEX-TEST: §dArachne's Brood used §r§2Venom Shot §r§don you hitting you for §r§c19.8 damage §r§dand infecting you with venom.
     */
    @Suppress("MaxLineLength")
    private val venomShotPattern by patternGroup.pattern(
        "venom",
        "§dArachne(?:'s (?:Keeper|Brood))? used §r§2Venom Shot §r§don you hitting you for §r§c[\\d.,]+ damage §r§dand infecting you with venom\\.",
    )

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return
        if (!shouldHide(event.message)) return

        event.blockedReason = "arachne"
    }

    private fun shouldHide(message: String): Boolean {

        venomShotPattern.matchMatcher(message) {
            return true
        }

        if (SkyBlockUtils.graphArea == "Arachne's Sanctuary") return false

        arachneCallingPattern.matchMatcher(message) {
            return true
        }
        arachneCrystalPattern.matchMatcher(message) {
            return true
        }
        arachneSpawnPattern.matchMatcher(message) {
            return true
        }

        if (message == "§a§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬") {
            hideArachneDeadMessage = !hideArachneDeadMessage
            return true
        }
        if (message == "                              §r§6§lARACHNE DOWN!") {
            hideArachneDeadMessage = true
        }
        return hideArachneDeadMessage
    }

    fun isEnabled() = IslandType.SPIDER_DEN.isCurrent() && config.hideArachneMessages
}
