package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.entity.EntityMoveEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.player.LocalPlayer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tracks the player's position over the last 30 seconds using move events and timestamps.
 * Used by features that need to know how long the player has been near a location.
 */
@SkyHanniModule
object PlayerPosData {

    private val maxAge = 30.seconds

    data class TimedPos(val pos: LorenzVec, val time: SimpleTimeMark = SimpleTimeMark.now())

    private val positions = ArrayDeque<TimedPos>()

    val positionHistory: List<TimedPos> get() = positions

    @HandleEvent
    fun onWorldChange() {
        positions.clear()
    }

    @HandleEvent
    fun onEntityMove(event: EntityMoveEvent<LocalPlayer>) {
        positions.addFirst(TimedPos(event.newLocation))
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        while (positions.lastOrNull()?.time?.passedSince()?.let { it > maxAge } == true) {
            positions.removeLast()
        }
    }

    /** Returns how long the player has been within [distance] of [pos], or null if not currently there. */
    fun timeAtPos(pos: LorenzVec, distance: Double): Duration? {
        val first = positions.firstOrNull() ?: return null
        if (first.pos.distance(pos) > distance) return null
        val timedPos = positions.firstOrNull { it.pos.distance(pos) > distance }
            ?: return positions.last().time.passedSince()
        return timedPos.time.passedSince()
    }

    /** Returns how long ago the player was last within [distance] of [pos], or null if never tracked. */
    fun timeSinceLastAt(pos: LorenzVec, distance: Double): Duration? =
        positions.firstOrNull { it.pos.distance(pos) <= distance }?.time?.passedSince()

    @HandleEvent
    fun debug(event: DebugDataCollectEvent) {
        event.title("PlayerPosData")
        event.addIrrelevant {
            add("Tracked positions: ${positions.size}")
            val oldest = positions.lastOrNull()?.time?.passedSince()
            val newest = positions.firstOrNull()?.time?.passedSince()
            add("Age range: $oldest to $newest")
        }
    }
}
