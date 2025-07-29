package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase

data class CheckRenderEntityEvent<T : EntityLivingBase>(
    val entity: T,
    val camX: Double,
    val camY: Double,
    val camZ: Double,
) : GenericSkyHanniEvent<T>(entity.javaClass)
