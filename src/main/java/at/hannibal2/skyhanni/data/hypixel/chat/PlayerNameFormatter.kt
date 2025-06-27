package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.chat.PlayerMessagesConfig
import at.hannibal2.skyhanni.data.hypixel.chat.event.CoopChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.GuildChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerShowItemChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.features.bingo.BingoApi
import at.hannibal2.skyhanni.features.chat.playerchat.PlayerChatFilter
import at.hannibal2.skyhanni.features.misc.MarkedPlayerManager
import at.hannibal2.skyhanni.features.misc.compacttablist.AdvancedPlayerList
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.getFirstColorCode
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.matchStyledMatcher
import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.applyFormattingFrom
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.style
import at.hannibal2.skyhanni.utils.compat.appendComponent
import at.hannibal2.skyhanni.utils.compat.appendString
import at.hannibal2.skyhanni.utils.compat.changeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.IChatComponent

/**
 * Listening to the player chat events, and applying custom chat options to them.
 * E.g. part order, rank hider, etc
 */
@SkyHanniModule
object PlayerNameFormatter {
    private val config get() = SkyHanniMod.feature.chat.playerMessage

    private val patternGroup = RepoPattern.group("data.chat.player.name")

    /**
     * REGEX-TEST: §7☢ §r§b[MVP§d+§b] hannibal2
     * REGEX-TEST: §7☢ §r§b[MVP§d+§b] hannibal2
     * REGEX-TEST: §7☢ §r§bhannibal2
     * REGEX-TEST: §7☢ §rhannibal2
     * REGEX-TEST: §7☢ §b[MVP§c+§b] hannibal2
     * REGEX-TEST: ♫ §b[MVP§d+§b] lrg89
     */
    private val emblemPattern by patternGroup.pattern(
        "emblem",
        "(?<emblem>(?:§.){0,2}.) (?<author>.*)",
    )

    @HandleEvent
    fun onPlayerAllChat(event: PlayerAllChatEvent) {
        if (!isEnabled()) return
        val levelColor = event.levelColor
        val levelComponent = event.levelComponent
        val message = event.messageComponent
        val authorComponent = event.authorComponent
        val privateIslandRank = event.privateIslandRank
        val privateIslandGuest = event.privateIslandGuest

        val shouldFilter = config.chatFilter && PlayerChatFilter.shouldChatFilter(message.intoComponent())
        val chatColor = if (shouldFilter) "§7" else if (config.sameChatColor) "§f" else event.chatColor

        val name = nameFormat(
            authorComponent,
            levelColor?.toString(),
            level = levelComponent,
            privateIslandRank = privateIslandRank,
            privateIslandGuest = privateIslandGuest,
        )
        val all = "".asComponent()
        all.appendComponent(name)
        all.appendString(": ")
        all.appendComponent(chatColor.asComponent())
        all.appendComponent(message.intoComponent())
        event.chatComponent = StringUtils.replaceIfNeeded(event.chatComponent, all) ?: return
    }

    @HandleEvent
    fun onCoopChat(event: CoopChatEvent) {
        if (!isEnabled()) return
        event.chatComponent = StringUtils.replaceIfNeeded(
            event.chatComponent,
            TextHelper.text("§bCo-op > ") {
                appendComponent(nameFormat(event.authorComponent))
                appendString("§f: ")
                appendComponent(event.messageComponent.intoComponent())
            },
        ) ?: return
    }

    @HandleEvent
    fun onGuildChat(event: GuildChatEvent) {
        if (!isEnabled()) return
        event.chatComponent = StringUtils.replaceIfNeeded(
            event.chatComponent,
            TextHelper.text("§2Guild > ") {
                appendComponent(nameFormat(event.authorComponent, guildRank = event.guildRank))
                appendString("§f: ")
                appendComponent(event.messageComponent.intoComponent())
            },
        ) ?: return
    }

    @HandleEvent
    fun onPartyChat(event: PartyChatEvent) {
        if (!isEnabled()) return
        event.chatComponent = StringUtils.replaceIfNeeded(
            event.chatComponent,
            TextHelper.text("§9Party §8> ") {
                appendComponent(nameFormat(event.authorComponent))
                appendString("§f: ")
                appendComponent(event.messageComponent.intoComponent())
            },
        ) ?: return
    }

    @HandleEvent
    fun onPrivateChat(event: PrivateMessageChatEvent) {
        if (!isEnabled()) return
        event.chatComponent = StringUtils.replaceIfNeeded(
            event.chatComponent,
            TextHelper.text("§d${event.direction}") {
                appendString(" ")
                appendComponent(nameFormat(event.authorComponent))
                appendString("§f: ")
                appendComponent(event.messageComponent.intoComponent())
            },
        ) ?: return
    }

    @HandleEvent
    fun onPlayerShowItemChat(event: PlayerShowItemChatEvent) {
        if (!isEnabled()) return
        event.chatComponent = StringUtils.replaceIfNeeded(
            event.chatComponent,
            TextHelper.text("") {
                appendComponent(
                    nameFormat(
                        event.authorComponent,
                        levelColor = event.levelComponent?.getText()?.getFirstColorCode()?.let { "§$it" },
                        level = event.levelComponent,
                    ),
                )

                appendString(" ")
                appendComponent(event.action.intoComponent().changeColor(LorenzColor.GRAY))

                appendString(" ")
                appendComponent(event.item.intoComponent())
            },
        ) ?: return
    }

