package at.hannibal2.skyhanni.events.yearofthepig

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.event.yearofthepig.PigFeaturesApi
import at.hannibal2.skyhanni.utils.EntityUtils

class ShinyOrbUsedEvent(
    private val shinyOrbEntityId: Int
) : SkyHanniEvent() {
    val shinyOrbEntity get() = EntityUtils.getEntityByID(shinyOrbEntityId)
    constructor (dataSet: PigFeaturesApi.ShinyOrbDataSet) : this(dataSet.shinyOrbEntityId ?: -1)
}
