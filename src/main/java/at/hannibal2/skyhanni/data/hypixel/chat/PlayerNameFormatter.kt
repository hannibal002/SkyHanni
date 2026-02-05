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
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.StringUtils.applyFormattingFrom
import at.hannibal2.skyhanni.utils.StringUtils.cleanPlayerName
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.chat.TextHelper.style
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import com.google.gson.JsonNull
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor

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
    fun onPlayerAllChat(event: PlayerAllChatEvent.Modify) {
        if (!isEnabled()) return
        val levelColor = event.levelColor
        val levelComponent = event.levelComponent
        val message = event.messageComponent
        val authorComponent = event.authorComponent
        val privateIslandRank = event.privateIslandRank
        val privateIslandGuest = event.privateIslandGuest

        val shouldFilter = config.chatFilter && PlayerChatFilter.shouldChatFilter(message)
        val chatColor = if (shouldFilter) TextColor.fromLegacyFormat(ChatFormatting.GRAY)!!
        else if (config.sameChatColor) TextColor.fromLegacyFormat(ChatFormatting.WHITE)!!
        else event.chatColor

        val name = nameFormat(
            authorComponent,
            levelColor?.toString(),
            level = levelComponent,
            privateIslandRank = privateIslandRank,
            privateIslandGuest = privateIslandGuest,
        )
        val component = componentBuilder {
            append(name)
            append(": ")
            appendWithColor(message, chatColor)
        }
        if (component == event.chatComponent) return
        /*val all = "".asComponent()
        all.append(name)
        all.append(": ")
        all.append(chatColor.asComponent())
        all.append(message.intoComponent())
        val component = StringUtils.replaceIfNeeded(event.chatComponent, all) ?: return*/
        event.replaceComponent(component, "player_chat_formatting")
    }

    @HandleEvent
    fun onCoopChat(event: CoopChatEvent.Modify) {
        if (!isEnabled()) return

        val component = componentBuilder {
            appendWithColor("Co-op > ", ChatFormatting.AQUA)
            append(nameFormat(event.authorComponent))
            appendWithColor(": ", ChatFormatting.WHITE)
            append(event.messageComponent)
        }

        if (component == event.chatComponent) return
        event.replaceComponent(component, "coop_chat_formatting")
    }

    @HandleEvent
    fun onGuildChat(event: GuildChatEvent.Modify) {
        if (!isEnabled()) return

        val component = componentBuilder {
            appendWithColor("Guild > ", ChatFormatting.DARK_GREEN)
            append(nameFormat(event.authorComponent, guildRank = event.guildRank))
            appendWithColor(": ", ChatFormatting.WHITE)
            append(event.messageComponent)
        }

        if (component == event.chatComponent) return

        event.replaceComponent(component, "guild_chat_formatting")
    }

    @HandleEvent
    fun onPartyChat(event: PartyChatEvent.Modify) {
        if (!isEnabled()) return

        val component = componentBuilder {
            appendWithColor("Party ", ChatFormatting.BLUE)
            appendWithColor("> ", ChatFormatting.DARK_GRAY)
            append(nameFormat(event.authorComponent))
            appendWithColor(": ", ChatFormatting.WHITE)
            append(event.messageComponent)
        }

        if (component == event.chatComponent) return

        event.replaceComponent(component, "party_chat_formatting")
    }

    @HandleEvent
    fun onPrivateChat(event: PrivateMessageChatEvent.Modify) {
        if (!isEnabled()) return

        val component = componentBuilder {
            appendWithColor(event.direction, ChatFormatting.LIGHT_PURPLE)
            append(" ")
            append(nameFormat(event.authorComponent))
            appendWithColor(": ", ChatFormatting.WHITE)
            append(event.messageComponent)
        }

        if (component == event.chatComponent) return

        event.replaceComponent(component, "private_chat_formatting")
    }

    @HandleEvent
    fun onPlayerShowItemChat(event: PlayerShowItemChatEvent.Modify) {
        if (!isEnabled()) return

        val component = componentBuilder {
            append(nameFormat(event.authorComponent, levelColor = event.levelComponent?.string?.getFirstColorCode()?.let { "§$it" }, level = event.levelComponent))
            append(" ")
            appendWithColor(event.action, ChatFormatting.GRAY)
            append(" ")
            append(event.item)
        }

        if (component == event.chatComponent) return

        event.replaceComponent(component, "show_chat_formatting")
    }

    private fun nameFormat(
        author: Component,
        levelColor: String? = null,
        level: Component? = null,
        guildRank: Component? = null,
        privateIslandRank: Component? = null,
        privateIslandGuest: Component? = null,
    ): Component {
        var cleanAuthor = author

        var emblemFormat: Component? = null
        emblemPattern.matchMatcher(author) {
            emblemFormat = TextHelper.matcher(author, group("emblem"))
            cleanAuthor = TextHelper.matcher(author, group("author")) ?: throw Error()
        }

        val name = formatAuthor(cleanAuthor, levelColor)
        val levelFormat = formatLevel(levelColor, level)

        val cleanName = cleanAuthor.string.cleanPlayerName()
        val (faction, ironman, bingo) = AdvancedPlayerList.tabPlayerData[cleanName]?.let {
            val faction = it.faction.icon?.trim()?.asComponent()
            val ironman = if (it.ironman) "§7♲".asComponent() else null
            val bingo = it.bingoLevel?.let { level -> BingoApi.getBingoIcon(level).asComponent() }
            listOf(faction, ironman, bingo)
        } ?: listOf(null, null, null)

        val map = mutableMapOf<PlayerMessagesConfig.MessagePart, Component?>()
        map[PlayerMessagesConfig.MessagePart.SKYBLOCK_LEVEL] = levelFormat
        map[PlayerMessagesConfig.MessagePart.EMBLEM] = emblemFormat
        map[PlayerMessagesConfig.MessagePart.PLAYER_NAME] = name
        map[PlayerMessagesConfig.MessagePart.CRIMSON_FACTION] = faction
        map[PlayerMessagesConfig.MessagePart.MODE_IRONMAN] = ironman
        map[PlayerMessagesConfig.MessagePart.BINGO_LEVEL] = bingo
        map[PlayerMessagesConfig.MessagePart.GUILD_RANK] = guildRank
        map[PlayerMessagesConfig.MessagePart.PRIVATE_ISLAND_RANK] = privateIslandRank
        map[PlayerMessagesConfig.MessagePart.PRIVATE_ISLAND_GUEST] = privateIslandGuest

        val all = "".asComponent()
        var first = true
        for (text in config.partsOrder.mapNotNull { map[it] }) {
            if (first) {
                first = false
            } else {
                if (!all.string.endsWith(" ")) {
                    all.append(" ")
                }
            }
            all.append(text)
        }

        return all
    }

    private fun formatLevel(rawColor: String?, rawLevel: Component?): Component? {
        val color = rawColor ?: return null
        val level = rawLevel?.string ?: error("level is null, color is not null")
        val levelData = "$color$level"
        val result = if (config.hideLevelBrackets) levelData else "§8[$levelData§8]"
        return result.applyFormattingFrom(rawLevel)
    }

    private fun Component.splitPlayerNameAndExtras(): Pair<Component?, Component> {
        val split = TextHelper.split(this, " ") ?: return Pair(null, this)
        if (split.size == 1) return Pair(null, this)
        return Pair(split.first().append(" "), TextHelper.join(split.drop(1), Component.literal(" ")))
    }

    private fun formatAuthor(author: Component, levelColor: String?): Component {
        if (author.string.contains("ADMIN")) return author
        if (config.ignoreYouTube && author.string.contains("YOUTUBE")) return author
        val (rank, name) = author.splitPlayerNameAndExtras()
        val rankColor =
            if (rank != null /*&& rank.sampleAtStart() === name.sampleAtStart()*/) StringUtils.getFormatFromString(rank.string) else ""
        val coloredName = createColoredName(name, levelColor, name.string.removeColor(), rankColor)
        return if (config.playerRankHider || rank == null) coloredName else rank.append(coloredName)
    }

    private fun createColoredName(
        name: Component,
        levelColor: String?,
        removeColor: String,
        rankColor: String,
    ): Component = when {
        MarkedPlayerManager.isMarkedPlayer(removeColor) && MarkedPlayerManager.config.highlightInChat ->
            (MarkedPlayerManager.replaceInChat(rankColor + removeColor)).asComponent()
                //.setStyle(name.sampleStyleAtStart())

        levelColor != null && config.useLevelColorForName ->
            (levelColor + removeColor).asComponent()
              //  .setStyle(name.sampleStyleAtStart())


        config.playerRankHider ->
            removeColor.asComponent()
                .style { withColor(ChatFormatting.AQUA) }
              //  .setStyle(name.sampleStyleAtStart())


        else ->
            if (rankColor.isEmpty()) name
            else (rankColor + removeColor).asComponent()
               // .setStyle(name.sampleStyleAtStart())
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
