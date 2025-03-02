package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentTaskType
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentTier

class TableTaskStartedEvent(
    val type: ExperimentTaskType,
    val tier: ExperimentTier,
) : SkyHanniEvent()
