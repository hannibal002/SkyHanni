package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.groupOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerChatManager {

    private val patternGroup = RepoPattern.group("data.chat.player")
    private val globalPattern by patternGroup.pattern(
        "global",
        "(?:§8\\[§r(?<levelColor>§.)(?<level>\\d+)§r§8] §r)?(?<author>§.+)(?:§f|§7§r§7): (?<message>.*)"
    )

    /**
     * REGEX-TEST: §9Party §8> §b§l⚛ §b[MVP§f+§b] Dankbarkeit§f: §rx: -190, y: 5, z: -163
     * REGEX-TEST: §9Party §8> §6⚔ §6[MVP§3++§6] RealBacklight§f: §r!warp
     * REGEX-TEST: §9Party §8> §b[MVP§3+§b] Eisengolem§f: §r!pt
     */
    private val partyPattern by patternGroup.pattern(
        "party",
        "§9Party §8> (?<author>[^:]*): §r(?<message>.*)"
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        globalPattern.matchMatcher(event.message) {
            val levelColor = groupOrNull("levelColor")
            val level = groupOrNull("level")?.formatInt()
            val author = group("author")
            val message = LorenzUtils.stripVanillaMessage(group("message"))
            if (author.contains("[NPC]")) {
                NpcChatEvent(author, message.removePrefix("§f")).postChat(event)
                NpcChatEvent(author, message.removePrefix("§f")).postChat(event)
            } else {
                PlayerAllChatEvent(levelColor, level, author, message).postChat(event)
            }
            return
        }
        partyPattern.matchMatcher(event.message) {
            val author = group("author")
            val message = group("message")
            PartyChatEvent(author, message).postChat(event)
            return
        }
    }

    private fun AbstractChatEvent.postChat(event: LorenzChatEvent) {
        if (postAndCatch()) {
            event.cancel()
        }
        blockedReason?.let {
            event.blockedReason = it
        }
    }
}
