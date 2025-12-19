package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.entity.projectile.FishingBobberEntity

class FishingBobberInLiquidEvent(val bobber: FishingBobberEntity, val onWater: Boolean) : SkyHanniEvent()
