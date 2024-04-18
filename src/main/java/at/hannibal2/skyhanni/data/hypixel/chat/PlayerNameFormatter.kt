package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.chat.PlayerMessagesConfig
import at.hannibal2.skyhanni.data.hypixel.chat.event.GuildChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerShowItemChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class PlayerNameFormatter {
    private val config get() = SkyHanniMod.feature.chat.playerMessage

    private val patternGroup = RepoPattern.group("data.chat.player.name")

    /**
     * REGEX-TEST: §7☢ §r§b[MVP§d+§b] hannibal2
     * REGEX-TEST: §7☢ §r§b[MVP§d+§b] hannibal2
     * REGEX-TEST: §7☢ §r§bhannibal2
     * REGEX-TEST: §7☢ §rhannibal2
     */
    private val emblemPattern by patternGroup.pattern(
        "emblem",
        "(?<emblem>(§.)*.) §r(?<author>.*)"
    )

    @SubscribeEvent
    fun onPlayerAllChat(event: PlayerAllChatEvent) {
        if (!isEnabled()) return
        val levelColor = event.levelColor
        val level = event.level
        val message = event.message
        val author = event.author
        val privateIslandRank = event.privateIslandRank

        val shouldFilter = config.chatFilter && PlayerChatFilter.shouldChatFilter(message)
        val chatColor = if (shouldFilter) "§7" else if (config.sameChatColor) "§f" else event.chatColor

        val name = nameFormat(author, levelColor, level, privateIslandRank = privateIslandRank)
        val newMessage = "$name$chatColor: $message"

        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, newMessage) ?: return
    }

    @SubscribeEvent
    fun onPrivateMessageChat(event: PrivateMessageChatEvent) {
        if (!isEnabled()) return
        val direction = event.direction
        val message = event.message
        val author = event.author
        val name = nameFormat(author)
        val newMessage = "§d$direction §f$name§7: §f$message"

        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, newMessage) ?: return
    }

    @SubscribeEvent
    fun onGuildChat(event: GuildChatEvent) {
        if (!isEnabled()) return
        val message = event.message
        val author = event.author
        val guildRank = event.guildRank
        val name = nameFormat(author, guildRank = guildRank)
        val newMessage = "§2Guild > $name§f: $message"

        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, newMessage) ?: return
    }

    @SubscribeEvent
    fun onPartyChat(event: PartyChatEvent) {
        if (!isEnabled()) return
        val message = event.message
        val author = event.author
        val name = nameFormat(author)
        val newMessage = "§9Party §8> $name§f: $message"

        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, newMessage) ?: return
    }

    @SubscribeEvent
    fun onPlayerShowItemChat(event: PlayerShowItemChatEvent) {
        if (!isEnabled()) return
        val author = event.author
        val action = event.action
        val itemName = event.itemName
        val name = nameFormat(author)
        val newMessage = "$name §7$action §r$itemName"

        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, newMessage) ?: return
    }

    private fun nameFormat(
        author: String,
        levelColor: String? = null,
        level: Int? = null,
        guildRank: String? = null,
        privateIslandRank: String? = null,
    ): String {
        var cleanAuthor = cleanAuthor(author)

        var emblemFormat = ""
        emblemPattern.matchMatcher(author) {
            val emblem = group("emblem")
            // TODO add emblem hider
            emblemFormat = "$emblem §r"
            cleanAuthor = group("author")
        }

        val name = formatAuthor(cleanAuthor)
        val levelFormat = formatLevel(levelColor, level)
        val guildRankFormat = guildRank ?: ""
        val privateIslandRankFormat = privateIslandRank ?: ""

        val cleanName = cleanAuthor.cleanPlayerName()
        val (faction, ironman, bingo) = AdvancedPlayerList.tabPlayerData[cleanName]?.let {
            val faction = it.faction.icon
            val ironman = if (it.ironman) "§7♲" else ""
            val bingo = it.bingoLevel?.let { level -> BingoAPI.getBingoIcon(level) } ?: ""
            listOf(faction, ironman, bingo)
        } ?: listOf("", "", "")

        val map = mutableMapOf<PlayerMessagesConfig.ChatPart, String>()
        map[PlayerMessagesConfig.ChatPart.SKYBLOCK_LEVEL] = levelFormat
        map[PlayerMessagesConfig.ChatPart.EMBLEM] = emblemFormat
        map[PlayerMessagesConfig.ChatPart.NAME] = name
        map[PlayerMessagesConfig.ChatPart.CRIMSON_FACTION] = faction
        map[PlayerMessagesConfig.ChatPart.MODE_IRONMAN] = ironman
        map[PlayerMessagesConfig.ChatPart.BINGO_LEVEL] = bingo
        map[PlayerMessagesConfig.ChatPart.EMPTY_CHAR] = " "
        map[PlayerMessagesConfig.ChatPart.GUILD_RANK] = guildRankFormat
        map[PlayerMessagesConfig.ChatPart.PRIVATE_ISLAND_RANK] = privateIslandRankFormat

        return config.messageOrder.map { map[it] }.joinToString(" ")
    }

    private fun formatLevel(rawColor: String?, rawLevel: Int?): String {
        val color = rawColor ?: return ""
        val level = rawLevel ?: error("level is null, color is not null")
        // TODO add level formatting options
        return "§8[§r$color$level§r§8] §r"
    }

    private fun cleanAuthor(author: String): String {
        val text = LorenzUtils.stripVanillaMessage(author)
        return text.removeSuffix("§f")
    }

    private fun formatAuthor(author: String): String {
        if (author.contains("ADMIN")) return author
        if (config.ignoreYouTube && author.contains("YOUTUBE")) return author

        val result = author.cleanPlayerName(displayName = true)

        return MarkedPlayerManager.replaceInChat(result)
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && (config.playerRankHider || config.chatFilter || config.sameChatColor)
}
