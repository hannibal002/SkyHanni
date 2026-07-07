package at.hannibal2.skyhanni.api.event

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.EventListeners.Listener
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.test.command.ErrorManager.maybeSkipError
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.takeIfNotEmpty
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import net.minecraft.ChatFormatting

class EventHandler<T : SkyHanniEvent> private constructor(
    val name: String,
    listeners: List<Listener>,
) {

    val invokeLog = SkyHanniEvents.EventInvokeLog()

    @Suppress("ArrayInDataClass")
    private data class Bucket(
        val listeners: Array<Listener>,
        val nextCancelled: IntArray,
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

            val nextCancelled = IntArray(listenerArray.size) { -1 }

            var nextCancelledIndex = -1
            for (i in listenerArray.lastIndex downTo 0) {
                nextCancelled[i] = nextCancelledIndex

                if (listenerArray[i].receiveCancelled) {
                    nextCancelledIndex = i
                }
            }

            Bucket(
                listeners = listenerArray,
                nextCancelled = nextCancelled,
            )
        }
    }

    constructor(event: Class<T>, listeners: List<Listener>) : this(
        (event.name.split(".").lastOrNull() ?: event.name).replace("$", "."),
        listeners.sortedBy { it.priority },
    )

    // FIXME: this assumes the current event is not cancelled when it is posted
    fun post(event: T, onError: ((Throwable) -> Unit)? = null): Boolean {
        invokeLog.invokeCount++
        val bucket = buckets.getOrNull(SkyHanniEvents.getCurrentStateIndex()) ?: return false
        if (SkyHanniEvents.isDisabledHandler(name)) return false

        var errors = 0
        var index = 0

        while (index >= 0 && index < bucket.listeners.size) {
            val listener = bucket.listeners[index]

            if (listener.shouldInvoke(event)) {
                try {
                    listener.invoker.accept(event)
                } catch (originalThrowable: Throwable) {
                    val throwable = originalThrowable.maybeSkipError()
                    errors++

                    if (errors <= 3) {
                        val errorName = throwable::class.simpleName ?: "error"
                        val aOrAn = StringUtils.optionalAn(errorName)
                        val message = "Caught $aOrAn $errorName in ${listener.name} at $name: ${throwable.message}"
                        ErrorManager.logErrorWithData(throwable, message, ignoreErrorCache = onError != null)
                    }

                    onError?.invoke(throwable)
                }
            }

            index = if (event.isCancelled) {
                bucket.nextCancelled[index]
            } else {
                index + 1
            }
        }

        if (errors > 3) {
            val hiddenErrors = errors - 3
            ChatUtils.chat(
                componentBuilder {
                    append("[SkyHanni/${SkyHanniMod.VERSION}] $hiddenErrors more errors in $name are hidden!")
                    withColor(ChatFormatting.RED)
                }
            )
        }
        return event.isCancelled
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
                islands.forEach { island ->
                    add(island + ISLAND_OFFSET)
                }
            }
        }

        private fun getIslands(options: HandleEvent): List<IslandType> {
            val islandTypes = mutableSetOf<IslandType>()
            options.onlyOnIsland.takeIf { it != IslandType.ANY }?.let { islandTypes.add(it) }
            islandTypes.addAll(options.onlyOnIslands)
            options.onlyOnIslandTypeTag.takeIfNotEmpty()?.forEach { tag ->
                islandTypes.addAll(tag.getTypes())
            }
            return islandTypes.toList()
        }
    }
}
