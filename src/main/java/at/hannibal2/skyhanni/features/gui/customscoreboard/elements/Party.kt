package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MiningAPI.inGlaciteArea
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.partyConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland

object Party : ScoreboardElement() {
    override fun getDisplay() =
        if (PartyAPI.partyMembers.isEmpty() && informationFilteringConfig.hideEmptyLines) listOf(HIDDEN)
        else {
            val title = if (PartyAPI.partyMembers.isEmpty()) "§9§lParty" else "§9§lParty (${PartyAPI.partyMembers.size})"
            val partyList = PartyAPI.partyMembers
                .take(partyConfig.maxPartyList.get())
                .map { " §7- §f$it" }
                .toTypedArray()
            listOf(title, *partyList)
        }

    override fun showWhen() = if (DungeonAPI.inDungeon()) {
        false // Hidden bc the scoreboard lines already exist
    } else {
        if (partyConfig.showPartyEverywhere) {
            true
        } else {
            inAnyIsland(IslandType.DUNGEON_HUB, IslandType.KUUDRA_ARENA, IslandType.CRIMSON_ISLE) || inGlaciteArea()
        }
    }

    override val configLine = "§9§lParty (4):\n §7- §fhannibal2\n §7- §fMoulberry\n §7- §fVahvl\n §7- §fSkirtwearer"
}
