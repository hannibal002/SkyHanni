package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.VipVisitsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.commands.suggestions.LazySuggestionEntry
import at.hannibal2.skyhanni.features.commands.suggestions.SuggestionProvider
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object PlayerTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete
    private var vipVisits = listOf<String>()

    private val friendsEntry = lazyEntry { PlayerCategory.FRIENDS.usernames().toList() }
    private val partyMembersEntry = lazyEntry { PlayerCategory.PARTY.usernames().toList() }
    private val guildMembersEntry = lazyEntry { PlayerCategory.GUILD.usernames().toList()}
    private val vipVisitsEntry = lazyEntry { vipVisits }
    private val islandPlayersEntry = lazyEntry { PlayerCategory.ISLAND_PLAYERS.usernames().toList() }

    private val suggestions = SuggestionProvider.build {
        parent("f", "friend") {
            parent("accept", "add", "deny") { add(getExcluding(PlayerCategory.FRIENDS)) }
            parent("best") { add(friendsEntry) }
            parent("remove", "nickname") { add(friendsEntry) }
            parent("list") { literal("best") }
            literal("help", "notifications", "removeall", "requests")
        }

        parent("g", "guild") {
            parent("invite") { add(getExcluding(PlayerCategory.GUILD)) }
            parent("kick", "transfer", "setrank", "promote", "demote") { add(guildMembersEntry) }
            parent("mute", "unmute") {
                add(guildMembersEntry)
                literal("everyone")
            }
            parent("member") { add(guildMembersEntry) }
            literal(
                "top", "toggle", "tagcolor", "tag", "slow", "settings", "rename", "quest", "permissions", "party", "onlinemode",
                "online", "officerchat", "notifications", "mypermissions", "motd", "menu", "members", "log", "leave", "info", "history",
                "help", "discord", "disband", "create", "chat", "accept",
            )
        }

        parent("p", "party") {
            parent("accept", "invite") { add(getExcluding(PlayerCategory.PARTY)) }
            conditional({ PartyApi.partyMembers.isNotEmpty() }) {
                parent("kick", "demote", "promote", "transfer") { add(partyMembersEntry) }
                literal("chat", "disband", "kickoffline", "leave", "list", "mute", "poll", "private", "settings", "warp")
            }
            add(getExcluding(PlayerCategory.PARTY))
        }

        parent("w", "msg", "tell", "boop", "boo") { add(getExcluding()) }

        parent("visit") {
            add(getExcluding())
            conditional({ config.vipVisits }) {
                add(vipVisitsEntry)
            }
        }

        parent("invite") { add(getExcluding()) }
        parent("ah") { add(getExcluding()) }

        parent("pv") { add(getExcluding()) }
        parent("shmarkplayer") { add(getExcluding()) }

        parent("trade") { add(islandPlayersEntry) }
    }

    private fun getExcluding(vararg excluded: PlayerCategory) = LazySuggestionEntry {

        fun allowed(category: PlayerCategory): Boolean = when (category) {
            PlayerCategory.FRIENDS -> config.friends
            PlayerCategory.ISLAND_PLAYERS -> config.islandPlayers
            PlayerCategory.PARTY -> config.party
            PlayerCategory.GUILD -> config.guild
            else -> true
        }

        val excludedSet = excluded.toSet()

        PlayerCategory.entries
            .filter { it !in excludedSet }
            .filter { allowed(it) }
            .flatMap { it.usernames() }
            .forEach { add(it) }
    }

    private fun lazyEntry(getter: () -> List<String>) = LazySuggestionEntry { addAll(getter()) }

    fun handleTabComplete(command: String): List<String>? = suggestions.getSuggestions(command).takeIf {
        it.isNotEmpty()
    }?.distinct()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<VipVisitsJson>("VipVisits")
        vipVisits = data.vipVisits
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.tabCompleteCommands", "commands.tabComplete")
    }
}
