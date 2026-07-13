package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import net.minecraft.world.entity.projectile.FishingHook

@Thread(RENDER)
class FishingBobberInLiquidEvent(val bobber: FishingHook, val onWater: Boolean) : SkyHanniEvent()
