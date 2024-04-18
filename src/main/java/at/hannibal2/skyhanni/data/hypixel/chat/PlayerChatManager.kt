package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.events.LorenzChatEvent
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

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        globalPattern.matchMatcher(event.message) {
            val levelColor = groupOrNull("levelColor")
            val level = groupOrNull("levelevellColor")?.formatInt()
            val author = group("author")
            val message = group("message")
            val playerChatEvent = PlayerChatEvent(levelColor, level, author, message)
            playerChatEvent.postAndCatch()
            playerChatEvent.blockedReason?.let {
                event.blockedReason = it
            }
            return
        }
    }
}
