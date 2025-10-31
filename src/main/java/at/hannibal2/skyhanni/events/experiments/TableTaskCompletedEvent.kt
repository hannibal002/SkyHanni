package at.hannibal2.hanni.events.experiments

import at.hannibal2.hanni.api.ExperimentationTableApi.ExperimentationTaskType
import at.hannibal2.hanni.api.ExperimentationTableApi.ExperimentationTier
import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.NeuInternalName

class TableTaskCompletedEvent(
    val type: ExperimentationTaskType,
    val tier: ExperimentationTier,
    val enchantingXpGained: Long? = null,
    val loot: Map<NeuInternalName, Int> = mapOf(),
) : HanniEvent()
