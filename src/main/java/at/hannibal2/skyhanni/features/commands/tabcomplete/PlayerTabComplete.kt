package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.commands.brigadier.PlayerSuggestions
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.VipVisitsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.commands.suggestions.LazySuggestionEntry
import at.hannibal2.skyhanni.features.commands.suggestions.SuggestionProvider
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource.BEST_FRIENDS
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource.CARRY_CUSTOMER
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource.FRIENDS
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource.GUILD
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource.ISLAND_PLAYERS
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource.PARTY
import at.hannibal2.skyhanni.features.commands.tabcomplete.PlayerNameSource.SELF
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object PlayerTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete
    private var vipVisits = listOf<String>()

    private val vipVisitsEntry = lazyEntry { vipVisits }
    private val allPlayers = getExcluding()

    // TODO: cache lazy entries, and also cache getExcluding calls with the same parameters
    private val suggestions = SuggestionProvider.build {
        parent("f", "friend") {
            parent("accept", "add", "deny") { add(getExcluding(FRIENDS)) }
            parent("best") { add(FRIENDS.lazyEntry()) }
            parent("remove", "nickname") { add(FRIENDS.lazyEntry()) }
            parent("list") { literal("best") }
            literal("help", "notifications", "removeall", "requests")
        }

        parent("g", "guild") {
            parent("invite") { add(getExcluding(GUILD)) }
            parent("kick", "transfer", "setrank", "promote", "demote") { add(GUILD.lazyEntry()) }
            parent("mute", "unmute") {
                add(GUILD.lazyEntry())
                literal("everyone")
            }
            parent("member") { add(GUILD.lazyEntry()) }
            literal(
                "top", "toggle", "tagcolor", "tag", "slow", "settings", "rename", "quest", "permissions", "party", "onlinemode",
                "online", "officerchat", "notifications", "mypermissions", "motd", "menu", "members", "log", "leave", "info", "history",
                "help", "discord", "disband", "create", "chat", "accept",
            )
        }

        parent("p", "party") {
            parent("accept", "invite") { add(getExcluding(PARTY)) }
            conditional({ PartyApi.partyMembers.isNotEmpty() }) {
                parent("kick", "demote", "promote", "transfer") { add(PARTY.lazyEntry()) }
                literal("chat", "disband", "kickoffline", "leave", "list", "mute", "poll", "private", "settings", "warp")
            }
            add(getExcluding(PARTY))
        }

        parent("w", "msg", "tell", "boop", "boo") { add(allPlayers) }

        parent("visit") {
            add(allPlayers)
            conditional({ config.vipVisits }) {
                add(vipVisitsEntry)
            }
        }

        parent("invite") { add(allPlayers) }
        parent("ah") { add(allPlayers) }

        parent("pv") {
            add(allPlayers)
            add(SELF.lazyEntry())
        }

        parent("trade") { add(ISLAND_PLAYERS.lazyEntry()) }
    }

    private fun getExcluding(vararg excluded: PlayerNameSource) = LazySuggestionEntry {

        fun allowed(category: PlayerNameSource): Boolean = when (category) {
            FRIENDS -> config.friends
            ISLAND_PLAYERS -> config.islandPlayers
            PARTY -> config.party
            GUILD -> config.guild
            CARRY_CUSTOMER -> config.carryCustomer
            else -> false
        }

        val allowedSet = PlayerNameSource.entries.filter {
            it !in excluded && allowed(it)
        }.toMutableSet()

        if (config.onlyBestFriends) {
            if (allowedSet.remove(FRIENDS)) {
                allowedSet.add(BEST_FRIENDS)
            }
        }

        addAll(
            PlayerSuggestions.buildPlayerSuggestions {
                include(allowedSet)
            }.getSequence()
        )
    }

    private fun PlayerNameSource.lazyEntry() = lazyEntry { usernames }

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
