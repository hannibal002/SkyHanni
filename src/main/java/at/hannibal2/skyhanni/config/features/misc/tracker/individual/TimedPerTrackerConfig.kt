package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TimedTrackerConfig
import at.hannibal2.skyhanni.config.features.misc.tracker.generic.TrackerSettings
import com.google.gson.annotations.Expose
import io.github.notenoughupdates.moulconfig.annotations.Accordion
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption

/**
 * Extends [PerTrackerConfig] with timed-tracker retention settings.
 *
 * Use this as the type in any tracker that extends [at.hannibal2.skyhanni.utils.tracker.SkyhanniTimedTracker].
 * The [timedTracker] accordion is synced before the base settings so that session-reset
 * behavior is applied consistently during bulk sync.
 *
 * @param Settings the [TrackerSettings] subclass whose options appear in the accordion.
 */
open class TimedPerTrackerConfig<out Settings : TrackerSettings> protected constructor(
    settingsClass: Class<@UnsafeVariance Settings>? = null,
) : PerTrackerConfig<Settings>(settingsClass) {

    // Subclasses that don't pass a class explicitly fall back to reflection.
    // This works for proper subclasses because the type argument is baked into
    // the subclass bytecode and genericSuperclass can read it.
    protected constructor() : this(null)

    @Expose
    @ConfigOption(name = "Timed Tracker", desc = "Timed Tracker Settings")
    @Accordion
    val timedTracker: TimedTrackerConfig = TimedTrackerConfig()

    override fun syncSettings() {
        // Sync timed settings first so the session-reset flag is in its final state
        // before the base sync runs and potentially triggers a new session.
        timedTracker.syncSettings()
        super.syncSettings()
    }

    companion object {
        /** Creates a [TimedPerTrackerConfig] using the reified [S] directly, bypassing reflection. */
        inline operator fun <reified S : TrackerSettings> invoke(): TimedPerTrackerConfig<S> =
            object : TimedPerTrackerConfig<S>(S::class.java) {}
    }
}
