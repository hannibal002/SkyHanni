package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import net.minecraft.world.entity.LivingEntity

// TODO replace all "cancel only" usages of this event. the only remaining stuff should be EntityOpacityManager
/**
 * This event gets called multiple times per frame per entity.
 * This is super inefficient, only use it if absolutely necessary, and then also only with heavy caching added.
 * For normal cases of "hide this entity" rather use [CheckRenderEntityEvent].
 */
@Deprecated("use CheckRenderEntityEvent instead")
open class SkyHanniRenderEntityEvent<T : LivingEntity>(
    val entity: T,
    val x: Double,
    val y: Double,
    val z: Double
) : GenericSkyHanniEvent<T>(entity.javaClass) {
    class Pre<T : LivingEntity>(
        entity: T,
        x: Double,
        y: Double,
        z: Double
    ) : SkyHanniRenderEntityEvent<T>(entity, x, y, z)

    class Post<T : LivingEntity>(
        entity: T,
        x: Double,
        y: Double,
        z: Double
    ) : SkyHanniRenderEntityEvent<T>(entity, x, y, z)

    open class Specials<T : LivingEntity>(
        entity: T,
        x: Double,
        y: Double,
        z: Double
    ) : SkyHanniRenderEntityEvent<T>(entity, x, y, z) {
        class Pre<T : LivingEntity>(
            entity: T,
            x: Double,
            y: Double,
            z: Double
        ) : Specials<T>(entity, x, y, z)

        class Post<T : LivingEntity>(
            entity: T,
            x: Double,
            y: Double,
            z: Double
        ) : Specials<T>(entity, x, y, z)
    }
}
