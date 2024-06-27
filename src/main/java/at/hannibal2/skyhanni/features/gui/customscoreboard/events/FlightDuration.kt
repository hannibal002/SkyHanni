package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.FlightDurationAPI
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.TimeUtils.format

// internal
object FlightDuration : ScoreboardEvent() {
    override fun getDisplay() = "Flight Duration: §a${FlightDurationAPI.flightDuration.format(maxUnits = 2)}"

    override fun showWhen() = FlightDurationAPI.isFlyingActive()

    override val configLine = "Flight Duration: §a10m 0s"

    override fun showIsland() = inAnyIsland(
        IslandType.PRIVATE_ISLAND,
        IslandType.PRIVATE_ISLAND_GUEST,
        IslandType.GARDEN,
        IslandType.GARDEN_GUEST,
    )
}
