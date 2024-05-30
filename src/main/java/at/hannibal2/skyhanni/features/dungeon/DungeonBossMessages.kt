package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.find
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonBossMessages {

    private val config get() = SkyHanniMod.feature.chat

    private val patternGroup = RepoPattern.group("dungeonbossmessages")
    private val bossPattern by patternGroup.pattern(
        "boss",
        "§(\\[cd4])\\[BOSS] (.*)"
    )
    private val excludedMessagesPatterns by patternGroup.list(
        "excludedmessages",
        "§c\\[BOSS] The Watcher§r§f: You have proven yourself. You may pass.",
    )

    // M7 – Dragons
    private val messagePatterns by patternGroup.list(
        "message",
        "§cThe Crystal withers your soul as you hold it in your hands!",
        "§cIt doesn't seem like that is supposed to go there.",
    )
    private val messageContainsPatterns by patternGroup.list(
        "messagecontains",
        " The Watcher§r§f: ",
        " Bonzo§r§f: ",
        " Scarf§r§f:",
        "Professor§r§f",
        " Livid§r§f: ",
        " Enderman§r§f: ",
        " Thorn§r§f: ",
        " Sadan§r§f: ",
        " Maxor§r§c: ",
        " Storm§r§c: ",
        " Goldor§r§c: ",
        " Necron§r§c: ",
        " §r§4§kWither King§r§c:",
    )
    private val messageEndsWithPatterns by patternGroup.list(
        "messageendswith",
        " Necron§r§c: That is enough, fool!$",
        " Necron§r§c: Adventurers! Be careful of who you are messing with..$",
        " Necron§r§c: Before I have to deal with you myself.$",
    )

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!DungeonAPI.inDungeon()) return
        if (!isBoss(event.message)) return

        DungeonAPI.handleBossMessage(event.message)

        if (config.dungeonBossMessages) {
            event.blockedReason = "dungeon_boss"
        }
    }

    /**
     * Checks if the message is a boss message
     * @return true if the message is a boss message
     * @param message The message to check
     * @see excludedMessagesPattern
     * @see messagePattern
     * @see messageContainsPattern
     * @see messageEndsWithPatterns
     */
    private fun isBoss(message: String): Boolean {
        // Cases that match below but should not be blocked

        if (excludedMessagesPatterns.any { it.matches(message) }) return false

        // Exact Matches
        if (messagePatterns.any { it.matches(message) }) return true

        // Matches Regex for Boss Prefix
        bossPattern.matchMatcher(message) {
            return messageContainsPatterns.any { it.matches(message) } || messageEndsWithPatterns.any { it.find(message) }
        }
        return false
    }
}
