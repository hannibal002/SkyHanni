package at.hannibal2.skyhanni.events.diana

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.event.diana.BurrowType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onBurrowDetect")
class BurrowDetectEvent(val burrowLocation: Vec3, val type: BurrowType) : SkyHanniEvent()
