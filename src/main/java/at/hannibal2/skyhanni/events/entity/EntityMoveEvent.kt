package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

@PrimaryFunction("onEntityMove")
class EntityMoveEvent<T : LivingEntity>(
    val entity: T,
    val oldLocation: Vec3,
    val newLocation: Vec3,
    val distance: Double,
) : GenericSkyHanniEvent<T>(entity.javaClass) {
    val isLocalPlayer get() = entity.isLocalPlayer
}
