package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard
import at.hannibal2.skyhanni.utils.SkyBlockUtils

// internal
// add party update event
object ScoreboardElementParty : ScoreboardElement() {
    private val config get() = CustomScoreboard.displayConfig.party

    // TODO cache until next party update event
    override fun getDisplay() = buildList {
        if (PartyApi.partyMembers.isEmpty() && CustomScoreboard.informationFilteringConfig.hideEmptyLines) return@buildList

        add(if (PartyApi.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyApi.partyMembers.size})")

        if (config.showPartyLeader && PartyApi.partyLeader != null) {
            add(" §7- §f${PartyApi.partyLeader} §e♚")
        }

        if (config.showPartyLeader) {
            PartyApi.partyMembers.filter { it != PartyApi.partyLeader }
        } else {
            PartyApi.partyMembers
        }.take(config.maxPartyList.get()).forEach { add(" §7- §f$it") }
    }

    override fun showWhen() =
        when {
            DungeonApi.inDungeon() -> false // Hidden because the scoreboard lines already exist
            config.showPartyEverywhere -> true
            else -> SkyBlockUtils.inAnyIsland(
                IslandType.DUNGEON_HUB,
                IslandType.KUUDRA_ARENA,
                IslandType.CRIMSON_ISLE,
            ) || IslandTypeTags.IS_COLD.inAny()
        }

    override val configLine = "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fEmpa_\n §7- §fSkirtwearer"

    override fun showIsland() = !DungeonApi.inDungeon()
}

// click (title): run /party list
// click (members): run /pv <name> or  /party kick <name> (maybe option?)
