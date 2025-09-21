package at.hannibal2.skyhanni.config.features.misc.tracker.timed

import at.hannibal2.skyhanni.config.features.misc.tracker.ItemTrackerGenericConfig

class TimedIndividualItemTrackerConfig : TimedGenericIndividualConfig<ItemTrackerGenericConfig>(
    { ItemTrackerGenericConfig() }
) {
    init {
        configSet.add(this)
    }
}
