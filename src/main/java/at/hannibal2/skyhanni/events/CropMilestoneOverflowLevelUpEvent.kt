package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.garden.CropType

class CropMilestoneOverflowLevelUpEvent(val cropType: CropType, val oldLevel: Int, val newLevel: Int) : LorenzEvent()
