package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTaskType
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTier

class TableTaskStartedEvent(
    val type: ExperimentationTaskType,
    val tier: ExperimentationTier,
) : SkyHanniEvent()
