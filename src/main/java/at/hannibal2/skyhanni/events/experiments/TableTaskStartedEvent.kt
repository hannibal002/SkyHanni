package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.ExperimentationTaskType
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.ExperimentationTier

class TableTaskStartedEvent(
    val type: ExperimentationTaskType,
    val tier: ExperimentationTier,
) : SkyHanniEvent()
