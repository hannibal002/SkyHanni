package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.GenericSkyHanniEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

@PrimaryFunction("onEntityNameTagRender")
class EntityNameTagRenderEvent<T : Entity>(
    override val entity: T,
    var chatComponent: Component,
) : GenericSkyHanniEvent<T>(entity.javaClass), SkyHanniEntityEvent<T>
