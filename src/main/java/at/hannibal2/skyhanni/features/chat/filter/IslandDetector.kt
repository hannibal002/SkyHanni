package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

class IslandDetector(
    private val checkIslandType: (island: IslandType) -> Boolean,
) {

    private var inIsland = false
    private val callbacks = mutableSetOf<(oldIsland: IslandType, newIsland: IslandType) -> Unit>()

    constructor(island: IslandType) : this({ it == island })

    constructor(islandTag: IslandTypeTag) : this({ it in islandTag })

    /**
     * Register this detector to receive island changes.
     *
     * Returns this instance for chaining.
     */
    fun register(
        callback: (oldIsland: IslandType, newIsland: IslandType) -> Unit,
    ): IslandDetector {
        if (callbacks.isEmpty()) {
            detectors.add(this)
        }
        callbacks.add(callback)
        return this
    }

    /**
     * Remove a specific callback.
     */
    fun unregister(
        callback: (oldIsland: IslandType, newIsland: IslandType) -> Unit,
    ) {
        callbacks.remove(callback)
        if (callbacks.isEmpty()) {
            unregister()
        }
    }

    /**
     * Completely unregister this detector.
     */
    fun unregister() {
        detectors.remove(this)
        callbacks.clear()
        inIsland = false
    }

    /**
     * Check if the player is currently inside this detector's island.
     */
    fun isInside(): Boolean = inIsland

    private fun updateIslandState(
        oldIsland: IslandType,
        newIsland: IslandType,
    ) {
        val wasInside = inIsland
        val isInside = checkIslandType(newIsland)

        inIsland = isInside

        // Only notify if the state actually changed.
        if (wasInside != isInside) {
            callbacks.forEach { it(oldIsland, newIsland) }
        }
    }

    @SkyHanniModule
    companion object {
        private val detectors = mutableSetOf<IslandDetector>()

        @HandleEvent(priority = HandleEvent.HIGHEST)
        fun onIslandChange(event: IslandChangeEvent) {
            detectors.forEach {
                it.updateIslandState(event.oldIsland, event.newIsland)
            }
        }
    }
}
