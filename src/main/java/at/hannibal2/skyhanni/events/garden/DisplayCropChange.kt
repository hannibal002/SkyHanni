package at.hannibal2.skyhanni.events.garden

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.garden.CropType

class DisplayCropChange(val crop: CropType?) : SkyHanniEvent()
