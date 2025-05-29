package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import com.google.gson.annotations.Expose

abstract class ItemTrackerData : TrackerData() {

    abstract fun resetItems()

    abstract fun getDescription(timesGained: Long): List<String>

    abstract fun getCoinName(item: TrackedItem): String

    // TODO add amount in the string
    abstract fun getCoinDescription(item: TrackedItem): List<String>

    open fun getCustomPricePer(internalName: NeuInternalName) = SkyHanniTracker.getPricePer(internalName)

    override fun reset() {
        items.clear()
        resetItems()
    }

    open fun addItem(internalName: NeuInternalName, amount: Int, command: Boolean) {
        val item = items.getOrPut(internalName) { TrackedItem() }
        item.processAdd(internalName, amount, command)
    }

    open fun removeItem(internalName: NeuInternalName) {
        items.remove(internalName)
    }

    open fun toggleItemHide(internalName: NeuInternalName, currentlyHidden: Boolean) {
        val item = items.getOrPut(internalName) { TrackedItem() }
        item.hidden = !currentlyHidden
    }

    fun TrackedItem.processAdd(
        internalName: NeuInternalName,
        amount: Int,
        command: Boolean,
        removalRunner: (NeuInternalName) -> Unit? = { removeItem(internalName) },
    ) = apply {
        if (!command) { timesGained++ }
        totalAmount += amount
        lastTimeUpdated = SimpleTimeMark.now()
        if (command && totalAmount <= 0) { removalRunner(internalName) }
    }

    @Expose
    var items: MutableMap<NeuInternalName, TrackedItem> = HashMap()

    data class TrackedItem(
        @Expose var timesGained: Long = 0,
        @Expose var totalAmount: Long = 0,
        @Expose var hidden: Boolean = false,
        var lastTimeUpdated: SimpleTimeMark = SimpleTimeMark.farPast()
    )
}
