package at.hannibal2.skyhanni.events.entity.abstract

import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import net.minecraft.world.entity.Entity

interface SkyHanniEntityEvent<T : Entity> {
    val entity: T
    val isLocalPlayer get() = entity.isLocalPlayer
}
