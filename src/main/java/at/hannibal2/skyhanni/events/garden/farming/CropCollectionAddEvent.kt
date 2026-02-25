package at.hannibal2.skyhanni.events.garden.farming

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.CropCollectionType
import at.hannibal2.skyhanni.features.garden.CropType

class CropCollectionAddEvent(val crop: CropType, val cropCollectionType: CropCollectionType, val amount: Long) : SkyHanniEvent()

