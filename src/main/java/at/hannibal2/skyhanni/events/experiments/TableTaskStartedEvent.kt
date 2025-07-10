package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.ExperimentationTableApi.ExperimentationTaskType
import at.hannibal2.skyhanni.api.ExperimentationTableApi.ExperimentationTier
import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class TableTaskStartedEvent(
    val type: ExperimentationTaskType,
    val tier: ExperimentationTier,
) : SkyHanniEvent()
