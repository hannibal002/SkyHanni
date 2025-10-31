package at.hannibal2.hanni.events.fishing

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.entity.projectile.EntityFishHook

class FishingBobberInLiquidEvent(val bobber: EntityFishHook, val onWater: Boolean) : HanniEvent()
