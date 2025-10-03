package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import com.google.gson.annotations.Expose
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
abstract class BucketedItemTrackerData<E : Enum<E>, T : SessionUptime>(clazz: KClass<E>, session: KClass<T>) : ItemTrackerData<T>(session) {

    final override fun getDescription(timesGained: Long): List<String> =
        throw UnsupportedOperationException("Use getDescription(bucket, timesGained) instead")

    abstract fun getDescription(bucket: E?, timesGained: Long): List<String>

    final override fun getCoinName(item: TrackedItem): String =
        throw UnsupportedOperationException("Use getCoinName(bucket, item) instead")

    abstract fun getCoinName(bucket: E?, item: TrackedItem): String

    final override fun getCoinDescription(item: TrackedItem): List<String> =
        throw UnsupportedOperationException("Use getCoinDescription(bucket, item) instead")

    abstract fun getCoinDescription(bucket: E?, item: TrackedItem): List<String>

    final override fun addItem(internalName: NeuInternalName, amount: Long, command: Boolean) =
        throw UnsupportedOperationException("Use addItem(bucket, internalName, amount) instead")

    final override fun addItem(internalName: NeuInternalName, amount: Int, command: Boolean) =
        throw UnsupportedOperationException("Use addItem(bucket, internalName, amount) instead")

    fun addItem(bucket: E, internalName: NeuInternalName, stackSize: Long, command: Boolean, timesAdded: Long = 1) {
        val bucketMap = bucketedItems.getOrPut(bucket) { HashMap() }
        val item = bucketMap.getOrPut(internalName) { TrackedItem() }
        item.processAdd(internalName, stackSize, command, timesAdded) {
            removeItem(bucket, internalName)
        }
    }

    fun addItem(bucket: E, internalName: NeuInternalName, stackSize: Int, command: Boolean, timesAdded: Int = 1) {
        addItem(bucket, internalName, stackSize.toLong(), command, timesAdded.toLong())
    }

    @Deprecated("Make data class extend Resettable instead")
    final override fun reset() {
        super.reset()
        bucketedItems.clear()
        selectedBucket = null
    }

    final override fun removeItem(internalName: NeuInternalName) =
        throw UnsupportedOperationException("Use removeItem(bucket, internalName) instead")

    fun removeItem(bucket: E?, internalName: NeuInternalName) {
        bucket?.let {
            bucketedItems[bucket]?.remove(internalName)
        } ?: bucketedItems.forEach {
            it.value.remove(internalName)
        }
    }

    final override fun toggleItemHide(internalName: NeuInternalName, currentlyHidden: Boolean) =
        throw UnsupportedOperationException("Use toggleItemHide(bucket, internalName, currentlyHidden) instead")

    fun toggleItemHide(bucket: E?, internalName: NeuInternalName, currentlyHidden: Boolean) {
        bucket?.let {
            bucketedItems[bucket]?.get(internalName)?.hidden = !currentlyHidden
        } ?: bucketedItems.forEach { (_, items) ->
            items[internalName]?.hidden = !currentlyHidden
        }
    }

    abstract fun E.isBucketSelectable(): Boolean

    abstract fun bucketName(): String

    protected val buckets: Array<E> = clazz.java.enumConstants
    val selectableBuckets: List<E> = buckets.filter { it.isBucketSelectable() }

    private val scrollValues: Map<E?, ScrollValue> = buckets.associateWith { ScrollValue() } + (null to ScrollValue())
    val selectedScrollValue: ScrollValue get() = scrollValues[selectedBucket] ?: ScrollValue()

    @Expose
    var selectedBucket: E? = null

    @Expose
    val bucketedItems: MutableMap<E, MutableMap<NeuInternalName, TrackedItem>> = mutableMapOf()

    fun getBucketedItems(bucket: E) = bucketedItems[bucket] ?: flattenBucketsItems()

    protected val E.items get() = bucketedItems[this] ?: mutableMapOf()
    val selectedBucketItems get() = selectedBucket?.items ?: flattenBucketsItems()

    open fun flattenBucketsItems(): MutableMap<NeuInternalName, TrackedItem> =
        buckets.distinct().fold(mutableMapOf()) { acc, bucket ->
            bucket.items.entries.distinctBy { it.key }
                .forEach { (key, value) ->
                    acc.merge(key, value, ::mergeBuckets)
                }
            acc
        }

    protected fun mergeBuckets(existing: TrackedItem, new: TrackedItem): TrackedItem = existing.copy(
        hidden = false,
        totalAmount = existing.totalAmount + new.totalAmount,
        timesGained = existing.timesGained + new.timesGained,
        lastTimeUpdated = maxOf(existing.lastTimeUpdated, new.lastTimeUpdated),
    )
}
