package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.world.entity.projectile.FishingHook

@Thread(RENDER)
@PrimaryFunction("onBobberCast")
class FishingBobberCastEvent(val bobber: FishingHook) : SkyHanniEvent()
