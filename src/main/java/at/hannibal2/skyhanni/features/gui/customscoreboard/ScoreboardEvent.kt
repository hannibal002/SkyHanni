package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.eventsConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getElementFromAny
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardEvents.VOTING
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.ActiveTablist
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Anniversary
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Broodmother
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Carnival
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Damage
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.DarkAuction
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Dojo
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Dungeons
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Essence
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Event
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.FlightDuration
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.GardenCleanUp
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.GardenPasting
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.JacobContest
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.JacobMedals
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Kuudra
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.MagmaBoss
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Mining
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.NewYear
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Queue
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Redstone
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Rift
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.ServerClose
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Spooky
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.StartingSoonTablist
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Trapper
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Voting
import at.hannibal2.skyhanni.features.gui.customscoreboard.events.Winter

/**
 * This enum contains all the lines that either are events or other lines that are so rare/not often seen that they
 * don't fit in the normal [ScoreboardElement] enum.
 *
 * We for example have the [VOTING] Event, while this is clearly not an event, I don't consider them as normal lines
 * because they are visible for a maximum of like 1 minute every 5 days and ~12 hours.
 */

enum class ScoreboardEvent(val event: Event) {
    VOTING(Voting),
    SERVER_CLOSE(ServerClose),
    DUNGEONS(Dungeons),
    KUUDRA(Kuudra),
    DOJO(Dojo),
    DARK_AUCTION(DarkAuction),
    JACOB_CONTEST(JacobContest),
    JACOB_MEDALS(JacobMedals),
    TRAPPER(Trapper),

    // TODO: Combine Cleanup & Pasting into Garden
    GARDEN_CLEAN_UP(GardenCleanUp),
    GARDEN_PASTING(GardenPasting),
    FLIGHT_DURATION(FlightDuration),
    WINTER(Winter),
    NEW_YEAR(NewYear),
    SPOOKY(Spooky),
    BROODMOTHER(Broodmother),
    MINING_EVENTS(Mining),
    DAMAGE(Damage),
    MAGMA_BOSS(MagmaBoss),
    CARNIVAL(Carnival),
    RIFT(Rift),
    ESSENCE(Essence),
    QUEUE(Queue),
    ANNIVERSARY(Anniversary),
    ACTIVE_TABLIST_EVENTS(ActiveTablist),
    STARTING_SOON_TABLIST_EVENTS(StartingSoonTablist),
    REDSTONE(Redstone),
    ;

    override fun toString() = event.configLine

    fun getLines(): List<ScoreboardElementType> = event.getDisplay().map { getElementFromAny(it) }

    companion object {
        fun getEvent(): List<ScoreboardEvents> =
            eventsConfig.eventEntries.filter { it.event.showWhen() && it.getLines().isNotEmpty() }.let { events ->
                events.takeIf { eventsConfig.showAllActiveEvents } ?: listOfNotNull(events.firstOrNull())
            }

        @JvmField
        val defaultOption = listOf(
            VOTING,
            SERVER_CLOSE,
            DUNGEONS,
            KUUDRA,
            DOJO,
            DARK_AUCTION,
            JACOB_CONTEST,
            JACOB_MEDALS,
            TRAPPER,
            GARDEN_CLEAN_UP,
            GARDEN_PASTING,
            FLIGHT_DURATION,
            NEW_YEAR,
            WINTER,
            SPOOKY,
            BROODMOTHER,
            MINING_EVENTS,
            DAMAGE,
            MAGMA_BOSS,
            CARNIVAL,
            RIFT,
            ESSENCE,
            ACTIVE_TABLIST_EVENTS,
            REDSTONE,
        )
    }
}
