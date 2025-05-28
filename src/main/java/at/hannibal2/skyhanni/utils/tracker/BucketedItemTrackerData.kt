package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import com.google.gson.annotations.Expose
import kotlin.reflect.KClass

abstract class BucketedItemTrackerData<E : Enum<E>>(clazz: KClass<E>) : ItemTrackerData() {

    @Deprecated("Use getBucketItems(bucket) instead", ReplaceWith("getBucketItems(bucket)"))
    override fun getDescription(timesGained: Long): List<String> =
        throw UnsupportedOperationException("Use getDescription(bucket, timesGained) instead")

    abstract fun getDescription(bucket: E?, timesGained: Long): List<String>

    @Deprecated("Use getBucketItems(bucket) instead", ReplaceWith("getBucketItems(bucket)"))
    override fun getCoinName(item: TrackedItem): String =
        throw UnsupportedOperationException("Use getCoinName(bucket, item) instead")

    abstract fun getCoinName(bucket: E?, item: TrackedItem): String

    @Deprecated("Use getBucketItems(bucket) instead", ReplaceWith("getBucketItems(bucket)"))
    override fun getCoinDescription(item: TrackedItem): List<String> =
        throw UnsupportedOperationException("Use getCoinDescription(bucket, item) instead")

    abstract fun getCoinDescription(bucket: E?, item: TrackedItem): List<String>

    abstract fun E.isBucketSelectable(): Boolean

    override fun reset() {
        bucketedItems.clear()
        selectedBucket = null
        resetItems()
    }

    fun addItem(bucket: E, internalName: NeuInternalName, stackSize: Int) {
        val bucketMap = bucketedItems.getOrPut(bucket) { HashMap() }
        val item = bucketMap.getOrPut(internalName) { TrackedItem() }

        item.timesGained++
        item.totalAmount += stackSize
        item.lastTimeUpdated = SimpleTimeMark.now()
    }

    fun removeItem(bucket: E?, internalName: NeuInternalName) {
        bucket?.let {
            bucketedItems[bucket]?.remove(internalName)
        } ?: bucketedItems.forEach {
            it.value.remove(internalName)
        }
    }

    fun toggleItemHide(bucket: E?, internalName: NeuInternalName) {
        bucket?.let {
            bucketedItems[bucket]?.get(internalName)?.let { it.hidden = !it.hidden }
        } ?: bucketedItems.forEach {
            it.value[internalName]?.hidden = !it.value[internalName]?.hidden!!
        }
    }

    private val buckets: Array<E> = clazz.java.enumConstants
    val selectableBuckets: List<E> = buckets.filter { it.isBucketSelectable() }

    private val scrollValues: Map<E?, ScrollValue> = buckets.associateWith { ScrollValue() } + (null to ScrollValue())
    val selectedScrollValue: ScrollValue get() = scrollValues[selectedBucket] ?: ScrollValue()

    @Expose
    var selectedBucket: E? = null

    @Expose
    val bucketedItems: MutableMap<E, MutableMap<NeuInternalName, TrackedItem>> = mutableMapOf()

    private val E.items get() = bucketedItems[this] ?: mutableMapOf()
    val selectedBucketItems get() = selectedBucket?.items ?: flattenBucketsItems()

    private fun flattenBucketsItems(): MutableMap<NeuInternalName, TrackedItem> =
        buckets.distinct().fold(mutableMapOf()) { acc, bucket ->
            bucket.items.filter { (_, item) -> !item.hidden }
                .entries.distinctBy { it.key }
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
