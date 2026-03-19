package at.hannibal2.skyhanni.config.features.misc.tracker.timed

import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig

class TimedIndividualTrackerConfig : TimedGenericIndividualConfig<TrackerGenericConfig>(
    { TrackerGenericConfig() }
) {
    init {
        configSet.add(this)
    }
}
