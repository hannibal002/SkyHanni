package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.entity.EntityLivingBase

open class SkyHanniRenderEntityEvent<T : EntityLivingBase>(
    val entity: T,
    val renderer: RendererLivingEntity<out T>,
    val x: Double,
    val y: Double,
    val z: Double
) : CancellableSkyHanniEvent() {
    class Pre<T : EntityLivingBase>(
        entity: T,
        renderer: RendererLivingEntity<out T>,
        x: Double,
        y: Double,
        z: Double
    ) : SkyHanniRenderEntityEvent<T>(entity, renderer, x, y, z)

    class Post<T : EntityLivingBase>(
        entity: T,
        renderer: RendererLivingEntity<out T>,
        x: Double,
        y: Double,
        z: Double
    ) : SkyHanniRenderEntityEvent<T>(entity, renderer, x, y, z)

    open class Specials<T : EntityLivingBase>(
        entity: T,
        renderer: RendererLivingEntity<out T>,
        x: Double,
        y: Double,
        z: Double
    ) : SkyHanniRenderEntityEvent<T>(entity, renderer, x, y, z) {
        class Pre<T : EntityLivingBase>(
            entity: T,
            renderer: RendererLivingEntity<out T>,
            x: Double,
            y: Double,
            z: Double
        ) : Specials<T>(entity, renderer, x, y, z)

        class Post<T : EntityLivingBase>(
            entity: T,
            renderer: RendererLivingEntity<out T>,
            x: Double,
            y: Double,
            z: Double
        ) : Specials<T>(entity, renderer, x, y, z)
    }
}
