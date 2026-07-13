package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.projectile.FishingHook

@Thread(RENDER)
@PrimaryFunction("onBobberCast")
class FishingBobberCastEvent(val bobber: FishingHook) : SkyHanniEvent()
