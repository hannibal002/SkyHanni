package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose

abstract class BucketedItemTrackerData<E: Enum<E>> : TrackerData() {

    private val config get() = SkyHanniMod.feature.misc.tracker

    abstract fun resetItems()

    abstract fun getDescription(timesGained: Long): List<String>

    abstract fun getCoinName(item: ItemTrackerData.TrackedItem): String

    abstract fun getCoinDescription(item: ItemTrackerData.TrackedItem): List<String>

    open fun getCustomPricePer(internalName: NEUInternalName) = SkyHanniTracker.getPricePer(internalName)

    override fun reset() {
        bucketedItems.clear()
        resetItems()
    }

    fun addItem(bucket: E, internalName: NEUInternalName, stackSize: Int) {
        val bucketMap = bucketedItems.getOrPut(bucket) { HashMap() }
        val item = bucketMap.getOrPut(internalName) { ItemTrackerData.TrackedItem() }

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

    fun getItems(bucket: E? = null): MutableMap<NEUInternalName, ItemTrackerData.TrackedItem> = bucket?.let { return bucketedItems[bucket] ?: HashMap() } ?: items
    fun getPoppedBuckets(): MutableList<E> = bucketedItems.filter { it.value.isNotEmpty() }.keys.toMutableList()

    @Expose
    var selectedBucket: E? = null

    @Expose
    var bucketedItems: MutableMap<E, MutableMap<NEUInternalName, ItemTrackerData.TrackedItem>> = HashMap()

    @Expose
    var items: MutableMap<NEUInternalName, ItemTrackerData.TrackedItem> =
        bucketedItems.values.flatMap { it.entries }.groupBy({ it.key }, { it.value })
        .mapValues { (_, valueList) ->
            ItemTrackerData.TrackedItem().apply {
                timesGained = valueList.filterNot { it.hidden }.sumOf { it.timesGained }
                totalAmount = valueList.filterNot { it.hidden }.sumOf { it.totalAmount }
                hidden = false
                lastTimeUpdated = valueList.filterNot { it.hidden }.maxByOrNull { it.lastTimeUpdated } ?.lastTimeUpdated
                    ?: SimpleTimeMark.farPast()
            }
        }.filter { it.value.timesGained > 0 && it.value.totalAmount > 0 } .toMutableMap()
}
