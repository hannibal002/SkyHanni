package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.api.event.EventListeners.Listener
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.SkyBlockUtils

class ListenerCollection(
    listeners: List<Listener>,
) {

    private val buckets: Array<Array<Listener>?>

    init {
        val sorted = listeners.sortedBy { it.priority }

        val localBuckets = arrayOfNulls<MutableList<Listener>>(BUCKET_COUNT)

        for (listener in sorted) {
            for (index in listener.indices) {
                val bucket = localBuckets[index]
                if (bucket != null) {
                    bucket += listener
                } else {
                    localBuckets[index] = mutableListOf(listener)
                }
            }
        }

        buckets = Array(BUCKET_COUNT) { index ->
            localBuckets[index]?.toTypedArray()
        }
    }

    fun current(): Array<Listener>? =
        buckets.getOrNull(SkyHanniEvents.getCurrentStateIndex())

    inline fun forEachCurrent(
        action: (Listener) -> Boolean,
    ) {
        val listeners = current() ?: return

        for (listener in listeners) {
            if (!action(listener)) return
        }
    }

    companion object {

        const val OUTSIDE = 0

        // Offset applied to island ordinals because bucket 0 
        // is reserved for the outside-SkyBlock state.
        private const val ISLAND_OFFSET = 1
        private val BUCKET_COUNT = IslandType.entries.size + ISLAND_OFFSET

        fun getCurrentStateIndex(): Int {
            if (!SkyBlockUtils.inSkyBlock) return OUTSIDE
            return SkyBlockUtils.currentIsland.ordinal + ISLAND_OFFSET
        }

        fun createListenerIndices(options: HandleEvent): List<Int> {
            val islands = getIslands(options)
                .map { it.ordinal }

            if (islands.isEmpty()) {
                return if (options.onlyOnSkyblock) {
                    (ISLAND_OFFSET until BUCKET_COUNT).toList()
                } else {
                    (OUTSIDE until BUCKET_COUNT).toList()
                }
            }

            return islands.map { it + ISLAND_OFFSET }
        }

        private fun getIslands(options: HandleEvent): List<IslandType> {
            val islandTypes = mutableSetOf<IslandType>()

            options.onlyOnIsland
                .takeUnless { it == IslandType.ANY }
                ?.let(islandTypes::add)

            islandTypes += options.onlyOnIslands

            options.onlyOnIslandTypeTag.forEach { tag -> islandTypes += tag.getTypes() }

            return islandTypes.toList()
        }
    }
}
