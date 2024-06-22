package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.partyConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.CollectionUtils.removeFirst
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Party : ScoreboardElement() {
    override fun getDisplay() = buildList {
        if (PartyAPI.partyMembers.isEmpty() && informationFilteringConfig.hideEmptyLines) listOf(HIDDEN)
        else {
            add(if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})")
            PartyAPI.partyLeader?.let { leader -> add(" §7- §f$leader §e♚") }
            PartyAPI.partyMembers
                .take(partyConfig.maxPartyList)
                .removeFirst { it == PartyAPI.partyLeader }
                .forEach {
                    add(" §7- §f$it")
                }
        }
    }

    override fun showWhen() =
        when {
            DungeonAPI.inDungeon() -> false // Hidden because the scoreboard lines already exist
            partyConfig.showPartyEverywhere -> true
            else -> inAnyIsland(IslandType.DUNGEON_HUB, IslandType.KUUDRA_ARENA, IslandType.CRIMSON_ISLE) || MiningAPI.inColdIsland()
        }

    override val configLine = "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fSkirtwearer"

    override fun showIsland() = !DungeonAPI.inDungeon()
}
