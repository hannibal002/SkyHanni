package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.tracker.ItemTrackerData.TrackedItem
import com.google.gson.annotations.Expose

abstract class BucketedItemTrackerData<E: Enum<E>> : TrackerData() {

    private val config get() = SkyHanniMod.feature.misc.tracker

    abstract fun resetItems()

    abstract fun getDescription(bucket: E?, timesGained: Long): List<String>

    abstract fun getCoinName(item: TrackedItem): String

    abstract fun getCoinDescription(item: TrackedItem): List<String>

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

    fun getPoppedBuckets(): MutableList<E> = bucketedItems.filter { it.value.isNotEmpty() }.keys.toMutableList()
    fun getItemsProp(): MutableMap<NEUInternalName, TrackedItem> = selectedBucket?.let {
        return bucketedItems[selectedBucket] ?: HashMap()
    } ?: flattenBuckets()

    @Expose
    var selectedBucket: E? = null
    fun selectBucket(type: E?) { selectedBucket = type }

    @Expose
    var bucketedItems: MutableMap<E, MutableMap<NEUInternalName, TrackedItem>> = HashMap()

    private fun flattenBuckets(): MutableMap<NEUInternalName, TrackedItem> {
        val flatMap: MutableMap<NEUInternalName, TrackedItem> = HashMap()
        getPoppedBuckets().forEach { bucket ->
            val entryMap: MutableMap<NEUInternalName, TrackedItem> = bucketedItems[bucket] ?: HashMap()
            entryMap.forEach { (key, value) ->
                flatMap.merge(key, value) { existing, new ->
                    existing.apply {
                        hidden = false
                        totalAmount += new.totalAmount
                        timesGained += new.timesGained
                        lastTimeUpdated = maxOf(lastTimeUpdated, new.lastTimeUpdated)
                    }
                }
            }
        }
        return flatMap
    }
}
