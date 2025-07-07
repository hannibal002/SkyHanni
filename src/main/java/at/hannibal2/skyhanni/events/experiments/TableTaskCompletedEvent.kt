package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.ExperimentationTaskType
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentationTableApi.ExperimentationTier
import at.hannibal2.skyhanni.utils.NeuInternalName

class TableTaskCompletedEvent(
    val type: ExperimentationTaskType,
    val tier: ExperimentationTier,
    val enchantingXpGained: Long? = null,
    val loot: Map<NeuInternalName, Int> = mapOf(),
) : SkyHanniEvent()
