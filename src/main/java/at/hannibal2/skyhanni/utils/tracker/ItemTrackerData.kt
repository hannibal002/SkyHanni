package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose

abstract class ItemTrackerData : TrackerData() {

    private val config get() = SkyHanniMod.feature.misc.tracker

    abstract fun resetItems()

    abstract fun getDescription(timesGained: Long): List<String>

    abstract fun getCoinName(item: TrackedItem): String

    abstract fun getCoinDescription(item: TrackedItem): List<String>

    open fun getCustomPricePer(internalName: NEUInternalName) = SkyHanniTracker.getPricePer(internalName)

    override fun reset() {
        items.clear()
        resetItems()
    }

    fun additem(internalName: NEUInternalName, stackSize: Int) {
        val item = items.getOrPut(internalName) { TrackedItem() }

        item.timesGained++
        item.totalAmount += stackSize
        item.lastTimeUpdated = SimpleTimeMark.now()
    }

    @Expose
    var items: MutableMap<NEUInternalName, TrackedItem> = HashMap()

    class TrackedItem {
        @Expose
        var internalName: NEUInternalName? = null

        @Expose
        var timesGained: Long = 0

        @Expose
        var totalAmount: Long = 0

        @Expose
        var hidden = false

        var lastTimeUpdated = SimpleTimeMark.farPast()
    }
}
