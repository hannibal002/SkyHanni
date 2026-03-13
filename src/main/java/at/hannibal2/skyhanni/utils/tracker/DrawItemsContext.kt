package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.tracker.data.ItemTrackerData

/**
 * Overridable accessor and action callbacks for [SkyHanniItemTracker.drawItems].
 *
 * The defaults suit any plain item tracker. [SkyHanniBucketedItemTracker] supplies its own
 * instance to redirect reads and mutations through the currently selected bucket without
 * duplicating the draw loop.
 *
 * Construct via [default] for the standard case, or supply all parameters explicitly for
 * bucketed or otherwise non-standard trackers.
 */
class DrawItemsContext(
    val itemsAccessor: () -> Map<NeuInternalName, ItemTrackerData.TrackedItem>,
    val getCoinName: (ItemTrackerData.TrackedItem) -> String,
    val itemRemover: (NeuInternalName, String) -> Unit,
    val itemHider: (NeuInternalName, Boolean) -> Unit,
    val getLoreList: (NeuInternalName, ItemTrackerData.TrackedItem) -> List<String>,
) {
    companion object {
        fun <Data : ItemTrackerData<*>> default(
            data: Data,
            tracker: SkyHanniItemTracker<Data>,
        ) = DrawItemsContext(
            itemsAccessor = { data.items },
            getCoinName = { item -> data.getCoinName(item) },
            itemRemover = { internalName, cleanName ->
                tracker.modify { it.items.remove(internalName) }
                ChatUtils.chat("Removed $cleanName §efrom ${tracker.name}.")
            },
            itemHider = { internalName, currentlyHidden ->
                tracker.modify { it.toggleItemHide(internalName, currentlyHidden) }
            },
            getLoreList = { internalName, item ->
                if (internalName == SKYBLOCK_COIN) data.getCoinDescription(item)
                else data.getDescription(item)
            },
        )
    }
}
