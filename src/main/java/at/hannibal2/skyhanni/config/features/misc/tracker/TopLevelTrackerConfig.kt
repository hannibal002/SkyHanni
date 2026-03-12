package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.GenericIndividualTrackerConfig

@Suppress("StorageNeedsExpose")
interface TopLevelTrackerConfig<T : TrackerGenericConfig> {
    var enabled: Boolean
    val perTrackerConfig: GenericIndividualTrackerConfig<T>
    val position: Position
}
