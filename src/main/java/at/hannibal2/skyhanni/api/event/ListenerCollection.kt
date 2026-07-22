package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.api.event.EventListeners.Listener
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.SkyBlockUtils

class ListenerCollection(
    listeners: List<Listener>,
) {

    @Suppress("ArrayInDataClass")
    data class Bucket(
        val listeners: Array<Listener>,
        val nextAfterCancellation: IntArray,
    )
    private val buckets: Array<Bucket?>

    init {
        val localBuckets = arrayOfNulls<MutableList<Listener>>(BUCKET_COUNT)

        listeners.forEach { listener ->
            listener.indices.forEach { index ->
                val bucket = localBuckets[index]
                if (bucket != null) {
                    bucket.add(listener)
                } else {
                    localBuckets[index] = mutableListOf(listener)
                }
            }
        }

        buckets = Array(BUCKET_COUNT) { index ->
            val bucketListeners = localBuckets[index] ?: return@Array null
            val listenerArray = bucketListeners.toTypedArray()

            val nextAfterCancellation = IntArray(listenerArray.size) { -1 }

            var nextCancelledIndex = -1
            for (i in listenerArray.lastIndex downTo 0) {
                nextAfterCancellation[i] = nextCancelledIndex

                if (listenerArray[i].receiveCancelled) {
                    nextCancelledIndex = i
                }
            }

            Bucket(
                listeners = listenerArray,
                nextAfterCancellation = nextAfterCancellation,
            )
        }
    }

    fun current(): Bucket? =
        buckets.getOrNull(SkyHanniEvents.getCurrentStateIndex())

    fun isEmpty(): Boolean =
        buckets.all { it == null }

    inline fun forEachCurrent(action: (Listener) -> Boolean) {
        val bucket = current() ?: return

        val listeners = bucket.listeners
        val nextReceiveCancelled = bucket.nextAfterCancellation

        var index = 0
        while (index < listeners.size) {
            val shouldContinue = action(listeners[index])

            index = if (shouldContinue) {
                index + 1
            } else {
                nextReceiveCancelled[index]
            }
        }
    }

    companion object {

        const val OUTSIDE = 0

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

            return buildList {
                islands.forEach {
                    add(it + ISLAND_OFFSET)
                }
            }
        }

        private fun getIslands(options: HandleEvent): List<IslandType> {
            val islandTypes = mutableSetOf<IslandType>()

            options.onlyOnIsland
                .takeIf { it != IslandType.ANY }
                ?.let(islandTypes::add)

            islandTypes += options.onlyOnIslands

            options.onlyOnIslandTypeTag.forEach { tag -> islandTypes += tag.getTypes() }

            return islandTypes.toList()
        }
    }
}
