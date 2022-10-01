package at.hannibal2.skyhanni.events

import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
class SpawnParticleEvent(val id: Int, val x: Double, val y: Double, val z: Double) : LorenzEvent()