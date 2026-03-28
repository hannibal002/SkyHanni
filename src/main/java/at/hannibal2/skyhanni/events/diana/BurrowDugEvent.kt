package at.hannibal2.skyhanni.events.diana

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onBurrowDug")
class BurrowDugEvent(val burrowLocation: Vec3, val current: Int, val max: Int) : SkyHanniEvent()
