package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.entity.projectile.FishingHook

@PrimaryFunction("onBobberCast")
class FishingBobberCastEvent(val bobber: FishingHook) : SkyHanniEvent()
