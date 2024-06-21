package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.data.FlightDurationAPI
import at.hannibal2.skyhanni.utils.TimeUtils.format

object FlightDuration : Event() {
    override fun getDisplay() = listOf("Flight Duration: §a${FlightDurationAPI.flightDuration.format(maxUnits = 2)}")

    override fun showWhen() = FlightDurationAPI.isFlyingActive()

    override val configLine = "Flight Duration: §a10m 0s"
}
