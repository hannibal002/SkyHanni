package at.hannibal2.skyhanni.data.hypixel.chat

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.hypixel.chat.event.AbstractSourcedChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.CoopChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.GuildChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.NpcChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PartyChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerAllChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PlayerShowItemChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.PrivateMessageChatEvent
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextColor
import java.util.regex.Matcher

/**
 * Reading normal chat events, and splitting them up into many different player chat events, with all available extra information
 */
@SkyHanniModule
object PlayerChatManager {

    private val patternGroup = RepoPattern.group("data.chat.player")

    /**
     * REGEX-TEST: [58] §7nea89o§7: haiiiii
     * REGEX-TEST: [266] ♫ §b[MVP§d+§b] lrg89§f: a
     * REGEX-TEST: [302] ♫ [MVP+] lrg89: problematic
     */
    private val globalPattern by patternGroup.pattern(
        "global",
        "^(?:\\[(?<level>\\d+)] )?(?<author>(?:[^ ] )?(?:(?:§.)?\\[[^\\]]+\\] )?[^ ]+?)(?<chatColor>§f|§7|)?: (?<message>.*)\$",
    )

    /**
     * REGEX-TEST: §9Party §8> §b[MVP§d+§b] lrg89§f: peee
     * REGEX-TEST: §9Party §8> §7nea89o§f: peee
     */
    private val partyPattern by patternGroup.pattern(
        "party",
        "§9Party §8> (?<author>[^:]*)§f: (?<message>.*)",
    )

    /**
     * REGEX-TEST: §bCo-op > §7nea89o§f: hallooooo
     * REGEX-TEST: §bCo-op > §b[MVP§5+§b] Throwpo§f: hi
     */
    private val coopPattern by patternGroup.pattern(
        "coop",
        "§bCo-op > (?<author>[^:]+)§f: (?<message>.*)",
    )

    /**
     * REGEX-TEST: §2Guild > §b[MVP§d+§b] infave §e[Em]§f: CEMENT DRINKERS INCORPORATED
     * REGEX-TEST: §2Guild > §6⚔ §6[MVP§3++§6] RealBacklight§f: !warp
     * REGEX-TEST: §2Guild > §b[MVP§d+§b] lrg89 §e[Iron]§f: h
     * REGEX-TEST: §2Guild > §b[MVP§c+§b] B2Square1 §3[IRON]§f: §r700 to go
     * REGEX-TEST: §2Guild > §6[MVP§5++§6] Throwpo §3[IRON]§f: §rbat pet clueless
     */
    private val guildPattern by patternGroup.pattern(
        "guild",
        "§2Guild > (?<author>.+?) ?(?<guildRank>§.\\[\\w*])?§f: (?<message>.*)",
    )

    /**
     * REGEX-TEST: To nea89o: lol
     * REGEX-TEST: From nea89o: hiii
     * REGEX-FAIL: From stash: Pufferfish
     * REGEX-FAIL: From stash: Wheat
     * REGEX-TEST: To [MVP+] Eisengolem: Boop!
     * REGEX-TEST: From [MVP+] Eisengolem: Boop!
     * REGEX-TEST: To [MVP+] Eisengolem: danke
     */
    private val privateMessagePattern by patternGroup.pattern(
        "privatemessage",
        "^(?!From stash: )(?<direction>From|To) (?<author>[^:]*): (?<message>.*)",
    )

    /**
     * REGEX-TEST: §b[MVP§c+§b] hannibal2§f§7 is holding §8[§6Heroic Aspect of the Void§8]
     * REGEX-TEST: §b[MVP§c+§b] hannibal2§f§7 is holding §8[§7[Lvl 2] §dSpider§8]
     * REGEX-TEST: §b[MVP§c+§b] hannibal2§f§7 is friends with a §8[§7[Lvl 200] §8[§6103§8§4✦§8] §6Golden Dragon§8]
     * REGEX-TEST: §b[MVP§c+§b] hannibal2§f§7 is wearing §8[§5Glistening Implosion Belt§8]
     * REGEX-TEST: §b[MVP§c+§b] hannibal2§f§7 is friends with a §8[§7[Lvl 100] §dEnderman§8]
     * REGEX-TEST: §b[MVP§c+§b] hannibal2§f§7 has §8[§6Heroic Aspect of the Void§8]
     * REGEX-TEST: §8[§b209§8] §b[MVP§d+§b] lrg89§f§7 is holding §8[§5Heroic Aspect of the Void§8]
     * REGEX-TEST: §8[§2179§8] §r§b[MVP§c+§b] Frogthink§f §7♲§7 is holding §r§8[§dBlessed Melon Dicer 3.0§8]
     * REGEX-TEST: §8[§2164§8] §6§lᛝ §r§7Vinc1x§7§7 is holding §r§8[§dStellar Titanium Drill DR-X655§8]
     */
    @Suppress("MaxLineLength")
    private val itemShowPattern by patternGroup.pattern(
        "itemshow",
        "(?:§8\\[(?<level>§.\\d+)§8] )?(?<author>.*)§.(?: §7♲)*?§7 (?<action>is (?:holding|friends with a|wearing)|has) (?<itemName>.*)",
    )

