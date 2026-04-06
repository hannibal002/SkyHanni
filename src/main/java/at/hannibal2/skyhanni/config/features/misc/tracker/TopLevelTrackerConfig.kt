package at.hannibal2.skyhanni.config.features.misc.tracker

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerSettings
import at.hannibal2.skyhanni.config.features.misc.tracker.individual.PerTrackerConfig

/**
 * Marker interface for tracker feature configs that live at the top level of a feature's config block.
 *
 * Holds the [TrackerSettings] subclass the [perTrackerConfig] wraps, ensuring the tracker implementation
 * and its config agree on what settings are available.
 *
 * Example:
 * ```
 * class GardenBpsTrackerConfig : TopLevelTrackerConfig {
 *     override val perTrackerConfig: TimedPerTrackerConfig<GardenTrackerSettings> = TimedPerTrackerConfig()
 *     ...
 * }
 * ```
 */
@Suppress("StorageNeedsExpose")
interface TopLevelTrackerConfig {
    var enabled: Boolean
    val perTrackerConfig: PerTrackerConfig<TrackerSettings>
    val position: Position
}
