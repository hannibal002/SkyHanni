package at.hannibal2.skyhanni.utils.tracker

import at.hannibal2.skyhanni.config.features.misc.tracker.ItemTrackerGenericConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.GenericIndividualTrackerConfig
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.utils.renderables.Searchable

open class
SkyHanniItemTracker<Data : ItemTrackerData<*>>(
    name: String,
    createNewSession: () -> Data,
    getStorage: (ProfileSpecificStorage) -> Data,
    extraDisplayModes: Map<DisplayMode, (ProfileSpecificStorage) -> Data> = emptyMap(),
    trackerConfig: () -> GenericIndividualTrackerConfig<ItemTrackerGenericConfig>,
    customUptimeControl: Boolean = false,
    drawDisplay: (Data) -> List<Searchable>,
) : SkyHanniTracker<Data, GenericIndividualTrackerConfig<ItemTrackerGenericConfig>>(
    name,
    createNewSession,
    getStorage,
    extraDisplayModes,
    customUptimeControl = customUptimeControl,
    drawDisplay = drawDisplay,
    trackerConfig = { trackerConfig() }
),
    ItemTracking<Data, GenericIndividualTrackerConfig<ItemTrackerGenericConfig>> {
    override val tracker get() = this
}
