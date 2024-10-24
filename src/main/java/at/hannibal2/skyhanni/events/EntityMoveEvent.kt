package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity

class EntityMoveEvent<T : Entity>(
    val entity: T,
    val oldLocation: LorenzVec,
    val newLocation: LorenzVec,
    val distance: Double,
) : GenericSkyHanniEvent<T>(entity.javaClass) {
    val isLocalPlayer by lazy { entity == Minecraft.getMinecraft().thePlayer }
}
