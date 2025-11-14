package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.entity.Entity

/**
 * THis event is already cached and only fires 5 times per second per entity.
 * This means the changed visibility state can take up to 200ms until it updates.
 * This is a cached version similarly to [SkyHanniRenderEntityEvent].
 * Do not use this event when you want to do further render calls!
 * Internally we directly mixin to shouldRender.
 */
data class CheckRenderEntityEvent<T : Entity>(
    val entity: T,
    val camX: Double,
    val camY: Double,
    val camZ: Double,
) : GenericSkyHanniEvent<T>(entity.javaClass)
