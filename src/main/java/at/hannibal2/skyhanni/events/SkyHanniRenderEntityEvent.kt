package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.EntityLivingBase

// TODO replace all "cancel only" usages of this event. the only remaining stuff should be EntityOpacityManager
/**
 * This event gets called multiple times per frame per entity.
 * This is super inefficient, only use it if absolutely necessary, and then also only with heavy caching added.
 * For normal cases of "hide this entity" rather use [CheckRenderEntityEvent].
 */
@Deprecated("use CheckRenderEntityEvent instead")
open class HanniRenderEntityEvent<T : EntityLivingBase>(
    val entity: T,
    val x: Double,
    val y: Double,
    val z: Double
) : GenericHanniEvent<T>(entity.javaClass) {
    class Pre<T : EntityLivingBase>(
        entity: T,
        x: Double,
        y: Double,
        z: Double
    ) : HanniRenderEntityEvent<T>(entity, x, y, z)

    class Post<T : EntityLivingBase>(
        entity: T,
        x: Double,
        y: Double,
        z: Double
    ) : HanniRenderEntityEvent<T>(entity, x, y, z)

    open class Specials<T : EntityLivingBase>(
        entity: T,
        x: Double,
        y: Double,
        z: Double
    ) : HanniRenderEntityEvent<T>(entity, x, y, z) {
        class Pre<T : EntityLivingBase>(
            entity: T,
            x: Double,
            y: Double,
            z: Double
        ) : Specials<T>(entity, x, y, z)

        class Post<T : EntityLivingBase>(
            entity: T,
            x: Double,
            y: Double,
            z: Double
        ) : Specials<T>(entity, x, y, z)
    }
}
