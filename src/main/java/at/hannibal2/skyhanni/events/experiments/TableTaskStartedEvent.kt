package at.hannibal2.hanni.events.experiments

import at.hannibal2.hanni.api.ExperimentationTableApi.ExperimentationTaskType
import at.hannibal2.hanni.api.ExperimentationTableApi.ExperimentationTier
import at.hannibal2.hanni.api.event.HanniEvent

class TableTaskStartedEvent(
    val type: ExperimentationTaskType,
    val tier: ExperimentationTier,
) : HanniEvent()
