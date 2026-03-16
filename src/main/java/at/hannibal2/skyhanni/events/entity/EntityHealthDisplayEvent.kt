package at.hannibal2.skyhanni.events.entity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.entity.abstract.SkyHanniEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity

@PrimaryFunction("onEntityHealthDisplay")
class EntityHealthDisplayEvent(
    override val entity: Entity,
    var text: Component,
) : SkyHanniEvent(), SkyHanniEntityEvent<Entity>
