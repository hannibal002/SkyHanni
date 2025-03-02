package at.hannibal2.skyhanni.events.experiments

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentTaskType
import at.hannibal2.skyhanni.features.inventory.experimentationtable.ExperimentTier
import at.hannibal2.skyhanni.utils.NeuInternalName

class TableTaskCompletedEvent(
    val type: ExperimentTaskType,
    val tier: ExperimentTier,
    val enchantingXpGained: Long? = null,
    val loot: Map<NeuInternalName, Int> = mapOf()
) : SkyHanniEvent()
