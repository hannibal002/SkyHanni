package at.hannibal2.skyhanni.events

import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class SpawnParticleEvent(val particleId: Int, val callerClass: String, val x: Double, val y: Double, val z: Double) :
    LorenzEvent()