package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import com.google.gson.annotations.Expose
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl

abstract class BucketedItemTrackerData<E : Enum<E>> : ItemTrackerData() {

    @Deprecated(
        "Use getDescription(bucket, timesGained) instead",
        ReplaceWith("getDescription(bucket, timesGained)")
    )
    override fun getDescription(timesGained: Long): List<String> =
        throw UnsupportedOperationException("Use getDescription(bucket, timesGained) instead")

    abstract fun getDescription(bucket: E?, timesGained: Long): List<String>

    @Deprecated(
        "Use getCoinName(bucket, item) instead",
        ReplaceWith("getCoinName(bucket, item)")
    )
    override fun getCoinName(item: TrackedItem): String =
        throw UnsupportedOperationException("Use getCoinName(bucket, item) instead")

    abstract fun getCoinName(bucket: E?, item: TrackedItem): String

    @Deprecated(
        "Use getCoinDescription(bucket, item) instead",
        ReplaceWith("getCoinDescription(bucket, item)")
    )
    override fun getCoinDescription(item: TrackedItem): List<String> =
        throw UnsupportedOperationException("Use getCoinDescription(bucket, item) instead")

    abstract fun getCoinDescription(bucket: E?, item: TrackedItem): List<String>

    @Deprecated(
        "Use addItem(bucket, internalName, amount) instead",
        ReplaceWith("addItem(bucket, internalName, amount)")
    )
    override fun addItem(internalName: NeuInternalName, amount: Int, command: Boolean) =
        throw UnsupportedOperationException("Use addItem(bucket, internalName, amount) instead")

    fun addItem(bucket: E, internalName: NeuInternalName, stackSize: Int, command: Boolean) {
        val bucketMap = bucketedItems.getOrPut(bucket) { HashMap() }
        val item = bucketMap.getOrPut(internalName) { TrackedItem() }
        item.processAdd(internalName, stackSize, command) {
            removeItem(bucket, internalName)
        }
    }

    override fun reset() {
        bucketedItems.clear()
        selectedBucket = null
        resetItems()
    }

    @Deprecated(
        "Use removeItem(bucket, internalName) instead",
        ReplaceWith("removeItem(bucket, internalName)")
    )
    override fun removeItem(internalName: NeuInternalName) =
        throw UnsupportedOperationException("Use removeItem(bucket, internalName) instead")

    fun removeItem(bucket: E?, internalName: NeuInternalName) {
        bucket?.let {
            bucketedItems[bucket]?.remove(internalName)
        } ?: bucketedItems.forEach {
            it.value.remove(internalName)
        }
    }

    @Deprecated(
        "Use toggleItemHide(bucket, internalName, currentlyHidden) instead",
        ReplaceWith("toggleItemHide(bucket, internalName, currentlyHidden)")
    )
    override fun toggleItemHide(internalName: NeuInternalName, currentlyHidden: Boolean) =
        throw UnsupportedOperationException("Use toggleItemHide(bucket, internalName, currentlyHidden) instead")

    fun toggleItemHide(bucket: E?, internalName: NeuInternalName, currentlyHidden: Boolean) {
        bucket?.let {
            bucketedItems[bucket]?.get(internalName)?.hidden = !currentlyHidden
        } ?: bucketedItems.forEach { (_, items) ->
            items[internalName]?.hidden = !currentlyHidden
        }
    }

    abstract fun E.isBucketSelectable(): Boolean

    private val E.items get() = bucketedItems[this] ?: mutableMapOf()
    private val scrollValues: Map<E?, ScrollValue> by lazy {
        buckets.associateWith { ScrollValue() } + (null to ScrollValue())
    }
    private val buckets: Array<E> by lazy {
        @Suppress("UNCHECKED_CAST")
        selectedBucket?.javaClass?.enumConstants
            ?: (this.javaClass.genericSuperclass as? ParameterizedTypeImpl)?.actualTypeArguments?.firstOrNull()?.let { type ->
                (type as? Class<E>)?.enumConstants
            } ?: throwBucketInitError()
    }

    val selectableBuckets get() = buckets.filter { it.isBucketSelectable() }
    val selectedScrollValue: ScrollValue get() = scrollValues[selectedBucket] ?: throwBucketInitError()
    val selectedBucketItems get() = selectedBucket?.items ?: flattenBucketsItems()

    @Expose
    val bucketedItems: MutableMap<E, MutableMap<NeuInternalName, TrackedItem>> = mutableMapOf()

    @Expose
    var selectedBucket: E? = null

    private fun throwBucketInitError(): Nothing = ErrorManager.skyHanniError(
        "Unable to retrieve enum constants for E in BucketedItemTrackerData",
        "selectedBucket" to selectedBucket,
        "dataClass" to this.javaClass.superclass.name,
    )

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
