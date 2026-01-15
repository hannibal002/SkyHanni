package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tracks the player's position over the last 30 seconds. THe index of the position in the list
 * corresponds to the time in ticks since the position was recorded, with the most recent position being at index 0.
 * This makes it so that the time can inaccurate by up to 1 tick.
 */
@Suppress("unused")
@SkyHanniModule
object PlayerPosData {

    private const val TIME_SECONDS = 30
    private const val SIZE = TIME_SECONDS * 20

    val time = TIME_SECONDS.seconds

    private val playerPositions = ArrayDeque<LorenzVec>()
    val positions: List<LorenzVec> get() = playerPositions

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onTick() {
        playerPositions.addFirst(LocationUtils.playerLocation())
        if (playerPositions.size > SIZE) playerPositions.removeLast()
    }

    @HandleEvent
    fun onWorldChange() = playerPositions.clear()

    /** Returns the time the player has been at the specified position, within a distance of [distance]. */
    fun timeAtPos(pos: LorenzVec, distance: Double): Duration? {
        val index = playerPositions.indexOfFirst { it.distance(pos) >= distance }
        return when (index) {
            0 -> null // The player is currently not at the position
            -1 -> playerPositions.lastIndex.ticks // all the tracked positions are within the distance
            else -> index.ticks
        }
    }

    /** Returns the time since the player was last at the specified [distance] or less from the specified [pos]. */
    fun timeSinceLastAt(pos: LorenzVec, distance: Double): Duration? {
        val index = playerPositions.indexOfFirst { it.distance(pos) <= distance }
        if (index == -1) return null // The last position isn't within the distance, or no position is within the distance.
        return index.ticks
    }

}
