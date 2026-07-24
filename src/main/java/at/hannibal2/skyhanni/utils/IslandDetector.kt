package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

class IslandDetector(
    val onIslandJoin: (island: IslandType) -> Unit,
    val onIslandLeave: (island: IslandType) -> Unit,
    val checkIslandType: (island: IslandType) -> Boolean
) {
    init {
        detectors.add(this)
    }

    private var inIsland = false

    /**
     * Check if the player is currently inside this inventory.
     */
    fun isInside() = inIsland

    constructor(
        island: IslandType,
        onIslandJoin: (island: IslandType) -> Unit = {},
        onIslandLeave: (island: IslandType) -> Unit = {}
    ) : this(onIslandJoin, onIslandLeave, { it == island })

    constructor(
        islandTag: IslandTypeTag,
        onIslandJoin: (island: IslandType) -> Unit = {},
        onIslandLeave: (island: IslandType) -> Unit = {}
    ) : this(onIslandJoin, onIslandLeave, { it in islandTag })

    private fun updateIslandState(oldIsland: IslandType, newIsland: IslandType) {
        if (checkIslandType(newIsland)) {
            inIsland = true
            onIslandJoin(newIsland)
        } else {
            inIsland = false
            onIslandLeave(oldIsland)
        }
    }

    @SkyHanniModule
    companion object {
        private val detectors = mutableListOf<IslandDetector>()

        @HandleEvent(priority = HandleEvent.HIGHEST)
        fun onIslandChange(event: IslandChangeEvent) {
            detectors.forEach { it.updateIslandState(event.oldIsland, event.newIsland) }
        }
    }
}