    private fun nameFormat(
        author: ComponentSpan,
        levelColor: String? = null,
        level: ComponentSpan? = null,
        guildRank: ComponentSpan? = null,
        privateIslandRank: ComponentSpan? = null,
        privateIslandGuest: ComponentSpan? = null,
    ): IChatComponent {
        var cleanAuthor = cleanAuthor(author)

        var emblemFormat: IChatComponent? = null
        emblemPattern.matchStyledMatcher(author) {
            emblemFormat = componentOrThrow("emblem")
            cleanAuthor = groupOrThrow("author").stripHypixelMessage()
        }

        val name = formatAuthor(cleanAuthor, levelColor)
        val levelFormat = formatLevel(levelColor, level)
        val guildRankFormat = guildRank?.intoComponent()
        val privateIslandRankFormat = privateIslandRank?.intoComponent()
        val privateIslandGuestFormat = privateIslandGuest?.intoComponent()

        val cleanName = cleanAuthor.getText().cleanPlayerName()
        val (faction, ironman, bingo) = AdvancedPlayerList.tabPlayerData[cleanName]?.let {
            val faction = it.faction.icon?.trim()?.asComponent()
            val ironman = if (it.ironman) "§7♲".asComponent() else null
            val bingo = it.bingoLevel?.let { level -> BingoApi.getBingoIcon(level).asComponent() }
            listOf(faction, ironman, bingo)
        } ?: listOf(null, null, null)

        val map = mutableMapOf<PlayerMessagesConfig.MessagePart, IChatComponent?>()
        map[PlayerMessagesConfig.MessagePart.SKYBLOCK_LEVEL] = levelFormat
        map[PlayerMessagesConfig.MessagePart.EMBLEM] = emblemFormat
        map[PlayerMessagesConfig.MessagePart.PLAYER_NAME] = name.intoComponent()
        map[PlayerMessagesConfig.MessagePart.CRIMSON_FACTION] = faction
        map[PlayerMessagesConfig.MessagePart.MODE_IRONMAN] = ironman
        map[PlayerMessagesConfig.MessagePart.BINGO_LEVEL] = bingo
        map[PlayerMessagesConfig.MessagePart.GUILD_RANK] = guildRankFormat
        map[PlayerMessagesConfig.MessagePart.PRIVATE_ISLAND_RANK] = privateIslandRankFormat
        map[PlayerMessagesConfig.MessagePart.PRIVATE_ISLAND_GUEST] = privateIslandGuestFormat

        val all = "".asComponent()
        var first = true
        for (text in config.partsOrder.mapNotNull { map[it] }) {
            if (first) {
                first = false
            } else {
                if (!all.unformattedText.endsWith(" ")) {
                    all.appendString(" ")
                }
            }
            all.appendComponent(text)
        }

        return all
    }

    private fun formatLevel(rawColor: String?, rawLevel: ComponentSpan?): IChatComponent? {
        val color = rawColor ?: return null
        val level = rawLevel?.getText() ?: error("level is null, color is not null")
        val levelData = "$color$level"
        val result = if (config.hideLevelBrackets) levelData else "§8[$levelData§8]"
        return result.applyFormattingFrom(rawLevel)
    }

    private fun cleanAuthor(author: ComponentSpan): ComponentSpan {
        // TODO: I don't think we even need to strip this ???
        return author.stripHypixelMessage().removePrefix("§f")
    }

    private fun ComponentSpan.splitPlayerNameAndExtras(): Pair<ComponentSpan?, ComponentSpan> {
        val space = getText().indexOf(' ')
        if (space < 0) return Pair(null, this)
        return Pair(slice(0, space + 1), slice(space + 1))
    }

    private fun formatAuthor(author: ComponentSpan, levelColor: String?): ComponentSpan {
        if (author.getText().contains("ADMIN")) return author
        if (config.ignoreYouTube && author.getText().contains("YOUTUBE")) return author
        val (rank, name) = author.splitPlayerNameAndExtras()
        val rankColor =
            if (rank != null && rank.sampleAtStart() === name.sampleAtStart()) StringUtils.getFormatFromString(rank.getText()) else ""
        val coloredName = createColoredName(name, levelColor, name.getText().removeColor(), rankColor)
        return if (config.playerRankHider || rank == null) coloredName else rank + coloredName
    }

    private fun createColoredName(
        name: ComponentSpan,
        levelColor: String?,
        removeColor: String,
        rankColor: String,
    ): ComponentSpan = when {
        MarkedPlayerManager.isMarkedPlayer(removeColor) && MarkedPlayerManager.config.highlightInChat ->
            (MarkedPlayerManager.replaceInChat(rankColor + removeColor)).asComponent()
                .setChatStyle(name.sampleStyleAtStart()).intoSpan()

        levelColor != null && config.useLevelColorForName ->
            (levelColor + removeColor).asComponent()
                .setChatStyle(name.sampleStyleAtStart())
                .intoSpan()

        config.playerRankHider ->
            removeColor.asComponent()
                .setChatStyle(name.sampleStyleAtStart()?.createShallowCopy())
                .style { color = EnumChatFormatting.AQUA }
                .intoSpan()

        else ->
            if (rankColor.isEmpty()) name
            else (rankColor + removeColor).asComponent()
                .setChatStyle(name.sampleStyleAtStart())
                .intoSpan()
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enable

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.transform(41, "chat.PlayerMessagesConfig.partsOrder") { element ->
            val newList = JsonArray()
            for (entry in element.asJsonArray) {
                if (entry is JsonNull) continue
                if (entry.asString != "EMPTY_CHAR") {
                    newList.add(entry)
                }
            }
            newList
        }
    }
}
