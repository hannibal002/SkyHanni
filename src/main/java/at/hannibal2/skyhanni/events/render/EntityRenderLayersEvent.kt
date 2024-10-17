package at.hannibal2.skyhanni.events.render

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.entity.Entity
import net.minecraftforge.fml.common.eventhandler.Cancelable

open class EntityRenderLayersEvent<T : Entity>(
    val entity: T,
) : SkyHanniEvent() {

    @Cancelable
    class Pre<T : Entity>(
        entity: T,
    ) : EntityRenderLayersEvent<T>(entity)
}
