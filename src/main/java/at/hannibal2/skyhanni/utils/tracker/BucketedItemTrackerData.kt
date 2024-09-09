package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData.TrackedItem
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl

abstract class BucketedItemTrackerData<E : Enum<E>> : TrackerData() {

    private val config get() = SkyHanniMod.feature.misc.tracker

    abstract fun resetItems()

    abstract fun getDescription(timesGained: Long): List<String>

    abstract fun getCoinName(bucket: E?, item: TrackedItem): String

    abstract fun getCoinDescription(bucket: E?, item: TrackedItem): List<String>

    open fun getCustomPricePer(internalName: NEUInternalName) = SkyHanniTracker.getPricePer(internalName)

    override fun reset() {
        bucketedItems.clear()
        selectedBucket = null
        resetItems()
    }

    fun addItem(bucket: E, internalName: NEUInternalName, stackSize: Int) {
        val bucketMap = bucketedItems.getOrPut(bucket) { HashMap() }
        val item = bucketMap.getOrPut(internalName) { TrackedItem() }

        item.timesGained++
        item.totalAmount += stackSize
        item.lastTimeUpdated = SimpleTimeMark.now()
    }

    fun removeItem(bucket: E?, internalName: NEUInternalName) {
        bucket?.let {
            bucketedItems[bucket]?.remove(internalName)
        } ?: bucketedItems.forEach {
            it.value.remove(internalName)
        }
    }

    fun toggleItemHide(bucket: E?, internalName: NEUInternalName) {
        bucket?.let {
            bucketedItems[bucket]?.get(internalName)?.let { it.hidden = !it.hidden }
        } ?: bucketedItems.forEach {
            it.value[internalName]?.hidden = !it.value[internalName]?.hidden!!
        }
    }

    private var selectedBucket: E? = null
    private var bucketedItems: MutableMap<E, MutableMap<NEUInternalName, TrackedItem>> = HashMap()

    private fun getBucket(bucket: E): MutableMap<NEUInternalName, TrackedItem> = bucketedItems[bucket]?.toMutableMap() ?: HashMap()
    private fun getPoppedBuckets(): MutableList<E> = (bucketedItems.toMutableMap().filter { it.value.isNotEmpty() }.keys).toMutableList()
    fun getItemsProp(): MutableMap<NEUInternalName, TrackedItem> = getSelectedBucket()?.let { getBucket(it) } ?: flattenBuckets()
    fun getSelectedBucket() = selectedBucket
    private fun selectBucket(type: E?) {
        selectedBucket = type;
    }

    fun selectNextSequentialBucket() {
        @Suppress("UNCHECKED_CAST")
        val enumValues: Array<E> = selectedBucket?.javaClass?.enumConstants
            ?: (this.javaClass.genericSuperclass as? ParameterizedTypeImpl)?.actualTypeArguments?.firstOrNull()?.let { type ->
                (type as? Class<E>)?.enumConstants
            } ?: throw IllegalStateException("Unable to retrieve enum constants for E")

        // If selectedBucket is null, start with the first enum[0]
        if (selectedBucket == null) {
            selectBucket(enumValues.first()) // Start with the first enum value
            return
        }

        // Move to the next ordinal, or wrap to null if at the last value
        val currentOrdinal = selectedBucket!!.ordinal
        val nextOrdinal = currentOrdinal + 1

        if (nextOrdinal >= enumValues.size) selectBucket(null) // Wrap to null if at the last value
        else selectBucket(enumValues[nextOrdinal]) // Move to the next enum value
    }


    private fun flattenBuckets(): MutableMap<NEUInternalName, TrackedItem> {
        val flatMap: MutableMap<NEUInternalName, TrackedItem> = HashMap()
        getPoppedBuckets().distinct().forEach { bucket ->
            getBucket(bucket).filter { !it.value.hidden }.entries.distinctBy { it.key }.forEach { (key, value) ->
                flatMap.merge(key, value) { existing, new ->
                    existing.copy(
                        hidden = false,
                        totalAmount = existing.totalAmount + new.totalAmount,
                        timesGained = existing.timesGained + new.timesGained,
                        lastTimeUpdated = maxOf(existing.lastTimeUpdated, new.lastTimeUpdated),
                    )
                }
            }
        }
        return flatMap.toMutableMap()
    }
}