    /**
     * REGEX-TEST: ♫ §c[Buddy ツ] §b[MVP§d+§b] lrg89
     * REGEX-FAIL: ℻ §b[MVP§5+§b] Alea1337
     */
    private val privateIslandRankPattern by patternGroup.pattern(
        "privateislandrank",
        "(?<prefix>.*?)(?<privateIslandRank>§.\\[(?!MVP(?:§.\\++)?§.]|VIP\\+*|YOU§.TUBE|ADMIN|MOD|GM)[^]]+\\]) (?<suffix>.*)",
    )

    /**
     * REGEX-TEST: ♫ §a[✌] §f[Gamer] §b[MVP§d+§b] lrg89
     * REGEX-FAIL: ℻ §b[MVP§5+§b] Alea1337
     * REGEX-TEST: ♫ §a[✌] §c[Buddy ツ] §b[MVP§d+§b] lrg89
     */
    private val privateIslandGuestPattern by patternGroup.pattern(
        "privateislandguest",
        "(?<prefix>.*)(?<guest>§a\\[✌] )(?<suffix>.*)",
    )

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val chatComponent = event.chatComponent
        coopPattern.matchMatcher(chatComponent) {
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            CoopChatEvent.Allow(author, message, chatComponent).postChat(event)
            return
        }
        partyPattern.matchMatcher(chatComponent) {
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            PartyChatEvent.Allow(author, message, chatComponent).postChat(event)
            return
        }
        guildPattern.matchMatcher(chatComponent) {
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            val rank = groupOrThrow("guildRank", chatComponent)
            GuildChatEvent.Allow(
                author,
                message,
                rank,
                chatComponent,
            ).postChat(event)
            return
        }
        privateMessagePattern.matchMatcher(chatComponent) {
            val direction = groupOrThrow("direction", chatComponent)
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            PrivateMessageChatEvent.Allow(direction.string, author, message, chatComponent).postChat(event)
            return
        }
        itemShowPattern.matchMatcher(chatComponent) {
            val level = TextHelper.matcher(chatComponent, group("level"))
            val author = groupOrThrow("author", chatComponent)
            val action = groupOrThrow("action", chatComponent)
            val itemName = groupOrThrow("itemName", chatComponent)

            PlayerShowItemChatEvent.Allow(
                level,
                action,
                author,
                itemName,
                author.copy().append(action).append(itemName),
                chatComponent,
            ).postChat(event)
            return
        }
        globalPattern.matchMatcher(chatComponent) {
            if (isGlobalChat(event)) return
        }

