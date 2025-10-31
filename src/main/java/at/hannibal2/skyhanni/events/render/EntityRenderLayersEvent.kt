package at.hannibal2.hanni.events.render

import at.hannibal2.hanni.api.event.GenericHanniEvent
import net.minecraft.entity.Entity

open class EntityRenderLayersEvent<T : Entity>(
    val entity: T,
) : GenericHanniEvent<T>(entity.javaClass) {

    class Pre<T : Entity>(
        entity: T,
    ) : EntityRenderLayersEvent<T>(entity)
}
