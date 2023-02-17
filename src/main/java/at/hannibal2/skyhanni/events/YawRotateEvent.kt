package at.hannibal2.skyhanni.events

import net.minecraftforge.fml.common.eventhandler.Cancelable

@Cancelable
data class YawRotateEvent(val yawDelta: Float, val oldYaw: Float) : LorenzEvent()