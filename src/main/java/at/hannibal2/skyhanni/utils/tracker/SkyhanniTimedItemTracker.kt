package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.features.misc.tracker.ItemTrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.timed.TimedGenericIndividualConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.utils.renderables.Searchable

open class SkyHanniTimedItemTracker<Data : ItemTrackerData<*>>(
    name: String,
    createNewSession: () -> Data,
    getStorage: (ProfileSpecificStorage) -> TimedTrackerData<Data, *>,
    extraDisplayModes: Set<DisplayMode> = emptySet(),
    trackerConfig: () -> TimedGenericIndividualConfig<ItemTrackerGenericConfig>,
    customUptimeControl: Boolean = false,
    drawDisplay: (Data) -> List<Searchable>,
) : SkyhanniTimedTracker<Data, TimedGenericIndividualConfig<ItemTrackerGenericConfig>>(
    name,
    createNewSession,
    getStorage,
    extraDisplayModes = extraDisplayModes,
    customUptimeControl = customUptimeControl,
    drawDisplay = drawDisplay,
    trackerConfig = { trackerConfig() }
),
    ItemTracking<Data, TimedGenericIndividualConfig<ItemTrackerGenericConfig>> {
    override val tracker: SkyHanniTracker<Data, TimedGenericIndividualConfig<ItemTrackerGenericConfig>>
        get() = this
}
