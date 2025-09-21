package at.hannibal2.skyhanni.config.features.misc.tracker.individual

import at.hannibal2.skyhanni.config.features.misc.tracker.TrackerGenericConfig

class IndividualTrackerConfig : GenericIndividualTrackerConfig<TrackerGenericConfig>(
    { TrackerGenericConfig() }
) {
    init {
        configSet.add(this)
    }

}
