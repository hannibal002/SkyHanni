package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.config.core.config.Position

@Suppress("StorageNeedsExpose")
interface TopLevelTrackerConfig {
    var enabled: Boolean
    val perTrackerConfig: GenericIndividualTrackerConfig<*>
    val position: Position
}