        sendSystemMessage(event)
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Modify) {
        val chatComponent = event.chatComponent
        coopPattern.matchMatcher(chatComponent) {
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            CoopChatEvent.Modify(author, message, chatComponent).postChat(event)
            return
        }
        partyPattern.matchMatcher(chatComponent) {
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            PartyChatEvent.Modify(author, message, chatComponent).postChat(event)
            return
        }
        guildPattern.matchMatcher(chatComponent) {
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            val rank = TextHelper.matcher(chatComponent, group("guildRank"))
            GuildChatEvent.Modify(
                author,
                message,
                rank,
                chatComponent,
            ).postChat(event)
            return
        }
        privateMessagePattern.matchMatcher(chatComponent) {
            val direction = groupOrThrow("direction", chatComponent)
            val author = groupOrThrow("author", chatComponent)
            val message = groupOrThrow("message", chatComponent)
            PrivateMessageChatEvent.Modify(direction.string, author, message, chatComponent).postChat(event)
            return
        }
        itemShowPattern.matchMatcher(chatComponent) {
            val level = TextHelper.matcher(chatComponent, group("level"))
            val author = groupOrThrow("author", chatComponent)
            val action = groupOrThrow("action", chatComponent)
            val itemName = groupOrThrow("itemName", chatComponent)

            PlayerShowItemChatEvent.Modify(
                level,
                action,
                author,
                itemName,
                author.copy().append(action).append(itemName),
                chatComponent,
            ).postChat(event)
            return
        }
        globalPattern.matchMatcher(chatComponent) {
            if (isGlobalChat(event)) return
        }

        sendSystemMessage(event)
    }

    private fun Matcher.isGlobalChat(event: SkyHanniChatEvent.Allow): Boolean {
        var author = groupOrThrow("author", event.chatComponent)
        //val chatColor = groupOrThrow("chatColor")
        //if (chatColor.length == 0 && !author.string.removeColor().endsWith(PlayerUtils.getName())) {
        //    // The last format string is always present, unless this is the players own message
        //    return false
        //}
        val message = groupOrThrow("message", event.chatComponent)
        if (author.string.contains("[NPC]")) {
            NpcChatEvent.Allow(author, message, event.chatComponent).postChat(event)
            return true
        }

        var privateIslandRank: Component? = null
        var privateIslandGuest: Component? = null
        if (IslandTypeTags.PRIVATE_ISLAND.inAny()) {
            privateIslandGuestPattern.matchMatcher(author) {
                privateIslandGuest = groupOrThrow("guest", author)
                val prefix = groupOrThrow("prefix", author)
                val suffix = groupOrThrow("suffix", author)
                author = prefix.copy().append(suffix)
            }
            privateIslandRankPattern.matchMatcher(author) {
                privateIslandRank = groupOrThrow("privateIslandRank", author)
                val prefix = groupOrThrow("prefix", author)
                val suffix = groupOrThrow("suffix", author)
                author = prefix.copy().append(suffix)
            }
        }

        PlayerAllChatEvent.Allow(
            levelComponent = TextHelper.matcher(event.chatComponent, "level"),
            privateIslandRank = privateIslandRank,
            privateIslandGuest = privateIslandGuest,
            chatColor = TextHelper.sampleStyleAtStart(event.messageComponent)?.color ?: TextColor.fromLegacyFormat(ChatFormatting.WHITE)!!,
            authorComponent = author,
            messageComponent = message,
            chatComponent = event.chatComponent,
        ).postChat(event)
        return true
    }

    private fun Matcher.isGlobalChat(event: SkyHanniChatEvent.Modify): Boolean {
        var author = groupOrThrow("author", event.chatComponent)
        //val chatColor = groupOrThrow("chatColor")
        //if (chatColor.length == 0 && !author.string.removeColor().endsWith(PlayerUtils.getName())) {
        //    // The last format string is always present, unless this is the players own message
        //    return false
        //}
        val message = groupOrThrow("message", event.chatComponent)
        if (author.string.contains("[NPC]")) {
            NpcChatEvent.Modify(author, message, event.chatComponent).postChat(event)
            return true
        }

        var privateIslandRank: Component? = null
        var privateIslandGuest: Component? = null
        if (IslandTypeTags.PRIVATE_ISLAND.inAny()) {
            privateIslandGuestPattern.matchMatcher(author) {
                privateIslandGuest = groupOrThrow("guest", author)
                val prefix = groupOrThrow("prefix", author)
                val suffix = groupOrThrow("suffix", author)
                author = prefix.copy().append(suffix)
            }
            privateIslandRankPattern.matchMatcher(author) {
                privateIslandRank = groupOrThrow("privateIslandRank", author)
                val prefix = groupOrThrow("prefix", author)
                val suffix = groupOrThrow("suffix", author)
                author = prefix.copy().append(suffix)
            }
        }

        PlayerAllChatEvent.Modify(
            levelComponent = TextHelper.matcher(event.chatComponent, groupOrNull("level")),
            privateIslandRank = privateIslandRank,
            privateIslandGuest = privateIslandGuest,
            chatColor = TextHelper.sampleStyleAtStart(event.messageComponent)?.color ?: TextColor.fromLegacyFormat(ChatFormatting.WHITE)!!,
            authorComponent = author,
            messageComponent = message,
            chatComponent = event.chatComponent,
        ).postChat(event)
        return true
    }

    private fun sendSystemMessage(event: SkyHanniChatEvent.Allow) {
        with(SystemMessageEvent.Allow(event.message, event.chatComponent)) {
            post()
            event.handleChat(blockedReason)
        }
    }

    private fun sendSystemMessage(event: SkyHanniChatEvent.Modify) {
        with(SystemMessageEvent.Modify(event.message, event.chatComponent)) {
            post()
            event.handleChat(chatComponent)
        }
    }

    private fun AbstractSourcedChatEvent.Allow.postChat(event: SkyHanniChatEvent.Allow) {
        post()
        event.handleChat(blockedReason)
    }

    private fun AbstractSourcedChatEvent.Modify.postChat(event: SkyHanniChatEvent.Modify) {
        post()
        event.handleChat(chatComponent)
    }

    private fun SkyHanniChatEvent.Allow.handleChat(blockedReason: String?) {
        blockedReason?.let {
            this.blockedReason = it
        }
    }

    private fun SkyHanniChatEvent.Modify.handleChat(chatComponent: Component) {
        this.replaceComponent(chatComponent, "player_chat_manager")
    }

    fun Matcher.groupOrThrow(group: String, component: Component): Component {
        return TextHelper.matcher(component, group(group)) ?: throw Error()
    }
}
