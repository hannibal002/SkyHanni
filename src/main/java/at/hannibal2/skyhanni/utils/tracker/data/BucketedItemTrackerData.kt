package at.hannibal2.skyhanni.utils.tracker.data

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.ReflectionUtils.findGenericSuperclassTypeArgument
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.tracker.SessionUptime
import com.google.gson.annotations.Expose

abstract class BucketedItemTrackerData<E : Enum<E>, T : SessionUptime> : ItemTrackerData<T>() {

    // TODO these final overrides that throw exist because this class deliberately breaks the
    //  ItemTrackerData contract for bucket-unaware operations (Liskov violation). The long-term
    //  fix is to split ItemTrackerData so these methods live on a separate interface that
    //  BucketedItemTrackerData simply does not implement.

    final override fun getDescription(timesGained: Long): List<String> =
        throw UnsupportedOperationException("Use getDescription(bucket, timesGained) instead")

    abstract fun getDescription(bucket: E?, timesGained: Long): List<String>

    internal fun getDropRate(dataMap: Map<E, Number>, bucket: E?, timesGained: Long): List<String> {
        val divisor = 1.coerceAtLeast(
            bucket?.let {
                dataMap[it]?.toInt()
            } ?: dataMap.values.sumOf { it.toInt() },
        )
        val percentage = timesGained.toDouble() / divisor
        val dropRate = percentage.coerceAtMost(1.0).formatPercentage()
        return listOf(
            "§7Dropped §e${timesGained.addSeparators()} §7times.",
            "§7Your drop rate: §c$dropRate.",
        )
    }

    final override fun getCoinName(item: TrackedItem): String =
        throw UnsupportedOperationException("Use getCoinName(bucket, item) instead")

    abstract fun getCoinName(bucket: E?, item: TrackedItem): String

    final override fun getCoinDescription(item: TrackedItem): List<String> =
        throw UnsupportedOperationException("Use getCoinDescription(bucket, item) instead")

    abstract fun getCoinDescription(bucket: E?, item: TrackedItem): List<String>

    final override fun addItem(internalName: NeuInternalName, amount: Int, command: Boolean) =
        throw UnsupportedOperationException("Use addItem(bucket, internalName, amount) instead")

    fun addItem(bucket: E, internalName: NeuInternalName, stackSize: Int, command: Boolean) {
        val bucketMap = bucketedItems.getOrPut(bucket) { HashMap() }
        val item = bucketMap.getOrPut(internalName) { TrackedItem() }
        item.processAdd(internalName, stackSize, command) {
            removeItem(bucket, internalName)
        }
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

    open fun E.isBucketSelectable(): Boolean = this in buckets
    abstract fun bucketName(): String

    private val buckets by lazy {
        val enumClass = findGenericSuperclassTypeArgument<BucketedItemTrackerData<*, *>, Enum<E>>(index = 0)
        @Suppress("UNCHECKED_CAST")
        enumClass.enumConstants as? Array<E> ?: error("Enum enum contains not an instance of ${enumClass.name}")
    }
    val selectableBuckets: List<E> = buckets.filter { it.isBucketSelectable() }

    // The null key represents the "all buckets" scroll state when no bucket is selected.
    private val scrollValues: Map<E?, ScrollValue> = buckets.associateWith { ScrollValue() } + (null to ScrollValue())
    val selectedScrollValue: ScrollValue get() = scrollValues[selectedBucket] ?: ScrollValue()

    @Expose
    var selectedBucket: E? = null

    @Expose
    val bucketedItems: MutableMap<E, MutableMap<NeuInternalName, TrackedItem>> = mutableMapOf()

    open fun getBucketedItems(bucket: E) = bucketedItems[bucket] ?: flattenBucketsItems()

    private val E.items get() = bucketedItems[this] ?: mutableMapOf()
    open val selectedBucketItems get() = selectedBucket?.items ?: flattenBucketsItems()

    private fun flattenBucketsItems(): MutableMap<NeuInternalName, TrackedItem> =
        buckets.distinct().fold(mutableMapOf()) { acc, bucket ->
            bucket.items.entries.distinctBy { it.key }
                .forEach { (key, value) ->
                    acc.merge(key, value, ::mergeBuckets)
                }
            acc
        }

    private fun mergeBuckets(existing: TrackedItem, new: TrackedItem): TrackedItem = existing.copy(
        hidden = false,
        totalAmount = existing.totalAmount + new.totalAmount,
        timesGained = existing.timesGained + new.timesGained,
        lastTimeUpdated = maxOf(existing.lastTimeUpdated, new.lastTimeUpdated),
    )
}
